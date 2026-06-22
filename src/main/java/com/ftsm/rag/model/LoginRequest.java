package com.ftsm.rag.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;

    @JsonProperty("tenant_id")
    private String tenantId;
}
