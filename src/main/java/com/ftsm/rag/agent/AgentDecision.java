package com.ftsm.rag.agent;

import java.io.Serializable;

public record AgentDecision(
        AgentRoute route,
        String query,
        String reason
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static AgentDecision direct(String query, String reason) {
        return new AgentDecision(AgentRoute.DIRECT, query, reason);
    }

    public static AgentDecision rag(AgentRoute route, String query, String reason) {
        return new AgentDecision(route, query, reason);
    }
}
