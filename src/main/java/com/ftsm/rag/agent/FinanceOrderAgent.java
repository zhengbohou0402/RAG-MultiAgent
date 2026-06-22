package com.ftsm.rag.agent;

import com.ftsm.rag.agent.tool.SmartCloudToolClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class FinanceOrderAgent {

    private final SmartCloudToolClient toolClient;

    public FinanceOrderAgent(SmartCloudToolClient toolClient) {
        this.toolClient = toolClient;
    }

    public Flux<String> streamAnswer(AgentDecision decision) {
        return streamAnswer(decision, null);
    }

    public Flux<String> streamAnswer(AgentDecision decision, AgentContext context) {
        SmartCloudToolClient.BillingSnapshot bill = toolClient.queryBilling(
                decision.query(),
                context == null ? null : context.user()
        );
        return Flux.just("""
                ## Finance_Order_Agent Result

                I checked the demo billing workspace for `%s`.

                - Billing month: %s
                - Current total cost: %s
                - Unpaid amount: %s
                - Main cost driver: %s
                - Optimization suggestion: %s

                This answer is generated through the SmartCloud tool layer. In production, this tool can be replaced by an MCP billing server or an internal finance API.
                """.formatted(
                bill.accountId(),
                bill.billingMonth(),
                bill.totalCost(),
                bill.unpaidAmount(),
                bill.topProduct(),
                bill.recommendation()
        ));
    }
}
