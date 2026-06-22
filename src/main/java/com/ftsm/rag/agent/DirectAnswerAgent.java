package com.ftsm.rag.agent;

import com.ftsm.rag.service.ModelFactory;
import com.ftsm.rag.service.SystemPromptService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Service
public class DirectAnswerAgent {

    private final ModelFactory modelFactory;
    private final SystemPromptService systemPromptService;

    public DirectAnswerAgent(ModelFactory modelFactory, SystemPromptService systemPromptService) {
        this.modelFactory = modelFactory;
        this.systemPromptService = systemPromptService;
    }

    public Flux<String> stream(AgentContext context) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPromptService.getPrompt()));
        ConversationMessageMapper.appendRecentHistory(messages, context.history());
        messages.add(UserMessage.from(context.userMessage()));

        return Flux.<String>create(sink -> {
            try {
                modelFactory.getStreamingChatModel().generate(
                        messages,
                        new StreamingResponseHandler<AiMessage>() {
                            @Override
                            public void onNext(String token) {
                                sink.next(token);
                            }

                            @Override
                            public void onComplete(Response<AiMessage> response) {
                                sink.complete();
                            }

                            @Override
                            public void onError(Throwable error) {
                                sink.error(error);
                            }
                        });
            } catch (Exception error) {
                sink.error(error);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
