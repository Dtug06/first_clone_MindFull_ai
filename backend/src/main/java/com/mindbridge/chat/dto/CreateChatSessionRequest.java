package com.mindbridge.chat.dto;

/**
 * Request payload for POST /chat/sessions (optional body).
 */
public record CreateChatSessionRequest(String title) {
}
