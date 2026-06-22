package com.ftsm.rag.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MarketingRequest {
    @JsonProperty("product_name")
    private String productName;
    private String scenario;
    private String audience;
}
