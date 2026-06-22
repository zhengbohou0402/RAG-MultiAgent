package com.ftsm.rag.agent;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentWorkflowGraphTest {

    @Test
    void routesDirectPathThroughVerifier() {
        AgentWorkflowState state = executeWith(AgentDecision.direct("hello", "casual"));

        assertEquals(AgentRoute.DIRECT, state.decision().route());
        assertEquals("DirectAnswerAgent", state.selectedAgent());
        assertEquals("supervisor -> direct -> verifier", state.traceText());
    }

    @Test
    void routesKnowledgeRagPathThroughVerifier() {
        AgentWorkflowState state = executeWith(AgentDecision.rag(
                AgentRoute.KNOWLEDGE_RAG,
                "SmartCloud ECS facilities",
                "knowledge"));

        assertEquals("KnowledgeResearchAgent", state.selectedAgent());
        assertEquals("supervisor -> knowledge_rag -> verifier", state.traceText());
    }

    @Test
    void routesSmartCloudSpecialistPathsThroughVerifier() {
        assertSpecialistRoute(AgentRoute.PRODUCT_TECH, "Product_Tech_Agent", "product_tech");
        assertSpecialistRoute(AgentRoute.FINANCE_ORDER, "Finance_Order_Agent", "finance_order");
        assertSpecialistRoute(AgentRoute.ICP_SERVICE, "ICP_Service_Agent", "icp_service");
        assertSpecialistRoute(AgentRoute.OPS_MARKETING, "Ops_Marketing_Agent", "ops_marketing");
        assertSpecialistRoute(AgentRoute.DEEP_RESEARCH, "Deep_Research_Agent", "deep_research");
    }

    @Test
    void routesProcedurePathThroughVerifier() {
        AgentWorkflowState state = executeWith(AgentDecision.rag(
                AgentRoute.PROCEDURE_RAG,
                "visa renewal steps",
                "procedure"));

        assertEquals("ProcedureAgent + KnowledgeResearchAgent", state.selectedAgent());
        assertEquals("supervisor -> procedure_rag -> verifier", state.traceText());
    }

    @Test
    void routesWebRefreshPathThroughVerifier() {
        AgentWorkflowState state = executeWith(AgentDecision.rag(
                AgentRoute.WEB_REFRESH,
                "refresh latest SmartCloud website",
                "refresh"));

        assertEquals("WebRefreshAgent fallback", state.selectedAgent());
        assertEquals("supervisor -> web_refresh -> verifier", state.traceText());
        assertTrue(state.guardMessage().contains("RAG stream"));
    }

    private AgentWorkflowState executeWith(AgentDecision decision) {
        SupervisorAgent supervisor = mock(SupervisorAgent.class);
        AgentContext context = new AgentContext("question", List.of());
        when(supervisor.decide(context)).thenReturn(Mono.just(decision));

        AgentWorkflowGraph graph = new AgentWorkflowGraph(supervisor);
        return graph.execute(context);
    }

    private void assertSpecialistRoute(AgentRoute route, String selectedAgent, String nodeName) {
        AgentWorkflowState state = executeWith(AgentDecision.rag(route, "demo query", "specialist"));

        assertEquals(route, state.decision().route());
        assertEquals(selectedAgent, state.selectedAgent());
        assertEquals("supervisor -> " + nodeName + " -> verifier", state.traceText());
    }
}
