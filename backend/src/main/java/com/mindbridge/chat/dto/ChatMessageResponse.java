package com.mindbridge.chat.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Individual message returned by send and list endpoints.
 */
public record ChatMessageResponse(
        UUID id,
        UUID sessionId,
        MessageRole role,
        String content,
        Instant createdAt,
        AnalysisStatus analysisStatus
) {

    public enum MessageRole {
        USER, ASSISTANT, SYSTEM
    }

    public enum AnalysisStatus {
        NOT_REQUESTED, PENDING, SUCCEEDED, FAILED
    }
}
