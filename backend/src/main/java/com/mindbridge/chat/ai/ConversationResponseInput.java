package com.mindbridge.chat.ai;

import java.util.List;
import java.util.UUID;

/** Redacted conversation context sent to a response provider. */
public record ConversationResponseInput(
        UUID userId,
        UUID sessionId,
        List<HistoryMessage> messages
) {
    public ConversationResponseInput {
        if (userId == null || sessionId == null) {
            throw new IllegalArgumentException("userId and sessionId must not be null");
        }
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        messages = List.copyOf(messages);
    }

    public record HistoryMessage(String role, String content) {
        public HistoryMessage {
            if (!("user".equals(role) || "assistant".equals(role))) {
                throw new IllegalArgumentException("history role must be user or assistant");
            }
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("history content must not be blank");
            }
        }
    }
}
