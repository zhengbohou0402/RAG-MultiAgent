package com.ftsm.rag.agent;

import com.ftsm.rag.service.RagService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class KnowledgeResearchAgent {

    private final RagService ragService;

    public KnowledgeResearchAgent(RagService ragService) {
        this.ragService = ragService;
    }

    public Flux<String> streamAnswer(AgentDecision decision) {
        return ragService.streamAnswer(decision.query());
    }
}
