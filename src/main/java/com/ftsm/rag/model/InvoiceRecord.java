package com.ftsm.rag.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InvoiceRecord(
        String id,
        @JsonProperty("invoice_no") String invoiceNo,
        String amount,
        String status,
        @JsonProperty("issued_at") String issuedAt
) {
}
