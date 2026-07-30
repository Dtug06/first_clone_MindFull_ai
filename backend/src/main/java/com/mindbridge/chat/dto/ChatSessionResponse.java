package com.mindbridge.chat.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Chat session summary returned by list and get-by-id endpoints.
 */
public record ChatSessionResponse(
        UUID id,
        String title,
        ChatSessionStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public enum ChatSessionStatus {
        ACTIVE, CLOSED, ARCHIVED
    }
}
