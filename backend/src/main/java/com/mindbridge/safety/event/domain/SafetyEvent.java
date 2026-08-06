package com.mindbridge.safety.event.domain;

import com.mindbridge.safety.event.SafetyEventStatus;
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

@Entity
@Table(name = "safety_events")
public class SafetyEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "risk_state_id", nullable = false, updatable = false)
    private UUID riskStateId;

    @Column(name = "risk_level", nullable = false, updatable = false)
    private short riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SafetyEventStatus status;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    protected SafetyEvent() {
    }

    public static SafetyEvent open(UUID id, RiskStateRow riskStateRow, String summary) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(riskStateRow, "riskStateRow must not be null");

        short level = riskStateRow.getRiskLevel();
        if (level < 3 || level > 4) {
            throw new IllegalArgumentException("riskLevel must be in range [3, 4], got: " + level);
        }

        SafetyEvent event = new SafetyEvent();
        event.id = id;
        event.userId = riskStateRow.getUserId();
        event.riskStateId = riskStateRow.getId();
        event.riskLevel = level;
        event.status = SafetyEventStatus.OPEN;
        event.summary = summary;
        event.createdAt = OffsetDateTime.now();
        return event;
    }

    public static SafetyEvent openFor(UUID id, UUID userId, RiskStateRow riskStateRow, String summary) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(riskStateRow, "riskStateRow must not be null");

        short level = riskStateRow.getRiskLevel();
        if (level < 1 || level > 4) {
            throw new IllegalArgumentException("riskLevel must be in range [1, 4], got: " + level);
        }

        SafetyEvent event = new SafetyEvent();
        event.id = id;
        event.userId = userId;
        event.riskStateId = riskStateRow.getId();
        event.riskLevel = level;
        event.status = SafetyEventStatus.OPEN;
        event.summary = summary;
        event.createdAt = OffsetDateTime.now();
        return event;
    }

    public void markUnderReview() {
        if (this.status != SafetyEventStatus.OPEN) {
            throw new IllegalStateException("markUnderReview only valid from OPEN but status was " + this.status);
        }
        this.status = SafetyEventStatus.UNDER_REVIEW;
    }

    public void markResolved() {
        if (this.status != SafetyEventStatus.UNDER_REVIEW) {
            throw new IllegalStateException("markResolved only valid from UNDER_REVIEW but status was " + this.status);
        }
        this.status = SafetyEventStatus.RESOLVED;
        this.resolvedAt = OffsetDateTime.now();
    }

    public void markDismissed() {
        if (this.status != SafetyEventStatus.UNDER_REVIEW) {
            throw new IllegalStateException("markDismissed only valid from UNDER_REVIEW but status was " + this.status);
        }
        this.status = SafetyEventStatus.DISMISSED;
        this.resolvedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getRiskStateId() { return riskStateId; }
    public short getRiskLevel() { return riskLevel; }
    public SafetyEventStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }

    void setId(UUID id) { this.id = id; }
    void setUserId(UUID userId) { this.userId = userId; }
    void setRiskStateId(UUID riskStateId) { this.riskStateId = riskStateId; }
    void setRiskLevel(short riskLevel) { this.riskLevel = riskLevel; }
    public void setStatus(SafetyEventStatus status) { this.status = status; }
    public void setSummary(String summary) { this.summary = summary; }
    void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyEvent other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public interface RiskStateRow {
        UUID getId();
        UUID getUserId();
        short getRiskLevel();
    }
}