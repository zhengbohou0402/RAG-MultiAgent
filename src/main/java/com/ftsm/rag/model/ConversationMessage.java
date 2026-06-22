package com.ftsm.rag.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String role;
    private String content;
    @JsonProperty("created_at")
    private long createdAt;
}
