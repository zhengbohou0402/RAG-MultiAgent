package com.ftsm.rag.agent;

import com.ftsm.rag.service.SmartCloudMetricsService;
import com.ftsm.rag.service.SmartCloudTraceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiAgentOrchestratorTest {

    @Test
    void directRouteStreamsDirectAnswerAndDoesNotCallRag() {
        AgentWorkflowGraph graph = mock(AgentWorkflowGraph.class);
        DirectAnswerAgent direct = mock(DirectAnswerAgent.class);
        ProductTechAgent product = mock(ProductTechAgent.class);
        FinanceOrderAgent finance = mock(FinanceOrderAgent.class);
        IcpServiceAgent icp = mock(IcpServiceAgent.class);
        OpsMarketingAgent marketing = mock(OpsMarketingAgent.class);
        DeepResearchAgent research = mock(DeepResearchAgent.class);
        KnowledgeResearchAgent rag = mock(KnowledgeResearchAgent.class);
        VerifierAgent verifier = new VerifierAgent();
        AgentDecision decision = AgentDecision.direct("hello", "casual");
        when(graph.execute(any())).thenReturn(state(decision, "DirectAnswerAgent", "supervisor", "direct", "verifier"));
        when(direct.stream(any())).thenReturn(Flux.just("Hi"));

        MultiAgentOrchestrator orchestrator = new MultiAgentOrchestrator(
                graph, direct, product, finance, icp, marketing, research, rag, verifier,
                mock(SmartCloudMetricsService.class), mock(SmartCloudTraceService.class));

        StepVerifier.create(orchestrator.executeStream("hello", List.of()))
                .expectNext("__THINK__Graph route: supervisor -> direct -> verifier; selected=DirectAnswerAgent; reason=casual__ENDTHINK__")
                .expectNext("Hi")
                .verifyComplete();

        verify(rag, never()).streamAnswer(any());
    }

    @Test
    void ragRouteGuardsEmptyStream() {
        AgentWorkflowGraph graph = mock(AgentWorkflowGraph.class);
        DirectAnswerAgent direct = mock(DirectAnswerAgent.class);
        ProductTechAgent product = mock(ProductTechAgent.class);
        FinanceOrderAgent finance = mock(FinanceOrderAgent.class);
        IcpServiceAgent icp = mock(IcpServiceAgent.class);
        OpsMarketingAgent marketing = mock(OpsMarketingAgent.class);
        DeepResearchAgent research = mock(DeepResearchAgent.class);
        KnowledgeResearchAgent rag = mock(KnowledgeResearchAgent.class);
        VerifierAgent verifier = new VerifierAgent();
        AgentDecision decision = AgentDecision.rag(AgentRoute.KNOWLEDGE_RAG, "SmartCloud ECS guide", "knowledge");
        when(graph.execute(any())).thenReturn(state(decision, "KnowledgeResearchAgent", "supervisor", "knowledge_rag", "verifier"));
        when(rag.streamAnswer(decision)).thenReturn(Flux.empty());

        MultiAgentOrchestrator orchestrator = new MultiAgentOrchestrator(
                graph, direct, product, finance, icp, marketing, research, rag, verifier,
                mock(SmartCloudMetricsService.class), mock(SmartCloudTraceService.class));

        StepVerifier.create(orchestrator.executeStream("calendar?", List.of()))
                .expectNext("__THINK__Graph route: supervisor -> knowledge_rag -> verifier; selected=KnowledgeResearchAgent; reason=knowledge__ENDTHINK__")
                .expectNext("SmartCloud does not have enough confirmed knowledge or tool data to answer this request safely.")
                .verifyComplete();
    }

    @Test
    void financeRouteUsesFinanceAgent() {
        AgentWorkflowGraph graph = mock(AgentWorkflowGraph.class);
        DirectAnswerAgent direct = mock(DirectAnswerAgent.class);
        ProductTechAgent product = mock(ProductTechAgent.class);
        FinanceOrderAgent finance = mock(FinanceOrderAgent.class);
        IcpServiceAgent icp = mock(IcpServiceAgent.class);
        OpsMarketingAgent marketing = mock(OpsMarketingAgent.class);
        DeepResearchAgent research = mock(DeepResearchAgent.class);
        KnowledgeResearchAgent rag = mock(KnowledgeResearchAgent.class);
        VerifierAgent verifier = new VerifierAgent();
        AgentDecision decision = AgentDecision.rag(AgentRoute.FINANCE_ORDER, "show bill", "billing");
        when(graph.execute(any())).thenReturn(state(decision, "Finance_Order_Agent", "supervisor", "finance_order", "verifier"));
        when(finance.streamAnswer(eq(decision), any())).thenReturn(Flux.just("bill"));

        MultiAgentOrchestrator orchestrator = new MultiAgentOrchestrator(
                graph, direct, product, finance, icp, marketing, research, rag, verifier,
                mock(SmartCloudMetricsService.class), mock(SmartCloudTraceService.class));

        StepVerifier.create(orchestrator.executeStream("bill", List.of()))
                .expectNext("__THINK__Graph route: supervisor -> finance_order -> verifier; selected=Finance_Order_Agent; reason=billing__ENDTHINK__")
                .expectNext("bill")
                .verifyComplete();

        verify(rag, never()).streamAnswer(any());
    }

    @Test
    void webRefreshRouteEmitsFallbackNoticeBeforeRagAnswer() {
        AgentWorkflowGraph graph = mock(AgentWorkflowGraph.class);
        DirectAnswerAgent direct = mock(DirectAnswerAgent.class);
        ProductTechAgent product = mock(ProductTechAgent.class);
        FinanceOrderAgent finance = mock(FinanceOrderAgent.class);
        IcpServiceAgent icp = mock(IcpServiceAgent.class);
        OpsMarketingAgent marketing = mock(OpsMarketingAgent.class);
        DeepResearchAgent research = mock(DeepResearchAgent.class);
        KnowledgeResearchAgent rag = mock(KnowledgeResearchAgent.class);
        VerifierAgent verifier = new VerifierAgent();
        AgentDecision decision = AgentDecision.rag(AgentRoute.WEB_REFRESH, "refresh website", "refresh");
        when(graph.execute(any())).thenReturn(state(decision, "WebRefreshAgent fallback", "supervisor", "web_refresh", "verifier"));
        when(rag.streamAnswer(decision)).thenReturn(Flux.just("indexed answer"));

        MultiAgentOrchestrator orchestrator = new MultiAgentOrchestrator(
                graph, direct, product, finance, icp, marketing, research, rag, verifier,
                mock(SmartCloudMetricsService.class), mock(SmartCloudTraceService.class));

        StepVerifier.create(orchestrator.executeStream("refresh", List.of()))
                .expectNext("__THINK__Graph route: supervisor -> web_refresh -> verifier; selected=WebRefreshAgent fallback; reason=refresh__ENDTHINK__")
                .expectNext("__THINK__Website refresh is not automatic yet; using indexed knowledge first...__ENDTHINK__")
                .expectNext("indexed answer")
                .verifyComplete();
    }

    private AgentWorkflowState state(AgentDecision decision, String selectedAgent, String... trace) {
        return new AgentWorkflowState(Map.of(
                AgentWorkflowState.USER_MESSAGE, "question",
                AgentWorkflowState.HISTORY, List.of(),
                AgentWorkflowState.DECISION, decision,
                AgentWorkflowState.SELECTED_AGENT, selectedAgent,
                AgentWorkflowState.TRACE, List.of(trace)
        ));
    }
}
