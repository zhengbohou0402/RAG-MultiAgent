package com.ftsm.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String projectRoot = "";
    private DashScopeProperties dashscope = new DashScopeProperties();
    private QdrantProperties qdrant = new QdrantProperties();
    private CacheProperties cache = new CacheProperties();
    private ConversationProperties conversation = new ConversationProperties();
    private SmartCloudProperties smartcloud = new SmartCloudProperties();
    private CrawlerProperties crawler = new CrawlerProperties();

    @Data
    public static class DashScopeProperties {
        private String apiKey = "";
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String chatModel = "qwen-turbo";
        private String embeddingModel = "text-embedding-v3";
        private String imageModel = "qwen-vl-plus";
        private String rerankModel = "gte-rerank-v2";
    }

    @Data
    public static class QdrantProperties {
        private String host = "localhost";
        private int port = 6334;
        private boolean useTls = false;
        private String apiKey = "";
        private String collectionName = "smartcloud_serviceops";
        private String persistDirectory = "qdrant_db_smartcloud";
        private String sparseModel = "Qdrant/bm25";
        private int k = 6;
        private int hybridSearchLimit = 20;
        private long hybridSearchTimeoutMs = 15_000;
        private int chunkSize = 800;
        private int chunkOverlap = 120;
        private String dataPath = "data/smartcloud_kb";
        private boolean autoStartLocal = true;
        private String localExecutable = "qdrant_local/qdrant.exe";
        private String localStoragePath = "qdrant_local/storage";
        private String lexicalIndexPath = "lucene_local";
        private List<String> allowKnowledgeFileTypes = List.of("txt", "pdf", "png", "jpg", "jpeg", "webp", "gif");
    }

    @Data
    public static class CacheProperties {
        private double threshold = 0.92;
        private int ttlDays = 7;
        private int maxEntries = 500;
        private boolean redisEnabled = false;
    }

    @Data
    public static class ConversationProperties {
        private int maxConversations = 200;
        private int maxMessages = 40;
        private int maxHistoryTurns = 5;
        private boolean mysqlEnabled = false;
        private boolean mongoEnabled = false;
    }

    @Data
    public static class SmartCloudProperties {
        private String defaultTenantId = "tenant-demo";
        private String defaultUserId = "demo-admin";
        private String jwtSecret = "smartcloud-local-demo-secret-change-me-please";
        private int tokenTtlMinutes = 720;
        private boolean mysqlEnabled = false;
        private boolean mongoEnabled = false;
        private boolean redisEnabled = false;
        private String mcpEndpoint = "http://127.0.0.1:8000/mcp";
        private String a2aEndpoint = "http://127.0.0.1:8000";
        private String marketingAssetsPath = "data/smartcloud/assets";
    }

    @Data
    public static class CrawlerProperties {
        private boolean enabled = false;
        private int intervalHours = 168;
        private int maxPages = 80;
        private boolean browserEnabled = true;
        private boolean browserAutoDownload = false;
        private boolean staticFallback = true;
        private boolean staticTlsFallback = true;
        private boolean headless = true;
        private int pageTimeoutSeconds = 40;
        private int pageDelayMillis = 800;
        private int minContentChars = 200;
        private int maxQueuedLinks = 2000;
        private String outputFilename = "smartcloud_official_website.txt";
        private List<String> browserChannels = List.of("msedge", "chrome", "");
        private List<String> allowedUrlPrefixes = List.of(
                "https://docs.smartcloud.local/",
                "https://support.smartcloud.local/",
                "https://status.smartcloud.local/"
        );
        private List<String> seedUrls = List.of(
                "https://docs.smartcloud.local/products/ecs",
                "https://docs.smartcloud.local/products/gpu-ai-computing",
                "https://docs.smartcloud.local/products/managed-database",
                "https://docs.smartcloud.local/products/object-storage",
                "https://docs.smartcloud.local/networking/vpc-security-group",
                "https://support.smartcloud.local/billing/invoices-renewal-refund",
                "https://support.smartcloud.local/icp/checklist",
                "https://support.smartcloud.local/tickets/sla-escalation",
                "https://docs.smartcloud.local/observability/prometheus-grafana",
                "https://docs.smartcloud.local/marketing/h5-campaign-assets"
        );
        private List<String> skipExtensions = List.of(
                ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar",
                ".jpg", ".jpeg", ".png", ".gif", ".webp"
        );
    }
}
