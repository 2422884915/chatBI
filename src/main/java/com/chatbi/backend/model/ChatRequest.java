package com.chatbi.backend.model;

import lombok.Data;

@Data
public class ChatRequest {
    private String query;
    private String databaseName; // Optional, or can be inferred/fixed
}
