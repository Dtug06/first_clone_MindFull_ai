package com.mindbridge.safety.event.domain;

import com.mindbridge.safety.event.SafetyEventSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Polymorphic reference to the row that triggered a {@link SafetyEvent}.
 * Mirrors {@code docs/02_DATABASE_MVP.md} section 6.3 and the
 * {@code safety_event_sources} table created in {@code V17__create_safety_events.sql}.
 *
 * <p>The {@code (source_type, source_id)} pair identifies the source row;
 * the application layer ({@code SafetyEventService}) verifies ownership
 * via the typed repository. Per the G3-T11 Phase 1 decision C5, the
 * trade-off of NO DB-level FK on the polymorphic {@code source_id} column
 * is accepted.
 *
 * <p>Note: {@code source_id} is nullable in the schema (a source may be
 * recorded with no concrete upstream row, e.g. system-detected risk).
 */
@Entity
@Table(name = "safety_event_sources")
public class SafetyEventSource {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "safety_event_id", nullable = false, updatable = false)
    private UUID safetyEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30, updatable = false)
    private SafetyEventSourceType sourceType;

    @Column(name = "source_id", updatable = false)
    private UUID sourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SafetyEventSource() {
    }

    private SafetyEventSource(UUID id, UUID safetyEventId, SafetyEventSourceType sourceType,
                             UUID sourceId, OffsetDateTime createdAt) {
        this.id = id;
        this.safetyEventId = safetyEventId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.createdAt = createdAt;
    }

    public static SafetyEventSource of(UUID id, UUID safetyEventId,
                                       SafetyEventSourceType sourceType, UUID sourceId) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(safetyEventId, "safetyEventId must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");

        return new SafetyEventSource(id, safetyEventId, sourceType, sourceId, OffsetDateTime.now());
    }

    public UUID getId() { return id; }
    public UUID getSafetyEventId() { return safetyEventId; }
    public SafetyEventSourceType getSourceType() { return sourceType; }
    public UUID getSourceId() { return sourceId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    void setId(UUID id) { this.id = id; }
    void setSafetyEventId(UUID safetyEventId) { this.safetyEventId = safetyEventId; }
    void setSourceType(SafetyEventSourceType sourceType) { this.sourceType = sourceType; }
    void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
    void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyEventSource other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}