package com.mindbridge.chat.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Result of accepting one user message and attempting one assistant reply.
 *
 * <p>The top-level message fields intentionally mirror {@link ChatMessageResponse}
 * for backward compatibility with the original G2 endpoint. The generated
 * assistant message is nested and may be absent when consent, Safety, an
 * approved template, or the upstream provider prevents generation.
 */
public record ChatTurnResponse(
        UUID id,
        UUID sessionId,
        ChatMessageResponse.MessageRole role,
        String content,
        Instant createdAt,
        ChatMessageResponse.AnalysisStatus analysisStatus,
        ChatMessageResponse assistantMessage,
        ReplyStatus replyStatus
) {

    public enum ReplyStatus {
        SUCCEEDED,
        CONSENT_REQUIRED,
        SAFETY_UNAVAILABLE,
        SAFETY_TEMPLATE_MISSING,
        PROVIDER_UNAVAILABLE
    }

    public static ChatTurnResponse of(
            ChatMessageResponse userMessage,
            ChatMessageResponse assistantMessage,
            ReplyStatus replyStatus) {
        return new ChatTurnResponse(
                userMessage.id(),
                userMessage.sessionId(),
                userMessage.role(),
                userMessage.content(),
                userMessage.createdAt(),
                userMessage.analysisStatus(),
                assistantMessage,
                replyStatus);
    }
}
