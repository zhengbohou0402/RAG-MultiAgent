package com.ftsm.rag.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarketingAsset(
        String id,
        @JsonProperty("product_name") String productName,
        String headline,
        @JsonProperty("landing_page_url") String landingPageUrl,
        @JsonProperty("poster_url") String posterUrl,
        @JsonProperty("campaign_copy") String campaignCopy
) {
}
