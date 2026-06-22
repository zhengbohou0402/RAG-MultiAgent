package com.ftsm.rag.agent;

import com.ftsm.rag.model.ConversationMessage;
import com.ftsm.rag.service.ModelFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupervisorAgentTest {

    @Test
    void parsesJsonDecisionFromModel() {
        SupervisorAgent supervisor = supervisorReturning("""
                {"route":"DIRECT","query":"hello","reason":"casual"}
                """);

        StepVerifier.create(supervisor.decide(new AgentContext("hello", List.of())))
                .assertNext(decision -> {
                    assertEquals(AgentRoute.DIRECT, decision.route());
                    assertEquals("hello", decision.query());
                    assertEquals("casual", decision.reason());
                })
                .verifyComplete();
    }

    @Test
    void fallsBackToDirectForChineseCasualMessageWhenRouterFails() {
        SupervisorAgent supervisor = supervisorFailing();

        StepVerifier.create(supervisor.decide(new AgentContext("你好！", List.of())))
                .assertNext(decision -> assertEquals(AgentRoute.DIRECT, decision.route()))
                .verifyComplete();
    }

    @Test
    void fallsBackToSmartCloudSpecialistRoutes() {
        SupervisorAgent supervisor = supervisorFailing();

        StepVerifier.create(supervisor.decide(new AgentContext("帮我查一下这个月的账单和发票", List.of())))
                .assertNext(decision -> assertEquals(AgentRoute.FINANCE_ORDER, decision.route()))
                .verifyComplete();
        StepVerifier.create(supervisor.decide(new AgentContext("我的域名要做 ICP 备案需要哪些材料", List.of())))
                .assertNext(decision -> assertEquals(AgentRoute.ICP_SERVICE, decision.route()))
                .verifyComplete();
        StepVerifier.create(supervisor.decide(new AgentContext("帮我给 GPU 云服务器生成推广海报", List.of())))
                .assertNext(decision -> assertEquals(AgentRoute.OPS_MARKETING, decision.route()))
                .verifyComplete();
        StepVerifier.create(supervisor.decide(new AgentContext("做一份 RAG 和 Agent 技术选型报告", List.of())))
                .assertNext(decision -> assertEquals(AgentRoute.DEEP_RESEARCH, decision.route()))
                .verifyComplete();
        StepVerifier.create(supervisor.decide(new AgentContext("如何创建 ECS 云服务器实例", List.of())))
                .assertNext(decision -> assertEquals(AgentRoute.PRODUCT_TECH, decision.route()))
                .verifyComplete();
    }

    @Test
    void fallsBackToProcedureRouteForLegacyProcedureQuestions() {
        SupervisorAgent supervisor = supervisorFailing();

        StepVerifier.create(supervisor.decide(new AgentContext("How do I renew my visa?", List.of())))
                .assertNext(decision -> assertEquals(AgentRoute.PROCEDURE_RAG, decision.route()))
                .verifyComplete();
    }

    @Test
    void fallbackContextualizesFollowUpQuestions() {
        SupervisorAgent supervisor = supervisorFailing();
        List<ConversationMessage> history = List.of(
                new ConversationMessage("user", "Tell me about SmartCloud ICP filing", 1L));

        StepVerifier.create(supervisor.decide(new AgentContext("What about the steps?", history)))
                .assertNext(decision -> {
                    assertEquals(AgentRoute.ICP_SERVICE, decision.route());
                    assertTrue(decision.query().contains("Tell me about SmartCloud ICP filing"));
                    assertTrue(decision.query().contains("Follow-up question: What about the steps?"));
                })
                .verifyComplete();
    }

    private SupervisorAgent supervisorReturning(String text) {
        ModelFactory modelFactory = mock(ModelFactory.class);
        ChatLanguageModel chatModel = mock(ChatLanguageModel.class);
        when(modelFactory.getChatModel()).thenReturn(chatModel);
        when(chatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(text)));
        return new SupervisorAgent(modelFactory);
    }

    private SupervisorAgent supervisorFailing() {
        ModelFactory modelFactory = mock(ModelFactory.class);
        when(modelFactory.getChatModel()).thenThrow(new IllegalStateException("router unavailable"));
        return new SupervisorAgent(modelFactory);
    }
}
