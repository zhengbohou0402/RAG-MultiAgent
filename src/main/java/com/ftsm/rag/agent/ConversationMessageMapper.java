package com.ftsm.rag.agent;

import com.ftsm.rag.model.ConversationMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.List;

final class ConversationMessageMapper {

    private ConversationMessageMapper() {
    }

    static void appendRecentHistory(List<ChatMessage> messages, List<ConversationMessage> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            ConversationMessage message = history.get(i);
            if ("user".equalsIgnoreCase(message.getRole())) {
                messages.add(UserMessage.from(message.getContent()));
            } else if ("assistant".equalsIgnoreCase(message.getRole())) {
                messages.add(AiMessage.from(message.getContent()));
            }
        }
    }
}
