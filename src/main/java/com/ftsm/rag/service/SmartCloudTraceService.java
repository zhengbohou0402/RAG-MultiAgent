package com.ftsm.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftsm.rag.config.AppConfig;
import com.ftsm.rag.model.SmartCloudUserContext;
import com.ftsm.rag.model.TraceRecord;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SmartCloudTraceService {

    private final AppConfig appConfig;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final List<TraceRecord> memoryTraces = new CopyOnWriteArrayList<>();

    public SmartCloudTraceService(AppConfig appConfig, Environment environment, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    public TraceRecord record(String conversationId, SmartCloudUserContext user, String route,
                              String selectedAgent, String reason, List<String> nodes,
                              List<Map<String, Object>> toolCalls, long latencyMs) {
        SmartCloudUserContext effectiveUser = user == null ? SmartCloudUserContext.demo() : user;
        TraceRecord trace = new TraceRecord(
                java.util.UUID.randomUUID().toString(),
                conversationId,
                effectiveUser.tenantId(),
                effectiveUser.userId(),
                route,
                selectedAgent,
                reason,
                nodes == null ? List.of() : List.copyOf(nodes),
                toolCalls == null ? List.of() : List.copyOf(toolCalls),
                Instant.now().getEpochSecond(),
                latencyMs
        );
        memoryTraces.add(trace);
        while (memoryTraces.size() > 200) {
            memoryTraces.remove(0);
        }
        writeMongo(trace);
        return trace;
    }

    public List<TraceRecord> list() {
        List<TraceRecord> copy = new ArrayList<>(memoryTraces);
        copy.sort(Comparator.comparingLong(TraceRecord::createdAt).reversed());
        return copy;
    }

    public List<TraceRecord> byConversation(String conversationId) {
        return list().stream()
                .filter(trace -> trace.conversationId().equals(conversationId))
                .toList();
    }

    public Map<String, Object> toolCall(String protocol, String method, String detail) {
        Map<String, Object> toolCall = new LinkedHashMap<>();
        toolCall.put("protocol", protocol);
        toolCall.put("method", method);
        toolCall.put("detail", detail);
        toolCall.put("at", Instant.now().getEpochSecond());
        return toolCall;
    }

    private void writeMongo(TraceRecord trace) {
        if (!appConfig.getSmartcloud().isMongoEnabled()) {
            return;
        }
        try {
            String uri = environment.getProperty("spring.data.mongodb.uri");
            MongoClient client = MongoClients.create(uri);
            org.bson.Document document = org.bson.Document.parse(objectMapper.writeValueAsString(trace));
            Mono.from(client.getDatabase("smartcloud")
                    .getCollection("agent_traces", org.bson.Document.class)
                    .insertOne(document))
                    .doOnError(error -> log.warn("Mongo trace write failed, keeping trace in memory: {}", error.getMessage()))
                    .doFinally(signal -> client.close())
                    .subscribe();
        } catch (Exception error) {
            log.warn("Mongo trace write failed, keeping trace in memory: {}", error.getMessage());
        }
    }

    public Mono<Void> recordToolTrace(String conversationId, SmartCloudUserContext user,
                                      String protocol, String method, String detail) {
        return Mono.fromRunnable(() -> record(
                conversationId == null ? "protocol-demo" : conversationId,
                user,
                protocol,
                protocol + "-adapter",
                method,
                List.of(protocol, method),
                List.of(toolCall(protocol, method, detail)),
                0
        ));
    }
}
