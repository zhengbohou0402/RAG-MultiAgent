package com.ftsm.rag.service;

import com.ftsm.rag.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class A2AClientService {

    private final AppConfig appConfig;
    private final SmartCloudProductCatalogService productCatalogService;
    private final SmartCloudMetricsService metricsService;

    public A2AClientService(AppConfig appConfig,
                            SmartCloudProductCatalogService productCatalogService,
                            SmartCloudMetricsService metricsService) {
        this.appConfig = appConfig;
        this.productCatalogService = productCatalogService;
        this.metricsService = metricsService;
    }

    public SmartCloudProductCatalogService.ProductProfile requestProductProfile(String query) {
        metricsService.protocol("a2a", "message:send");
        try {
            Map<String, Object> response = WebClient.builder()
                    .baseUrl(appConfig.getSmartcloud().getA2aEndpoint())
                    .build()
                    .post()
                    .uri("/message:send")
                    .bodyValue(Map.of(
                            "jsonrpc", "2.0",
                            "id", UUID.randomUUID().toString(),
                            "method", "message/send",
                            "params", Map.of(
                                    "message", Map.of(
                                            "role", "user",
                                            "parts", List.of(Map.of("kind", "text", "text", query == null ? "" : query))
                                    )
                            )
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofMillis(900));
            SmartCloudProductCatalogService.ProductProfile parsed = parseProfile(response);
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception error) {
            log.debug("A2A product lookup fell back to local catalog: {}", error.getMessage());
        }
        return productCatalogService.productFor(query);
    }

    @SuppressWarnings("unchecked")
    private SmartCloudProductCatalogService.ProductProfile parseProfile(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object result = response.get("result");
        if (!(result instanceof Map<?, ?> resultMap)) {
            return null;
        }
        Object metadata = resultMap.get("metadata");
        if (!(metadata instanceof Map<?, ?> metadataMap)) {
            return null;
        }
        Object product = metadataMap.get("product");
        if (!(product instanceof Map<?, ?> productMap)) {
            return null;
        }
        Object highlights = productMap.get("highlights");
        List<String> highlightList = highlights instanceof List<?> list
                ? list.stream().map(Object::toString).toList()
                : List.of();
        return new SmartCloudProductCatalogService.ProductProfile(
                value(productMap, "name", "SmartCloud ECS Standard"),
                value(productMap, "category", "compute"),
                highlightList,
                value(productMap, "promotion", "Starter package available."),
                value(productMap, "use_case", "Cloud service operations.")
        );
    }

    private String value(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : value.toString();
    }
}
