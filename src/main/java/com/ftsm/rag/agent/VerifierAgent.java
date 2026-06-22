package com.ftsm.rag.agent;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class VerifierAgent {

    public Flux<String> guard(AgentDecision decision, Flux<String> answerStream) {
        if (decision.route() == AgentRoute.DIRECT) {
            return answerStream;
        }
        return answerStream.switchIfEmpty(Flux.just(
                "SmartCloud does not have enough confirmed knowledge or tool data to answer this request safely."));
    }
}
