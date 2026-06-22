package com.ftsm.rag.agent.tool;

import com.ftsm.rag.model.BillingSummary;
import com.ftsm.rag.model.SmartCloudUserContext;
import com.ftsm.rag.service.SmartCloudBusinessDataService;
import org.springframework.stereotype.Service;

@Service
public class LocalSmartCloudToolClient implements SmartCloudToolClient {

    private final SmartCloudBusinessDataService businessDataService;

    public LocalSmartCloudToolClient(SmartCloudBusinessDataService businessDataService) {
        this.businessDataService = businessDataService;
    }

    @Override
    public BillingSnapshot queryBilling(String query) {
        return queryBilling(query, SmartCloudUserContext.demo());
    }

    @Override
    public BillingSnapshot queryBilling(String query, SmartCloudUserContext user) {
        SmartCloudUserContext effectiveUser = user == null ? SmartCloudUserContext.demo() : user;
        BillingSummary summary = businessDataService.billingSummary(effectiveUser, query);
        return new BillingSnapshot(
                summary.accountId(),
                summary.billingMonth(),
                summary.totalCost(),
                summary.unpaidAmount(),
                summary.topProduct(),
                summary.recommendation()
        );
    }

    @Override
    public IcpChecklist buildIcpChecklist(String query) {
        String scenario = containsAny(query, "change", "transfer", "modify", "filing update")
                ? "ICP change filing"
                : "new ICP filing";
        return new IcpChecklist(
                scenario,
                "Domain certificate, company license, responsible-person ID, server access proof, website commitment letter.",
                "Initial review: 1-3 working days; authority review: usually 7-20 working days.",
                "The domain owner, company name, and server access provider must stay consistent before submission."
        );
    }

    @Override
    public MarketingPackage createMarketingPackage(String query) {
        return createMarketingPackage(query, SmartCloudUserContext.demo());
    }

    @Override
    public MarketingPackage createMarketingPackage(String query, SmartCloudUserContext user) {
        String product = containsAny(query, "gpu", "GPU", "llm", "LLM", "ai", "AI")
                ? "SmartCloud GPU AI Computing"
                : "SmartCloud ECS Standard";
        return new MarketingPackage(
                product,
                "Deploy faster, scale safer, pay only for what your business needs.",
                "Clean enterprise cloud poster, product dashboard, server racks, AI workload cards.",
                "https://smartcloud.local/campaigns/demo-share",
                "Launch-ready cloud resources for growing teams: elastic compute, secure networking, and observability in one managed platform."
        );
    }

    @Override
    public ResearchPlan createResearchPlan(String query) {
        return new ResearchPlan(
                query == null || query.isBlank() ? "Cloud-native AI application architecture" : query,
                "Business goal, workload profile, model serving strategy, retrieval architecture, cost and risk.",
                "Latency, reliability, data security, token cost, vector retrieval quality, integration complexity.",
                "Executive summary, option matrix, recommended architecture, migration plan, and risk checklist."
        );
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
