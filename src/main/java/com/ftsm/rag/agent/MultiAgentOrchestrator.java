package com.ftsm.rag.agent;

import com.ftsm.rag.model.ConversationMessage;
import com.ftsm.rag.model.SmartCloudUserContext;
import com.ftsm.rag.service.SmartCloudMetricsService;
import com.ftsm.rag.service.SmartCloudTraceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Service
public class MultiAgentOrchestrator {

    private final AgentWorkflowGraph workflowGraph;
    private final DirectAnswerAgent directAnswerAgent;
    private final ProductTechAgent productTechAgent;
    private final FinanceOrderAgent financeOrderAgent;
    private final IcpServiceAgent icpServiceAgent;
    private final OpsMarketingAgent opsMarketingAgent;
    private final DeepResearchAgent deepResearchAgent;
    private final KnowledgeResearchAgent knowledgeResearchAgent;
    private final VerifierAgent verifierAgent;
    private final SmartCloudMetricsService metricsService;
    private final SmartCloudTraceService traceService;

    public MultiAgentOrchestrator(AgentWorkflowGraph workflowGraph,
                                  DirectAnswerAgent directAnswerAgent,
                                  ProductTechAgent productTechAgent,
                                  FinanceOrderAgent financeOrderAgent,
                                  IcpServiceAgent icpServiceAgent,
                                  OpsMarketingAgent opsMarketingAgent,
                                  DeepResearchAgent deepResearchAgent,
                                  KnowledgeResearchAgent knowledgeResearchAgent,
                                  VerifierAgent verifierAgent,
                                  SmartCloudMetricsService metricsService,
                                  SmartCloudTraceService traceService) {
        this.workflowGraph = workflowGraph;
        this.directAnswerAgent = directAnswerAgent;
        this.productTechAgent = productTechAgent;
        this.financeOrderAgent = financeOrderAgent;
        this.icpServiceAgent = icpServiceAgent;
        this.opsMarketingAgent = opsMarketingAgent;
        this.deepResearchAgent = deepResearchAgent;
        this.knowledgeResearchAgent = knowledgeResearchAgent;
        this.verifierAgent = verifierAgent;
        this.metricsService = metricsService;
        this.traceService = traceService;
    }

    public Flux<String> executeStream(String userMessage, List<ConversationMessage> history) {
        return executeStream(userMessage, history, SmartCloudUserContext.demo(), "protocol-demo");
    }

    public Flux<String> executeStream(String userMessage, List<ConversationMessage> history,
                                      SmartCloudUserContext user, String conversationId) {
        AgentContext context = new AgentContext(userMessage, history, user);
        long startedAt = System.currentTimeMillis();
        return Mono.fromCallable(() -> workflowGraph.execute(context))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(workflowState -> {
                    AgentDecision decision = workflowState.decision();
                    metricsService.agentRoute(decision.route().name());
                    traceService.record(
                            conversationId,
                            context.user(),
                            decision.route().name(),
                            workflowState.selectedAgent(),
                            decision.reason(),
                            workflowState.trace(),
                            List.of(),
                            System.currentTimeMillis() - startedAt
                    );
                    log.info("Multi-agent route={} reason={} query={}",
                            decision.route(), decision.reason(), decision.query());
                    Flux<String> routed = switch (decision.route()) {
                        case DIRECT -> directAnswerAgent.stream(context);
                        case PRODUCT_TECH -> productTechAgent.streamAnswer(decision);
                        case FINANCE_ORDER -> financeOrderAgent.streamAnswer(decision, context);
                        case ICP_SERVICE -> icpServiceAgent.streamAnswer(decision);
                        case OPS_MARKETING -> opsMarketingAgent.streamAnswer(decision, context);
                        case DEEP_RESEARCH -> deepResearchAgent.streamAnswer(decision);
                        case KNOWLEDGE_RAG, PROCEDURE_RAG -> knowledgeResearchAgent.streamAnswer(decision);
                        case WEB_REFRESH -> Flux.concat(
                                Flux.just("__THINK__Website refresh is not automatic yet; using indexed knowledge first...__ENDTHINK__"),
                                knowledgeResearchAgent.streamAnswer(decision));
                    };
                    return Flux.concat(routeTrace(workflowState), verifierAgent.guard(decision, routed));
                });
    }

    private Flux<String> routeTrace(AgentWorkflowState workflowState) {
        AgentDecision decision = workflowState.decision();
        return Flux.just("__THINK__Graph route: " + workflowState.traceText()
                + "; selected=" + workflowState.selectedAgent()
                + "; reason=" + decision.reason() + "__ENDTHINK__");
    }
}
