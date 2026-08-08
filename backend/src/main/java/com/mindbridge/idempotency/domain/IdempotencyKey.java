package com.mindbridge.idempotency.domain;

import com.mindbridge.auth.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity for {@code idempotency_keys}.
 *
 * Per G2-T08 plan §3.3-§3.6. Records a successful (2xx) response for a
 * client-supplied idempotency key so that subsequent requests with the same
 * key + same endpoint + same user replay the original response exactly.
 *
 * The row is immutable after insert. There is no setter for the response
 * fields — once a response is recorded, it must stay the same for the replay
 * path to be deterministic.
 *
 * TTL: 24 hours from {@code created_at}. After expiry, the service treats
 * the key as absent and the request creates a new idempotency record.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    /** TTL for an idempotency record. Per G2-T08 plan §3.4. */
    public static final Duration TTL = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "endpoint", nullable = false, length = 64)
    private String endpoint;

    @Column(name = "key_value", nullable = false, length = 64)
    private String keyValue;

    @Column(name = "response_status", nullable = false)
    private Short responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = false, columnDefinition = "jsonb")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyKey() {
    }

    private IdempotencyKey(User user, String endpoint, String keyValue,
                            short responseStatus, String responseBody,
                            Instant createdAt, Instant expiresAt) {
        this.user = user;
        this.endpoint = endpoint;
        this.keyValue = keyValue;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /**
     * Factory: builds a new idempotency record with TTL applied via the supplied clock.
     *
     * @param user           the authenticated user (must be a managed entity)
     * @param endpoint       logical endpoint identifier, e.g. "POST:/chat/sessions/{sessionId}/messages"
     * @param keyValue       client-supplied UUID (max 64 chars)
     * @param responseStatus 2xx HTTP status to record
     * @param responseBody   JSON-serialized response body (must be non-null)
     * @param clock          clock used for created_at and expires_at
     */
    public static IdempotencyKey create(User user, String endpoint, String keyValue,
                                         short responseStatus, String responseBody,
                                         Clock clock) {
        Instant now = clock.instant();
        return new IdempotencyKey(user, endpoint, keyValue, responseStatus, responseBody,
                now, now.plus(TTL));
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.expiresAt == null) {
            this.expiresAt = this.createdAt.plus(TTL);
        }
    }

    public boolean isExpired(Clock clock) {
        return expiresAt.isBefore(clock.instant());
    }

    /**
     * Test-only setter for {@code expiresAt}. Used by integration tests that
     * need to simulate a TTL-expired idempotency record without waiting 24h.
     * NOT exposed via any API surface.
     */
    public void setExpiresAtForTest(java.time.Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public Short getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
