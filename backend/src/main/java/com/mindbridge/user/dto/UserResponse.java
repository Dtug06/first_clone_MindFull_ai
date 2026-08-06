package com.mindbridge.user.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public user profile returned by API endpoints.
 * Intentionally excludes passwordHash, salt, and any internal fields.
 */
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        UserStatus status,
        Instant createdAt
) {

    public enum UserRole {
        USER, EXPERT, ADMIN
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}
