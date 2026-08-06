package com.mindbridge.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for POST /chat/sessions/{sessionId}/messages.
 */
public record SendMessageRequest(
        @NotBlank(message = "Message content is required")
        @Size(min = 1, max = 10000, message = "Message must be between 1 and 10000 characters")
        String content
) {
}
