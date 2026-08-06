package com.mindbridge.common.domain.entity;

import com.mindbridge.common.audit.AuditActorType;
import com.mindbridge.common.audit.AuditCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity for the {@code audit_logs} table (V3__create_consent_and_audit.sql).
 *
 * Append-only by design:
 * - Only setters limited to fields supplied at creation time.
 * - Application code never updates or deletes audit rows.
 * - Retention is not enforced in code; see docs/LOGGING.md.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditCategory category;

    @Column(nullable = false, length = 50)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private AuditActorType actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "subject_type", length = 50)
    private String subjectType;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    private AuditLog(AuditCategory category, String action, AuditActorType actorType,
                     UUID actorId, String subjectType, UUID subjectId,
                     String requestId, String metadata) {
        this.category = category;
        this.action = action;
        this.actorType = actorType;
        this.actorId = actorId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.requestId = requestId;
        this.metadata = metadata;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    /**
     * Factory method. The application never mutates AuditLog fields after
     * creation — the entity has no setters for writeable fields on purpose.
     */
    public static AuditLog create(AuditCategory category, String action,
                                  AuditActorType actorType, UUID actorId,
                                  String subjectType, UUID subjectId,
                                  String requestId, String metadata) {
        return new AuditLog(category, action, actorType, actorId,
                subjectType, subjectId, requestId, metadata);
    }

    public UUID getId() {
        return id;
    }

    public AuditCategory getCategory() {
        return category;
    }

    public String getAction() {
        return action;
    }

    public AuditActorType getActorType() {
        return actorType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}