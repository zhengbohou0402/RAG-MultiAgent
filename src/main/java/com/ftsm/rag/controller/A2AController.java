package com.ftsm.rag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftsm.rag.config.AppConfig;
import com.ftsm.rag.service.SmartCloudMetricsService;
import com.ftsm.rag.service.SmartCloudProductCatalogService;
import com.ftsm.rag.service.SmartCloudTraceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
public class A2AController {

    private final AppConfig appConfig;
    private final SmartCloudProductCatalogService productCatalogService;
    private final SmartCloudMetricsService metricsService;
    private final SmartCloudTraceService traceService;
    private final ObjectMapper objectMapper;

    public A2AController(AppConfig appConfig,
                         SmartCloudProductCatalogService productCatalogService,
                         SmartCloudMetricsService metricsService,
                         SmartCloudTraceService traceService,
                         ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.productCatalogService = productCatalogService;
        this.metricsService = metricsService;
        this.traceService = traceService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> agentCard() {
        return Map.of(
                "name", "SmartCloud ProductTech Agent",
                "description", "A2A demo agent that returns cloud product context for marketing and support agents.",
                "url", appConfig.getSmartcloud().getA2aEndpoint(),
                "version", "1.0.0",
                "capabilities", Map.of("streaming", true),
                "skills", List.of(Map.of(
                        "id", "product-info",
                        "name", "Product information lookup",
                        "description", "Finds product highlights, promotion, and best-fit scenarios."
                ))
        );
    }

    @PostMapping("/message:send")
    public Map<String, Object> send(@RequestBody Map<String, Object> request) {
        metricsService.protocol("a2a", "message:send");
        String query = extractText(request);
        SmartCloudProductCatalogService.ProductProfile profile = productCatalogService.productFor(query);
        traceService.recordToolTrace("a2a-session", com.ftsm.rag.model.SmartCloudUserContext.demo(),
                "a2a", "message:send", profile.name()).subscribe();
        Object id = request.getOrDefault("id", UUID.randomUUID().toString());
        return jsonRpcSuccess(id, taskResult(query, profile, "completed"));
    }

    @PostMapping(value = "/message:stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody Map<String, Object> request) {
        metricsService.protocol("a2a", "message:stream");
        String query = extractText(request);
        SmartCloudProductCatalogService.ProductProfile profile = productCatalogService.productFor(query);
        traceService.recordToolTrace("a2a-session", com.ftsm.rag.model.SmartCloudUserContext.demo(),
                "a2a", "message:stream", profile.name()).subscribe();
        Object id = request.getOrDefault("id", UUID.randomUUID().toString());
        return Flux.just(
                event("message", jsonRpcSuccess(id, taskResult(query, profile, "working"))),
                event("completed", jsonRpcSuccess(id, taskResult(query, profile, "completed")))
        );
    }

    private Map<String, Object> taskResult(String query, SmartCloudProductCatalogService.ProductProfile profile, String state) {
        String text = "ProductTechAgent recommends " + profile.name()
                + " for query `" + query + "`. " + profile.useCase();
        Map<String, Object> product = productCatalogService.asMap(profile);
        return Map.of(
                "id", UUID.randomUUID().toString(),
                "contextId", "smartcloud-product-demo",
                "status", Map.of("state", state),
                "artifacts", List.of(Map.of(
                        "artifactId", "product-context",
                        "parts", List.of(Map.of("kind", "text", "text", text))
                )),
                "metadata", Map.of(
                        "agent", "ProductTechAgent",
                        "product", product
                )
        );
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> request) {
        Object params = request.get("params");
        if (params instanceof Map<?, ?> paramsMap) {
            Object message = paramsMap.get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object parts = messageMap.get("parts");
                if (parts instanceof List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof Map<?, ?> partMap) {
                        return value(partMap, "text", "");
                    }
                }
            }
        }
        Object message = request.get("message");
        if (message instanceof String text) {
            return text;
        }
        return Objects.toString(request.getOrDefault("query", ""), "");
    }

    private String value(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private Map<String, Object> jsonRpcSuccess(Object id, Map<String, Object> result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private String event(String name, Map<String, Object> payload) {
        try {
            return "event: " + name + "\ndata: " + objectMapper.writeValueAsString(payload) + "\n\n";
        } catch (Exception error) {
            return "event: error\ndata: {\"message\":\"serialization failed\"}\n\n";
        }
    }
}
