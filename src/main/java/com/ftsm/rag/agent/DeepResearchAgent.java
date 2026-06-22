package com.ftsm.rag.agent;

import com.ftsm.rag.agent.tool.SmartCloudToolClient;
import com.ftsm.rag.service.RagService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class DeepResearchAgent {

    private final SmartCloudToolClient toolClient;
    private final RagService ragService;

    public DeepResearchAgent(SmartCloudToolClient toolClient, RagService ragService) {
        this.toolClient = toolClient;
        this.ragService = ragService;
    }

    public Flux<String> streamAnswer(AgentDecision decision) {
        SmartCloudToolClient.ResearchPlan plan = toolClient.createResearchPlan(decision.query());
        Flux<String> header = Flux.just("""
                ## Deep_Research_Agent Plan

                Topic:
                %s

                Research scope:
                %s

                Comparison dimensions:
                %s

                Output format:
                %s

                ### Retrieved evidence

                """.formatted(
                plan.topic(),
                plan.researchScope(),
                plan.comparisonDimensions(),
                plan.outputFormat()
        ));
        return Flux.concat(header, ragService.streamAnswer(decision.query()));
    }
}
