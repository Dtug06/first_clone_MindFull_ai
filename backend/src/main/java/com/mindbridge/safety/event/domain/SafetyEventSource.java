package com.mindbridge.safety.event.domain;

import com.mindbridge.safety.event.SafetyEventSourceType;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class SafetyEventSource {

    private UUID id;
    private UUID safetyEventId;
    private SafetyEventSourceType sourceType;
    private UUID sourceId;
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
}