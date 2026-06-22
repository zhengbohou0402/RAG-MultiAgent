package com.ftsm.rag.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        String token,
        SmartCloudUserContext user,
        @JsonProperty("expires_at") long expiresAt
) {
}
