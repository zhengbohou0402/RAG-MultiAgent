package com.ftsm.rag.agent;

import com.ftsm.rag.model.ConversationMessage;
import com.ftsm.rag.model.SmartCloudUserContext;

import java.util.List;

public record AgentContext(
        String userMessage,
        List<ConversationMessage> history,
        SmartCloudUserContext user
) {
    public AgentContext {
        history = history == null ? List.of() : List.copyOf(history);
        user = user == null ? SmartCloudUserContext.demo() : user;
    }

    public AgentContext(String userMessage, List<ConversationMessage> history) {
        this(userMessage, history, SmartCloudUserContext.demo());
    }
}
