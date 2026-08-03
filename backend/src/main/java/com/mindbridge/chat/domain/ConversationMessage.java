package com.mindbridge.chat.domain;

import com.mindbridge.chat.domain.MessageRole;
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
 * JPA entity for the {@code conversation_messages} table.
 *
 * Invariants:
 * - A message always belongs to exactly one session (session_id FK).
 * - A message always belongs to exactly one user (user_id FK).
 * - role determines the sender type.
 * - content stores the message text as it was stored at save time.
 *   For user-sent messages, this is the output of MessagePreprocessor.process():
 *   validated, redacted, and safe for AI/display. Raw input is not stored separately.
 * - redacted flag indicates whether the content has been through the redaction step.
 *   Both raw and redacted are stored in the same column; the flag marks that
 *   redaction was applied. Expert review reads from this column — not from logs.
 * - No emotion, risk or AI conclusion columns — scope boundary of G2-T02.
 * - No setters — entity is immutable after construction.
 */
@Entity
@Table(name = "conversation_messages")
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Boolean redacted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConversationMessage() {
    }

    private ConversationMessage(UUID sessionId, UUID userId, MessageRole role,
                              String content, Boolean redacted,
                              Instant createdAt, Instant updatedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.redacted = redacted;
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
        if (this.redacted == null) {
            this.redacted = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Factory: creates a new USER message with preprocessed (redacted) content.
     * Used after MessagePreprocessor.process() has been applied.
     *
     * @param sessionId       owning session
     * @param userId          owning user
     * @param processedContent the content after validation and redaction — never raw
     * @param redacted        true when content has been redacted
     */
    public static ConversationMessage createUserMessage(
            UUID sessionId, UUID userId, String processedContent, boolean redacted) {
        Instant now = Instant.now();
        return new ConversationMessage(sessionId, userId, MessageRole.USER,
                                      processedContent, redacted, now, now);
    }

    /**
     * @deprecated Use {@link #createUserMessage(UUID, UUID, String, boolean)} instead.
     *             Retained for G2-T02 backwards compatibility.
     */
    @Deprecated
    public static ConversationMessage createUserMessage(UUID sessionId, UUID userId, String content) {
        return createUserMessage(sessionId, userId, content, false);
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Boolean getRedacted() {
        return redacted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
