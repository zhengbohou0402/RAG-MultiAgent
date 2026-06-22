package com.ftsm.rag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftsm.rag.agent.tool.LocalSmartCloudToolClient;
import com.ftsm.rag.agent.tool.SmartCloudToolClient;
import com.ftsm.rag.model.MarketingAsset;
import com.ftsm.rag.model.MarketingRequest;
import com.ftsm.rag.model.SmartCloudUserContext;
import com.ftsm.rag.service.MarketingAssetService;
import com.ftsm.rag.service.SmartCloudAuthService;
import com.ftsm.rag.service.SmartCloudMetricsService;
import com.ftsm.rag.service.SmartCloudTraceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class McpController {

    private static final String PROTOCOL_VERSION = "2025-06-18";

    private final LocalSmartCloudToolClient toolClient;
    private final MarketingAssetService marketingAssetService;
    private final SmartCloudAuthService authService;
    private final SmartCloudMetricsService metricsService;
    private final SmartCloudTraceService traceService;
    private final ObjectMapper objectMapper;

    public McpController(LocalSmartCloudToolClient toolClient,
                         MarketingAssetService marketingAssetService,
                         SmartCloudAuthService authService,
                         SmartCloudMetricsService metricsService,
                         SmartCloudTraceService traceService,
                         ObjectMapper objectMapper) {
        this.toolClient = toolClient;
        this.marketingAssetService = marketingAssetService;
        this.authService = authService;
        this.metricsService = metricsService;
        this.traceService = traceService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/mcp", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamableHttpInfo() {
        return Flux.just("event: endpoint\ndata: {\"status\":\"ready\",\"transport\":\"streamable-http\"}\n\n");
    }

    @DeleteMapping("/mcp")
    public Map<String, Object> closeSession() {
        return Map.of("status", "closed", "transport", "streamable-http");
    }

    @PostMapping("/mcp")
    public Map<String, Object> handle(@RequestBody Map<String, Object> request,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        Object id = request.get("id");
        String method = Objects.toString(request.get("method"), "");
        SmartCloudUserContext user = authService.resolve(authorization);
        metricsService.protocol("mcp", method.isBlank() ? "unknown" : method);
        traceService.recordToolTrace("mcp-session", user, "mcp", method, "json-rpc request").subscribe();
        try {
            return switch (method) {
                case "initialize" -> success(id, initializeResult());
                case "tools/list" -> success(id, Map.of("tools", tools()));
                case "tools/call" -> success(id, callTool(request, user));
                default -> error(id, -32601, "Unknown MCP method: " + method);
            };
        } catch (Exception error) {
            return error(id, -32000, error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        }
    }

    private Map<String, Object> initializeResult() {
        return Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of("tools", Map.of("listChanged", true)),
                "serverInfo", Map.of("name", "smartcloud-serviceops-mcp", "version", "1.0.0")
        );
    }

    private List<Map<String, Object>> tools() {
        return List.of(
                tool("billing.query", "Query tenant billing, invoices, cost drivers, and optimization suggestions."),
                tool("icp.checklist", "Build an ICP filing checklist for a cloud customer scenario."),
                tool("marketing.generate_package", "Generate campaign copy and poster prompt for a cloud product."),
                tool("research.plan", "Create a deep research plan for technical architecture comparison."),
                tool("h5.generate", "Generate a deterministic H5 landing page and poster asset.")
        );
    }

    private Map<String, Object> tool(String name, String description) {
        return Map.of(
                "name", name,
                "description", description,
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of("type", "string"),
                                "product_name", Map.of("type", "string")
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callTool(Map<String, Object> request, SmartCloudUserContext user) throws Exception {
        Map<String, Object> params = request.get("params") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        String name = Objects.toString(params.get("name"), "");
        Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        String query = firstNonBlank(arguments.get("query"), arguments.get("input"), arguments.get("product_name"));
        Object payload = switch (name) {
            case "billing.query" -> toolClient.queryBilling(query, user);
            case "icp.checklist" -> toolClient.buildIcpChecklist(query);
            case "marketing.generate_package" -> toolClient.createMarketingPackage(query, user);
            case "research.plan" -> toolClient.createResearchPlan(query);
            case "h5.generate" -> generateAsset(arguments);
            default -> throw new IllegalArgumentException("Unknown MCP tool: " + name);
        };
        return Map.of(
                "content", List.of(Map.of("type", "text", "text", objectMapper.writeValueAsString(payload))),
                "isError", false
        );
    }

    private MarketingAsset generateAsset(Map<String, Object> arguments) {
        MarketingRequest request = new MarketingRequest();
        request.setProductName(firstNonBlank(arguments.get("product_name"), arguments.get("query")));
        request.setScenario(firstNonBlank(arguments.get("scenario")));
        request.setAudience(firstNonBlank(arguments.get("audience")));
        return marketingAssetService.generate(request);
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = value == null ? "" : value.toString().trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private Map<String, Object> success(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }
}
