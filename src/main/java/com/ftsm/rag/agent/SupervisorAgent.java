package com.ftsm.rag.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftsm.rag.model.ConversationMessage;
import com.ftsm.rag.service.ModelFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class SupervisorAgent {

    private static final String ROUTER_PROMPT = """
            You are the supervisor in SmartCloud, a multi-agent cloud service customer support platform.

            Return only compact JSON:
            {"route":"DIRECT|PRODUCT_TECH|FINANCE_ORDER|ICP_SERVICE|OPS_MARKETING|DEEP_RESEARCH|KNOWLEDGE_RAG|PROCEDURE_RAG|WEB_REFRESH","query":"standalone query","reason":"short reason"}

            Routing rules:
            - DIRECT: greetings, thanks, writing help, translation, or general non-cloud questions.
            - PRODUCT_TECH: cloud product consultation, ECS, GPU, database, storage, network, Kubernetes, architecture, troubleshooting, or product configuration.
            - FINANCE_ORDER: billing, orders, invoices, refunds, payment, cost analysis, renewal, or account spending.
            - ICP_SERVICE: ICP filing, domain filing, website filing, filing materials, filing status, or filing change procedure.
            - OPS_MARKETING: product sharing, campaigns, promotion copy, posters, H5 landing pages, product recommendations, or marketing assets.
            - DEEP_RESEARCH: industry research, technical comparison, solution selection, architecture report, market analysis, or long-form investigation.
            - KNOWLEDGE_RAG: factual knowledge-base questions that do not fit a specialized cloud agent.
            - PROCEDURE_RAG: process questions retained for compatibility with the legacy education knowledge base.
            - WEB_REFRESH: user explicitly asks to crawl, update, refresh, or check the latest website content.

            Never answer product, policy, or billing facts from memory. Rewrite follow-up questions into standalone search queries.
            """;

    private final ModelFactory modelFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupervisorAgent(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    public Mono<AgentDecision> decide(AgentContext context) {
        return Mono.fromCallable(() -> decideBlocking(context))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    log.warn("Supervisor agent failed, using conservative fallback: {}", error.getMessage());
                    return Mono.just(fallbackDecision(context));
                });
    }

    private AgentDecision decideBlocking(AgentContext context) throws Exception {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(ROUTER_PROMPT));
        ConversationMessageMapper.appendRecentHistory(messages, context.history());
        messages.add(UserMessage.from(context.userMessage()));

        Response<AiMessage> response = modelFactory.getChatModel().generate(messages);
        String raw = response.content() == null ? "" : response.content().text();
        return parseDecision(raw, context);
    }

    private AgentDecision parseDecision(String raw, AgentContext context) throws Exception {
        String json = extractJson(raw);
        JsonNode node = objectMapper.readTree(json);
        AgentRoute route = parseRoute(node.path("route").asText(""));
        String query = node.path("query").asText(context.userMessage()).trim();
        String reason = node.path("reason").asText("supervisor route").trim();
        if (query.isEmpty()) {
            query = context.userMessage();
        }
        return route == AgentRoute.DIRECT
                ? AgentDecision.direct(query, reason)
                : AgentDecision.rag(route, query, reason);
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private AgentRoute parseRoute(String raw) {
        try {
            return AgentRoute.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return AgentRoute.KNOWLEDGE_RAG;
        }
    }

    private AgentDecision fallbackDecision(AgentContext context) {
        String query = contextualizeQuery(context.userMessage(), context.history());
        if (isCasualMessage(query)) {
            return AgentDecision.direct(query, "casual message fallback");
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        if (normalized.contains("crawl") || normalized.contains("refresh")
                || normalized.contains("update website") || normalized.contains("latest website")) {
            return AgentDecision.rag(AgentRoute.WEB_REFRESH, query, "website refresh fallback");
        }
        if (containsAny(normalized, "bill", "billing", "invoice", "order", "refund", "payment", "cost", "账单", "订单", "发票", "退款", "费用", "续费")) {
            return AgentDecision.rag(AgentRoute.FINANCE_ORDER, query, "finance/order fallback");
        }
        if (containsAny(normalized, "icp", "filing", "domain", "备案", "域名", "接入备案", "变更备案")) {
            return AgentDecision.rag(AgentRoute.ICP_SERVICE, query, "ICP service fallback");
        }
        if (containsAny(normalized, "poster", "share", "campaign", "marketing", "promotion", "landing page", "推广", "海报", "活动", "分享", "营销")) {
            return AgentDecision.rag(AgentRoute.OPS_MARKETING, query, "marketing fallback");
        }
        if (containsAny(normalized, "research", "report", "compare", "comparison", "analysis", "selection", "调研", "报告", "对比", "选型", "分析")) {
            return AgentDecision.rag(AgentRoute.DEEP_RESEARCH, query, "deep research fallback");
        }
        if (containsAny(normalized, "ecs", "gpu", "cloud", "server", "database", "storage", "kubernetes", "network", "load balancer", "云服务器", "云数据库", "对象存储", "负载均衡", "容器", "大模型部署")) {
            return AgentDecision.rag(AgentRoute.PRODUCT_TECH, query, "product tech fallback");
        }
        if (normalized.contains("how") || normalized.contains("apply") || normalized.contains("renew")
                || normalized.contains("procedure") || normalized.contains("steps")
                || normalized.contains("visa") || normalized.contains("registration")) {
            return AgentDecision.rag(AgentRoute.PROCEDURE_RAG, query, "procedure fallback");
        }
        return AgentDecision.rag(AgentRoute.KNOWLEDGE_RAG, query, "knowledge fallback");
    }

    static boolean isCasualMessage(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return normalized.matches(
                "^(hi|hello|hey|good morning|good afternoon|good evening|thanks|thank you|你好|您好|谢谢|多谢)[!.。！ ]*$"
        );
    }

    static String contextualizeQuery(String query, List<ConversationMessage> history) {
        if (history == null || history.isEmpty()) {
            return query;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        boolean likelyFollowUp = query.length() < 40
                || normalized.matches(".*\\b(it|that|this|they|them|there|those|its)\\b.*")
                || normalized.matches(".*(这个|那个|它|他们|那里|上述|刚才).*");
        if (!likelyFollowUp) {
            return query;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            ConversationMessage message = history.get(i);
            if ("user".equalsIgnoreCase(message.getRole())) {
                return message.getContent() + "\nFollow-up question: " + query;
            }
        }
        return query;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
