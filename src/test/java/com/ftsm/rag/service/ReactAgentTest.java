package com.ftsm.rag.service;

import com.ftsm.rag.model.ConversationMessage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReactAgentTest {

    @Test
    void invokesRagWhenTheModelRequestsTheTool() {
        ModelFactory models = mock(ModelFactory.class);
        RagService rag = mock(RagService.class);
        SystemPromptService prompts = mock(SystemPromptService.class);
        ChatLanguageModel chatModel = mock(ChatLanguageModel.class);
        when(models.getChatModel()).thenReturn(chatModel);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1")
                .name("rag_search")
                .arguments("{\"query\":\"SmartCloud ECS setup requirements\"}")
                .build();
        when(chatModel.generate(anyList(), any(ToolSpecification.class)))
                .thenReturn(Response.from(AiMessage.from(request)));
        when(rag.streamAnswer("SmartCloud ECS setup requirements"))
                .thenReturn(Flux.just("answer"));

        ReactAgent agent = new ReactAgent(models, rag, prompts);

        StepVerifier.create(agent.executeStream("How can I create an ECS instance?", List.of()))
                .expectNext("__THINK__Searching knowledge base...__ENDTHINK__")
                .expectNext("answer")
                .verifyComplete();

        verify(rag).streamAnswer("SmartCloud ECS setup requirements");
    }

    @Test
    void fallbackContextualizesDomainFollowUpsWhenRoutingFails() {
        ModelFactory models = mock(ModelFactory.class);
        RagService rag = mock(RagService.class);
        SystemPromptService prompts = mock(SystemPromptService.class);
        ChatLanguageModel chatModel = mock(ChatLanguageModel.class);
        when(models.getChatModel()).thenReturn(chatModel);
        when(chatModel.generate(anyList(), any(ToolSpecification.class)))
                .thenThrow(new IllegalStateException("router unavailable"));

        String contextualized = "Tell me about the SmartCloud ECS setup guide\nFollow-up question: What about it?";
        when(rag.streamAnswer(contextualized)).thenReturn(Flux.just("answer"));
        ReactAgent agent = new ReactAgent(models, rag, prompts);

        StepVerifier.create(agent.executeStream(
                        "What about it?",
                        List.of(new ConversationMessage(
                                "user", "Tell me about the SmartCloud ECS setup guide", 1L))))
                .expectNext("__THINK__Searching knowledge base...__ENDTHINK__")
                .expectNext("answer")
                .verifyComplete();
    }

    @Test
    void recognizesChineseCasualMessages() {
        org.junit.jupiter.api.Assertions.assertTrue(ReactAgent.isCasualMessage("你好！"));
        org.junit.jupiter.api.Assertions.assertTrue(ReactAgent.isCasualMessage("谢谢"));
    }

    @Test
    void streamsDirectAnswerWhenRouterRejectsTheToolCall() {
        ModelFactory models = mock(ModelFactory.class);
        RagService rag = mock(RagService.class);
        SystemPromptService prompts = mock(SystemPromptService.class);
        ChatLanguageModel chatModel = mock(ChatLanguageModel.class);
        StreamingChatLanguageModel streamingChatModel = mock(StreamingChatLanguageModel.class);
        when(models.getChatModel()).thenReturn(chatModel);
        when(models.getStreamingChatModel()).thenReturn(streamingChatModel);
        when(prompts.getPrompt()).thenReturn("helpful system prompt");
        when(chatModel.generate(anyList(), any(ToolSpecification.class)))
                .thenReturn(Response.from(AiMessage.from("Hi there, happy to chat!")));

        AtomicBoolean streamingInvoked = new AtomicBoolean(false);
        AtomicReference<String> streamedPromptHolder = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            streamingInvoked.set(true);
            List<ChatMessage> promptMessages = invocation.getArgument(0);
            streamedPromptHolder.set(promptMessages.toString());
            StreamingResponseHandler<AiMessage> handler = invocation.getArgument(1);
            handler.onNext("Hello");
            handler.onNext(" there");
            handler.onComplete(Response.from(AiMessage.from("Hello there")));
            return null;
        }).when(streamingChatModel).generate(anyList(), any(StreamingResponseHandler.class));

        ReactAgent agent = new ReactAgent(models, rag, prompts);
        StepVerifier.create(agent.executeStream("Hi", List.of()))
                .expectNext("Hello")
                .expectNext(" there")
                .verifyComplete();
        assertTrue(streamingInvoked.get(), "Streaming model must be invoked for direct answer");
        org.mockito.Mockito.verify(rag, org.mockito.Mockito.never()).streamAnswer(any());
        assertNotNull(streamedPromptHolder.get(),
                "Direct-answer streaming must receive the assembled prompt messages");
    }

    @Test
    void ragToolCallAppendsSourceFooterAtTheEnd() {
        ModelFactory models = mock(ModelFactory.class);
        RagService rag = mock(RagService.class);
        SystemPromptService prompts = mock(SystemPromptService.class);
        ChatLanguageModel chatModel = mock(ChatLanguageModel.class);
        when(models.getChatModel()).thenReturn(chatModel);
        when(prompts.getPrompt()).thenReturn("helpful system prompt");
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1")
                .name("rag_search")
                .arguments("{\"query\":\"SmartCloud GPU promotion\"}")
                .build();
        when(chatModel.generate(anyList(), any(ToolSpecification.class)))
                .thenReturn(Response.from(AiMessage.from(request)));
        when(rag.streamAnswer("SmartCloud GPU promotion"))
                .thenReturn(Flux.just("body chunk\n\nSources:\n- [1] x [Y]"));

        ReactAgent agent = new ReactAgent(models, rag, prompts);
        StepVerifier.create(agent.executeStream("Any GPU promotion?", List.of()))
                .expectNext("__THINK__Searching knowledge base...__ENDTHINK__")
                .expectNext("body chunk\n\nSources:\n- [1] x [Y]")
                .verifyComplete();
        verify(rag).streamAnswer("SmartCloud GPU promotion");
    }
}
