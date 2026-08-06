package com.mindbridge.chat.domain;

/**
 * Role of a conversation message sender.
 *
 * Matches MessageRole schema in 03_API_CONTRACT.yaml.
 */
public enum MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
