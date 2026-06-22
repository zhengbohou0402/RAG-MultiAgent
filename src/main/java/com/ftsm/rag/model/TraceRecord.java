package com.ftsm.rag.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record TraceRecord(
        String id,
        @JsonProperty("conversation_id") String conversationId,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("user_id") String userId,
        String route,
        @JsonProperty("selected_agent") String selectedAgent,
        String reason,
        List<String> nodes,
        @JsonProperty("tool_calls") List<Map<String, Object>> toolCalls,
        @JsonProperty("created_at") long createdAt,
        @JsonProperty("latency_ms") long latencyMs
) {
}
