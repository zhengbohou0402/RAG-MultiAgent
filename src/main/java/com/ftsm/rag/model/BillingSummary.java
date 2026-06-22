package com.ftsm.rag.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BillingSummary(
        @JsonProperty("account_id") String accountId,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("billing_month") String billingMonth,
        @JsonProperty("total_cost") String totalCost,
        @JsonProperty("unpaid_amount") String unpaidAmount,
        @JsonProperty("top_product") String topProduct,
        String recommendation
) {
}
