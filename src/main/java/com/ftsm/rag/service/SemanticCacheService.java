package com.ftsm.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ftsm.rag.config.AppConfig;
import com.ftsm.rag.model.CacheData;
import com.ftsm.rag.model.CacheEntry;
import dev.langchain4j.data.embedding.Embedding;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SemanticCacheService {

    private final AppConfig appConfig;
    private final ModelFactory modelFactory;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final Path cacheFile;
    private final ReentrantLock lock = new ReentrantLock();

    private final List<CacheEntry> entries = new CopyOnWriteArrayList<>();
    private int hitCount = 0;
    private int missCount = 0;

    public SemanticCacheService(AppConfig appConfig, ModelFactory modelFactory) {
        this(appConfig, modelFactory, null);
    }

    @Autowired
    public SemanticCacheService(AppConfig appConfig, ModelFactory modelFactory, Environment environment) {
        this.appConfig = appConfig;
        this.modelFactory = modelFactory;
        this.environment = environment;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        
        this.cacheFile = Paths.get(appConfig.getQdrant().getDataPath(), "semantic_cache.json");
        loadCache();
    }

    public static double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size() || a.isEmpty()) return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double valA = a.get(i);
            double valB = b.get(i);
            dotProduct += valA * valB;
            normA += valA * valA;
            normB += valB * valB;
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<Double> embed(String text) {
        Embedding embedding = modelFactory.getEmbeddingModel().embed(text).content();
        List<Double> list = new ArrayList<>();
        for (float f : embedding.vector()) {
            list.add((double) f);
        }
        return list;
    }

    public CacheResult get(String question, String namespace) {
        CacheResult l1 = getRedis(question, namespace);
        if (l1.isHit()) {
            lock.lock();
            try {
                hitCount++;
            } finally {
                lock.unlock();
            }
            return l1;
        }

        List<Double> qVec;
        try {
            qVec = embed(question);
        } catch (Exception e) {
            log.error("[SemanticCache] Embedding failed during lookup: {}", e.getMessage());
            return new CacheResult(false, null);
        }

        long now = Instant.now().getEpochSecond();
        long ttlSeconds = (long) appConfig.getCache().getTtlDays() * 24 * 3600;

        double bestScore = 0.0;
        String bestAnswer = null;

        lock.lock();
        try {
            for (CacheEntry entry : entries) {
                if (!Objects.equals(
                        Optional.ofNullable(entry.getNamespace()).orElse(""),
                        Optional.ofNullable(namespace).orElse("")
                )) {
                    continue;
                }
                // Skip expired entries
                if (now - entry.getCreatedAt() > ttlSeconds) {
                    continue;
                }
                double score = cosineSimilarity(qVec, entry.getVector());
                if (score > bestScore) {
                    bestScore = score;
                    bestAnswer = entry.getAnswer();
                }
            }
        } finally {
            lock.unlock();
        }

        double threshold = appConfig.getCache().getThreshold();
        if (bestScore >= threshold && bestAnswer != null) {
            log.info("[SemanticCache] HIT similarity={}, q={}", String.format("%.4f", bestScore),
                    question.substring(0, Math.min(question.length(), 60)));
            lock.lock();
            try {
                hitCount++;
            } finally {
                lock.unlock();
            }
            return new CacheResult(true, bestAnswer);
        }

        log.info("[SemanticCache] MISS similarity={}, q={}", String.format("%.4f", bestScore), 
                question.substring(0, Math.min(question.length(), 60)));
        lock.lock();
        try {
            missCount++;
        } finally {
            lock.unlock();
        }
        return new CacheResult(false, null);
    }

    public void set(String question, String answer, String namespace) {
        setRedis(question, answer, namespace);

        List<Double> qVec;
        try {
            qVec = embed(question);
        } catch (Exception e) {
            log.error("[SemanticCache] Embedding failed during insertion: {}", e.getMessage());
            return;
        }

        CacheEntry entry = new CacheEntry();
        entry.setNamespace(namespace);
        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setVector(qVec);
        entry.setCreatedAt(Instant.now().getEpochSecond());

        lock.lock();
        try {
            entries.add(entry);
            int maxEntries = appConfig.getCache().getMaxEntries();
            if (entries.size() > maxEntries) {
                // Remove oldest entries
                int toRemove = entries.size() - maxEntries;
                for (int i = 0; i < toRemove; i++) {
                    entries.remove(0);
                }
            }
            saveCache();
        } finally {
            lock.unlock();
        }

        log.info("[SemanticCache] SET q={}", question.substring(0, Math.min(question.length(), 60)));
    }

    public Map<String, Object> stats() {
        lock.lock();
        try {
            int total = entries.size();
            long now = Instant.now().getEpochSecond();
            long ttlSeconds = (long) appConfig.getCache().getTtlDays() * 24 * 3600;
            
            long validCount = entries.stream()
                    .filter(e -> (now - e.getCreatedAt()) <= ttlSeconds)
                    .count();

            Map<String, Integer> namespaceDistribution = new HashMap<>();
            for (CacheEntry entry : entries) {
                String namespace = entry.getNamespace();
                String ns = namespace == null || namespace.isEmpty() ? "legacy" : namespace;
                namespaceDistribution.put(ns, namespaceDistribution.getOrDefault(ns, 0) + 1);
            }

            int totalQueries = hitCount + missCount;
            double hitRate = totalQueries > 0 ? (double) hitCount / totalQueries : 0.0;
            // Round to 4 decimal places
            hitRate = Math.round(hitRate * 10000.0) / 10000.0;

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("size", total);
            stats.put("valid", validCount);
            stats.put("threshold", appConfig.getCache().getThreshold());
            stats.put("hit_count", hitCount);
            stats.put("miss_count", missCount);
            stats.put("hit_rate", hitRate);
            stats.put("namespaces", namespaceDistribution);
            return stats;
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            entries.clear();
            hitCount = 0;
            missCount = 0;
            saveCache();
            log.info("[SemanticCache] CLEARED");
        } finally {
            lock.unlock();
        }
    }

    // --- File Storage ---

    private void loadCache() {
        if (!Files.exists(cacheFile)) {
            return;
        }
        lock.lock();
        try {
            CacheData data = objectMapper.readValue(cacheFile.toFile(), CacheData.class);
            if (data != null && data.getEntries() != null) {
                entries.addAll(data.getEntries());
                log.info("[SemanticCache] Loaded {} entries from disk", entries.size());
            }
        } catch (IOException e) {
            log.warn("[SemanticCache] Failed to load cache file: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void saveCache() {
        try {
            Files.createDirectories(cacheFile.getParent());
            CacheData data = new CacheData();
            data.setEntries(new ArrayList<>(entries));
            objectMapper.writeValue(cacheFile.toFile(), data);
        } catch (IOException e) {
            log.error("[SemanticCache] Failed to save cache file", e);
        }
    }

    private CacheResult getRedis(String question, String namespace) {
        if (!redisEnabled()) {
            return new CacheResult(false, null);
        }
        try {
            String answer = withRedis(connection -> {
                redisAuthIfNeeded(connection);
                writeCommand(connection.out(), "GET", redisKey(question, namespace));
                return readResp(connection.in());
            });
            if (answer != null && !answer.isBlank()) {
                log.info("[SemanticCache] Redis L1 HIT q={}", question.substring(0, Math.min(question.length(), 60)));
                return new CacheResult(true, answer);
            }
        } catch (Exception error) {
            log.debug("[SemanticCache] Redis L1 lookup skipped: {}", error.getMessage());
        }
        return new CacheResult(false, null);
    }

    private void setRedis(String question, String answer, String namespace) {
        if (!redisEnabled() || answer == null || answer.isBlank()) {
            return;
        }
        try {
            withRedis(connection -> {
                redisAuthIfNeeded(connection);
                long ttlSeconds = (long) appConfig.getCache().getTtlDays() * 24 * 3600;
                writeCommand(connection.out(), "SETEX", redisKey(question, namespace), Long.toString(ttlSeconds), answer);
                readResp(connection.in());
                return null;
            });
            log.info("[SemanticCache] Redis L1 SET q={}", question.substring(0, Math.min(question.length(), 60)));
        } catch (Exception error) {
            log.debug("[SemanticCache] Redis L1 write skipped: {}", error.getMessage());
        }
    }

    private boolean redisEnabled() {
        return appConfig.getCache().isRedisEnabled() || appConfig.getSmartcloud().isRedisEnabled();
    }

    private String redisKey(String question, String namespace) {
        String normalized = Optional.ofNullable(question).orElse("")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        String raw = Optional.ofNullable(namespace).orElse("default") + "|" + normalized;
        return "smartcloud:l1:" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private <T> T withRedis(RedisCallback<T> callback) throws Exception {
        if (environment == null) {
            return null;
        }
        String host = environment.getProperty("spring.data.redis.host", "localhost");
        int port = Integer.parseInt(environment.getProperty("spring.data.redis.port", "6379"));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            socket.setSoTimeout(1000);
            return callback.execute(new RedisConnection(socket.getInputStream(), socket.getOutputStream()));
        }
    }

    private void redisAuthIfNeeded(RedisConnection connection) throws IOException {
        String password = environment == null ? "" : environment.getProperty("spring.data.redis.password", "");
        if (password != null && !password.isBlank()) {
            writeCommand(connection.out(), "AUTH", password);
            readResp(connection.in());
        }
    }

    private void writeCommand(OutputStream out, String... args) throws IOException {
        out.write(("*" + args.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String arg : args) {
            byte[] bytes = Optional.ofNullable(arg).orElse("").getBytes(StandardCharsets.UTF_8);
            out.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
    }

    private String readResp(InputStream in) throws IOException {
        String line = readLine(in);
        if (line == null || line.isEmpty()) {
            return null;
        }
        char prefix = line.charAt(0);
        String body = line.substring(1);
        if (prefix == '+') {
            return body;
        }
        if (prefix == '-') {
            throw new IOException(body);
        }
        if (prefix == ':') {
            return body;
        }
        if (prefix == '$') {
            int length = Integer.parseInt(body);
            if (length < 0) {
                return null;
            }
            byte[] bytes = in.readNBytes(length);
            in.readNBytes(2);
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return body;
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        int previous = -1;
        while (true) {
            int current = in.read();
            if (current == -1) {
                return builder.length() == 0 ? null : builder.toString();
            }
            if (previous == '\r' && current == '\n') {
                builder.setLength(builder.length() - 1);
                return builder.toString();
            }
            builder.append((char) current);
            previous = current;
        }
    }

    private record RedisConnection(InputStream in, OutputStream out) {
    }

    @FunctionalInterface
    private interface RedisCallback<T> {
        T execute(RedisConnection connection) throws Exception;
    }

    @Data
    public static class CacheResult {
        private final boolean hit;
        private final String answer;
    }
}
