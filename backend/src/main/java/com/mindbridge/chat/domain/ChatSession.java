package com.mindbridge.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the {@code chat_sessions} table.
 *
 * Invariants:
 * - A session always belongs to exactly one user (user_id FK).
 * - Status transitions: ACTIVE → CLOSED → ARCHIVED (enforced at service level).
 * - closed_at is set only when status becomes CLOSED.
 * - updated_at is refreshed on every state change.
 */
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatSessionStatus status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChatSession() {
    }

    private ChatSession(UUID userId, String title, ChatSessionStatus status,
                       Instant startedAt, Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.title = title;
        this.status = status;
        this.startedAt = startedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.startedAt == null) {
            this.startedAt = now;
        }
        if (this.status == null) {
            this.status = ChatSessionStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Factory: creates a new ACTIVE session for the given user.
     * No setter — immutable entity after construction.
     */
    public static ChatSession create(UUID userId, String title) {
        Instant now = Instant.now();
        return new ChatSession(userId, title, ChatSessionStatus.ACTIVE, now, now, now);
    }

    /**
     * Closes this session. Sets status to CLOSED and records closed_at.
     * Idempotent: closing an already-closed session is a no-op at DB level
     * (updated_at still refreshed by @PreUpdate).
     */
    public void close() {
        if (this.status != ChatSessionStatus.CLOSED) {
            this.status = ChatSessionStatus.CLOSED;
            this.closedAt = Instant.now();
        }
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public ChatSessionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
