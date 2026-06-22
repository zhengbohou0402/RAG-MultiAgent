package com.ftsm.rag.agent;

import com.ftsm.rag.model.ConversationMessage;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentWorkflowGraph {

    private static final String SUPERVISOR = "supervisor";
    private static final String DIRECT = "direct";
    private static final String PRODUCT_TECH = "product_tech";
    private static final String FINANCE_ORDER = "finance_order";
    private static final String ICP_SERVICE = "icp_service";
    private static final String OPS_MARKETING = "ops_marketing";
    private static final String DEEP_RESEARCH = "deep_research";
    private static final String KNOWLEDGE_RAG = "knowledge_rag";
    private static final String PROCEDURE_RAG = "procedure_rag";
    private static final String WEB_REFRESH = "web_refresh";
    private static final String VERIFIER = "verifier";

    private final SupervisorAgent supervisorAgent;
    private final CompiledGraph<AgentWorkflowState> graph;

    public AgentWorkflowGraph(SupervisorAgent supervisorAgent) {
        this.supervisorAgent = supervisorAgent;
        this.graph = compileGraph();
    }

    public AgentWorkflowState execute(AgentContext context) {
        Map<String, Object> input = new HashMap<>();
        input.put(AgentWorkflowState.USER_MESSAGE, context.userMessage());
        input.put(AgentWorkflowState.HISTORY, context.history());
        input.put(AgentWorkflowState.TRACE, List.of());
        return graph.invoke(input)
                .orElseThrow(() -> new IllegalStateException("Agent workflow graph returned no final state"));
    }

    private CompiledGraph<AgentWorkflowState> compileGraph() {
        try {
            StateGraph<AgentWorkflowState> stateGraph = new StateGraph<>(AgentWorkflowState::new);
            stateGraph.addNode(SUPERVISOR, AsyncNodeAction.node_async(this::supervisorNode));
            stateGraph.addNode(DIRECT, AsyncNodeAction.node_async(state -> selectNode(state, "DirectAnswerAgent", DIRECT)));
            stateGraph.addNode(PRODUCT_TECH, AsyncNodeAction.node_async(
                    state -> selectNode(state, "Product_Tech_Agent", PRODUCT_TECH)));
            stateGraph.addNode(FINANCE_ORDER, AsyncNodeAction.node_async(
                    state -> selectNode(state, "Finance_Order_Agent", FINANCE_ORDER)));
            stateGraph.addNode(ICP_SERVICE, AsyncNodeAction.node_async(
                    state -> selectNode(state, "ICP_Service_Agent", ICP_SERVICE)));
            stateGraph.addNode(OPS_MARKETING, AsyncNodeAction.node_async(
                    state -> selectNode(state, "Ops_Marketing_Agent", OPS_MARKETING)));
            stateGraph.addNode(DEEP_RESEARCH, AsyncNodeAction.node_async(
                    state -> selectNode(state, "Deep_Research_Agent", DEEP_RESEARCH)));
            stateGraph.addNode(KNOWLEDGE_RAG, AsyncNodeAction.node_async(
                    state -> selectNode(state, "KnowledgeResearchAgent", KNOWLEDGE_RAG)));
            stateGraph.addNode(PROCEDURE_RAG, AsyncNodeAction.node_async(
                    state -> selectNode(state, "ProcedureAgent + KnowledgeResearchAgent", PROCEDURE_RAG)));
            stateGraph.addNode(WEB_REFRESH, AsyncNodeAction.node_async(
                    state -> selectNode(state, "WebRefreshAgent fallback", WEB_REFRESH)));
            stateGraph.addNode(VERIFIER, AsyncNodeAction.node_async(this::verifierNode));

            stateGraph.addEdge(GraphDefinition.START, SUPERVISOR);
            stateGraph.addConditionalEdges(SUPERVISOR, AsyncEdgeAction.edge_async(this::routeFromDecision), Map.of(
                    DIRECT, DIRECT,
                    PRODUCT_TECH, PRODUCT_TECH,
                    FINANCE_ORDER, FINANCE_ORDER,
                    ICP_SERVICE, ICP_SERVICE,
                    OPS_MARKETING, OPS_MARKETING,
                    DEEP_RESEARCH, DEEP_RESEARCH,
                    KNOWLEDGE_RAG, KNOWLEDGE_RAG,
                    PROCEDURE_RAG, PROCEDURE_RAG,
                    WEB_REFRESH, WEB_REFRESH
            ));
            stateGraph.addEdge(DIRECT, VERIFIER);
            stateGraph.addEdge(PRODUCT_TECH, VERIFIER);
            stateGraph.addEdge(FINANCE_ORDER, VERIFIER);
            stateGraph.addEdge(ICP_SERVICE, VERIFIER);
            stateGraph.addEdge(OPS_MARKETING, VERIFIER);
            stateGraph.addEdge(DEEP_RESEARCH, VERIFIER);
            stateGraph.addEdge(KNOWLEDGE_RAG, VERIFIER);
            stateGraph.addEdge(PROCEDURE_RAG, VERIFIER);
            stateGraph.addEdge(WEB_REFRESH, VERIFIER);
            stateGraph.addEdge(VERIFIER, GraphDefinition.END);
            return stateGraph.compile();
        } catch (GraphStateException error) {
            throw new IllegalStateException("Could not compile multi-agent workflow graph", error);
        }
    }

    private Map<String, Object> supervisorNode(AgentWorkflowState state) {
        AgentContext context = new AgentContext(state.userMessage(), state.history());
        AgentDecision decision = supervisorAgent.decide(context).block();
        if (decision == null) {
            decision = AgentDecision.rag(AgentRoute.KNOWLEDGE_RAG, state.userMessage(), "empty supervisor decision");
        }
        return Map.of(
                AgentWorkflowState.DECISION, decision,
                AgentWorkflowState.TRACE, AgentWorkflowState.appendTrace(state, SUPERVISOR)
        );
    }

    private String routeFromDecision(AgentWorkflowState state) {
        return switch (state.decision().route()) {
            case DIRECT -> DIRECT;
            case PRODUCT_TECH -> PRODUCT_TECH;
            case FINANCE_ORDER -> FINANCE_ORDER;
            case ICP_SERVICE -> ICP_SERVICE;
            case OPS_MARKETING -> OPS_MARKETING;
            case DEEP_RESEARCH -> DEEP_RESEARCH;
            case KNOWLEDGE_RAG -> KNOWLEDGE_RAG;
            case PROCEDURE_RAG -> PROCEDURE_RAG;
            case WEB_REFRESH -> WEB_REFRESH;
        };
    }

    private Map<String, Object> selectNode(AgentWorkflowState state, String selectedAgent, String nodeName) {
        return Map.of(
                AgentWorkflowState.SELECTED_AGENT, selectedAgent,
                AgentWorkflowState.TRACE, AgentWorkflowState.appendTrace(state, nodeName)
        );
    }

    private Map<String, Object> verifierNode(AgentWorkflowState state) {
        String guardMessage = state.decision().route() == AgentRoute.DIRECT
                ? ""
                : "RAG stream will be guarded for empty output.";
        return Map.of(
                AgentWorkflowState.GUARD_MESSAGE, guardMessage,
                AgentWorkflowState.TRACE, AgentWorkflowState.appendTrace(state, VERIFIER)
        );
    }
}
