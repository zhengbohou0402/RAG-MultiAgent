package com.ftsm.rag.agent;

import com.ftsm.rag.agent.tool.SmartCloudToolClient;
import com.ftsm.rag.service.A2AClientService;
import com.ftsm.rag.service.SmartCloudProductCatalogService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class OpsMarketingAgent {

    private final SmartCloudToolClient toolClient;
    private final A2AClientService a2aClientService;

    public OpsMarketingAgent(SmartCloudToolClient toolClient, A2AClientService a2aClientService) {
        this.toolClient = toolClient;
        this.a2aClientService = a2aClientService;
    }

    public Flux<String> streamAnswer(AgentDecision decision) {
        return streamAnswer(decision, null);
    }

    public Flux<String> streamAnswer(AgentDecision decision, AgentContext context) {
        SmartCloudToolClient.MarketingPackage pkg = toolClient.createMarketingPackage(
                decision.query(),
                context == null ? null : context.user()
        );
        SmartCloudProductCatalogService.ProductProfile product = a2aClientService.requestProductProfile(decision.query());
        return Flux.just("""
                ## Ops_Marketing_Agent Package

                Product: %s

                A2A product context:
                - Category: %s
                - Best fit: %s
                - Current promotion: %s

                Headline:
                %s

                Campaign copy:
                %s

                Poster generation prompt:
                `%s`

                Share landing page:
                %s

                This demo uses a deterministic local tool. The same interface can be backed by an image-generation MCP server, CMS API, or campaign platform.
                """.formatted(
                product.name(),
                product.category(),
                product.useCase(),
                product.promotion(),
                pkg.headline(),
                pkg.campaignCopy(),
                pkg.posterPrompt(),
                pkg.landingPageUrl()
        ));
    }
}
