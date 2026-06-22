package com.ftsm.rag.agent;

import com.ftsm.rag.agent.tool.SmartCloudToolClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class IcpServiceAgent {

    private final SmartCloudToolClient toolClient;

    public IcpServiceAgent(SmartCloudToolClient toolClient) {
        this.toolClient = toolClient;
    }

    public Flux<String> streamAnswer(AgentDecision decision) {
        SmartCloudToolClient.IcpChecklist checklist = toolClient.buildIcpChecklist(decision.query());
        return Flux.just("""
                ## ICP_Service_Agent Checklist

                Scenario: %s

                Required materials:
                %s

                Expected duration:
                %s

                Risk notice:
                %s

                Next action: prepare the material package first, then submit the filing request from the cloud console. If the company name, domain owner, or access provider is inconsistent, fix that before submission.
                """.formatted(
                checklist.scenario(),
                checklist.requiredMaterials(),
                checklist.expectedDuration(),
                checklist.riskNotice()
        ));
    }
}
