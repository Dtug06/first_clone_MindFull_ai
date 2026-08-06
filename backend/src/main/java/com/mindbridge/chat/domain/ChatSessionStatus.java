package com.mindbridge.chat.domain;

/**
 * Status of a chat session.
 *
 * Lifecycle: ACTIVE → CLOSED (user closes) → ARCHIVED (admin/system archives).
 */
public enum ChatSessionStatus {
    ACTIVE,
    CLOSED,
    ARCHIVED
}
