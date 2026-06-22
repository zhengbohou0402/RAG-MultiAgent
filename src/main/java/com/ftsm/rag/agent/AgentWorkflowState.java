package com.ftsm.rag.agent;

import com.ftsm.rag.model.ConversationMessage;
import org.bsc.langgraph4j.state.AgentState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentWorkflowState extends AgentState {

    static final String USER_MESSAGE = "userMessage";
    static final String HISTORY = "history";
    static final String DECISION = "decision";
    static final String SELECTED_AGENT = "selectedAgent";
    static final String TRACE = "trace";
    static final String GUARD_MESSAGE = "guardMessage";

    public AgentWorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    public String userMessage() {
        return value(USER_MESSAGE).map(String.class::cast).orElse("");
    }

    @SuppressWarnings("unchecked")
    public List<ConversationMessage> history() {
        return value(HISTORY)
                .map(value -> (List<ConversationMessage>) value)
                .orElseGet(List::of);
    }

    public AgentDecision decision() {
        return value(DECISION).map(AgentDecision.class::cast).orElseGet(() -> AgentDecision.rag(
                AgentRoute.KNOWLEDGE_RAG,
                userMessage(),
                "default workflow decision"));
    }

    public String selectedAgent() {
        return value(SELECTED_AGENT).map(String.class::cast).orElse("KnowledgeResearchAgent");
    }

    public String guardMessage() {
        return value(GUARD_MESSAGE).map(String.class::cast).orElse("");
    }

    @SuppressWarnings("unchecked")
    public List<String> trace() {
        return value(TRACE)
                .map(value -> (List<String>) value)
                .orElseGet(List::of);
    }

    public String traceText() {
        List<String> trace = trace();
        return trace.isEmpty() ? "graph not executed" : String.join(" -> ", trace);
    }

    static List<String> appendTrace(AgentWorkflowState state, String node) {
        List<String> trace = new ArrayList<>(state.trace());
        trace.add(node);
        return trace;
    }
}
