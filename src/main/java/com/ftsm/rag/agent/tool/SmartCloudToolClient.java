package com.ftsm.rag.agent.tool;

import com.ftsm.rag.model.SmartCloudUserContext;

public interface SmartCloudToolClient {

    BillingSnapshot queryBilling(String query);

    default BillingSnapshot queryBilling(String query, SmartCloudUserContext user) {
        return queryBilling(query);
    }

    IcpChecklist buildIcpChecklist(String query);

    MarketingPackage createMarketingPackage(String query);

    default MarketingPackage createMarketingPackage(String query, SmartCloudUserContext user) {
        return createMarketingPackage(query);
    }

    ResearchPlan createResearchPlan(String query);

    record BillingSnapshot(
            String accountId,
            String billingMonth,
            String totalCost,
            String unpaidAmount,
            String topProduct,
            String recommendation
    ) {
    }

    record IcpChecklist(
            String scenario,
            String requiredMaterials,
            String expectedDuration,
            String riskNotice
    ) {
    }

    record MarketingPackage(
            String productName,
            String headline,
            String posterPrompt,
            String landingPageUrl,
            String campaignCopy
    ) {
    }

    record ResearchPlan(
            String topic,
            String researchScope,
            String comparisonDimensions,
            String outputFormat
    ) {
    }
}
