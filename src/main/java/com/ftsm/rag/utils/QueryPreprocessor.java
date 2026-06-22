package com.ftsm.rag.utils;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class QueryPreprocessor {

    private final int maxExpansions = 3;

    private static final List<Pattern> NAVIGATION_PATTERNS = List.of(
            Pattern.compile("\\bwhere\\s+can\\s+i\\s+(find|view|check|see|get|look\\s+for)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhere\\s+to\\s+(find|view|check|see|get|look\\s+for)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhow\\s+can\\s+i\\s+(find|view|check|see|get|look\\s+for)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhow\\s+do\\s+i\\s+(find|view|check|see|get|look\\s+for)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcan\\s+you\\s+(tell|show|give)\\s+me\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bgive\\s+me\\s+(the\\s+)?(information|details|summary)\\s+(about|on|for)\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final List<TopicRewriteRule> TOPIC_REWRITE_RULES = List.of(
            new TopicRewriteRule(List.of("ecs", "cloud server", "云服务器", "服务器", "instance"),
                    "SmartCloud ECS instance creation configuration security group elastic compute"),
            new TopicRewriteRule(List.of("gpu", "ai computing", "大模型", "llm", "inference", "training"),
                    "SmartCloud GPU AI Computing model deployment inference training vector database"),
            new TopicRewriteRule(List.of("bill", "billing", "invoice", "cost", "renewal", "refund", "账单", "发票", "续费", "退款", "成本"),
                    "SmartCloud billing invoice order renewal refund cost optimization reserved instance"),
            new TopicRewriteRule(List.of("icp", "备案", "域名", "website filing"),
                    "SmartCloud ICP filing checklist domain company license server access proof"),
            new TopicRewriteRule(List.of("database", "mysql", "mongo", "redis", "数据库"),
                    "SmartCloud managed database MySQL MongoDB Redis backup replica migration"),
            new TopicRewriteRule(List.of("object storage", "oss", "bucket", "storage", "对象存储"),
                    "SmartCloud object storage bucket lifecycle CDN static website access key"),
            new TopicRewriteRule(List.of("vpc", "network", "security group", "load balancer", "网络", "安全组", "负载均衡"),
                    "SmartCloud VPC security group load balancer subnet public IP firewall"),
            new TopicRewriteRule(List.of("ticket", "case", "support", "工单", "客服", "故障"),
                    "SmartCloud support ticket severity SLA escalation troubleshooting runbook"),
            new TopicRewriteRule(List.of("monitor", "observability", "prometheus", "grafana", "告警", "监控"),
                    "SmartCloud observability Prometheus Grafana alert trace metric dashboard"),
            new TopicRewriteRule(List.of("marketing", "promotion", "poster", "h5", "推广", "海报", "活动"),
                    "SmartCloud marketing package poster H5 landing page campaign copy product promotion")
    );

    private static final Map<String, List<String>> SYNONYM_MAP = new LinkedHashMap<>();

    static {
        SYNONYM_MAP.put("云服务器", List.of("ECS", "cloud server", "elastic compute"));
        SYNONYM_MAP.put("服务器", List.of("ECS instance", "cloud server", "security group"));
        SYNONYM_MAP.put("gpu", List.of("AI Computing", "LLM inference", "model deployment"));
        SYNONYM_MAP.put("大模型", List.of("LLM", "GPU AI Computing", "RAG deployment"));
        SYNONYM_MAP.put("账单", List.of("billing", "invoice", "cost optimization"));
        SYNONYM_MAP.put("发票", List.of("invoice", "billing account", "tax invoice"));
        SYNONYM_MAP.put("续费", List.of("renewal", "subscription", "reserved instance"));
        SYNONYM_MAP.put("备案", List.of("ICP filing", "domain filing", "website filing"));
        SYNONYM_MAP.put("域名", List.of("domain", "ICP filing", "DNS"));
        SYNONYM_MAP.put("工单", List.of("support ticket", "case", "SLA escalation"));
        SYNONYM_MAP.put("故障", List.of("incident", "troubleshooting", "root cause"));
        SYNONYM_MAP.put("监控", List.of("observability", "Prometheus", "Grafana alert"));
        SYNONYM_MAP.put("告警", List.of("alert", "metric threshold", "incident notification"));
        SYNONYM_MAP.put("对象存储", List.of("object storage", "bucket", "CDN"));
        SYNONYM_MAP.put("数据库", List.of("managed database", "MySQL", "MongoDB", "Redis"));
        SYNONYM_MAP.put("安全组", List.of("security group", "firewall", "inbound rule"));
        SYNONYM_MAP.put("推广", List.of("marketing campaign", "poster", "H5 landing page"));
        SYNONYM_MAP.put("海报", List.of("poster", "marketing asset", "campaign creative"));
        SYNONYM_MAP.put("billing", List.of("invoice", "order", "renewal", "cost optimization"));
        SYNONYM_MAP.put("invoice", List.of("billing", "tax invoice", "unpaid amount"));
        SYNONYM_MAP.put("support", List.of("ticket", "SLA", "escalation"));
        SYNONYM_MAP.put("observability", List.of("metrics", "trace", "Prometheus", "Grafana"));
    }

    public String traditionalToSimplified(String text) {
        if (text == null) {
            return null;
        }
        try {
            return ZhConverterUtil.toSimple(text);
        } catch (Exception e) {
            log.warn("ZhConverterUtil failed to translate traditional to simplified: {}", e.getMessage());
            return text;
        }
    }

    public String normalize(String text) {
        if (text == null) {
            return null;
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        return normalized.replaceAll("\\s+", " ").trim();
    }

    public List<String> rewriteStudentQuery(String text) {
        List<String> rewrites = new ArrayList<>();
        String cleaned = text == null ? "" : text;
        for (Pattern pattern : NAVIGATION_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceAll(" ");
        }
        cleaned = cleaned.replaceAll("[?？。！!]+", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("^[,.;:]+|[,.;:]+$", "");

        if (!cleaned.isEmpty() && !cleaned.equalsIgnoreCase(text)) {
            rewrites.add(cleaned);
        }

        String lower = text == null ? "" : text.toLowerCase();
        for (TopicRewriteRule rule : TOPIC_REWRITE_RULES) {
            for (String keyword : rule.keywords) {
                if (lower.contains(keyword.toLowerCase())) {
                    if (!rewrites.contains(rule.rewrite)) {
                        rewrites.add(rule.rewrite);
                    }
                    break;
                }
            }
        }
        return rewrites;
    }

    public List<String> expandSynonyms(String text) {
        List<String> expansions = new ArrayList<>();
        String lower = text == null ? "" : text.toLowerCase();
        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                for (String synonym : entry.getValue().subList(0, Math.min(entry.getValue().size(), maxExpansions))) {
                    String candidate = (text + " " + synonym).trim();
                    if (!expansions.contains(candidate) && !candidate.equalsIgnoreCase(text)) {
                        expansions.add(candidate);
                    }
                }
                if (expansions.size() >= maxExpansions) {
                    break;
                }
            }
        }
        return expansions;
    }

    public List<String> process(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String simplified = traditionalToSimplified(query);
        String normalized = normalize(simplified);

        List<String> queries = new ArrayList<>();
        queries.add(normalized);
        for (String rewrite : rewriteStudentQuery(normalized)) {
            String value = normalize(rewrite);
            if (!value.isEmpty() && !queries.contains(value)) {
                queries.add(value);
            }
        }
        for (String expansion : expandSynonyms(normalized)) {
            String value = normalize(expansion);
            if (!value.isEmpty() && !queries.contains(value)) {
                queries.add(value);
            }
        }
        if (queries.size() > 1 + maxExpansions) {
            return queries.subList(0, 1 + maxExpansions);
        }
        return queries;
    }

    private static class TopicRewriteRule {
        final List<String> keywords;
        final String rewrite;

        TopicRewriteRule(List<String> keywords, String rewrite) {
            this.keywords = keywords;
            this.rewrite = rewrite;
        }
    }
}
