package com.mindbridge.dailyquestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for the {@code daily_question_assignments} table.
 *
 * Each row represents one (user, template version, local date) — i.e. one question
 * for one user on one calendar day in their timezone.
 *
 * Invariants (G2-T05):
 * - The template version is pinned at assignment time. Even if the admin publishes
 *   a newer template version later the same day, the existing assignment keeps
 *   pointing to the original version — historical consistency.
 * - The user_id is taken from the JWT principal at the service layer; never from
 *   the request body.
 * - DB-level UNIQUE (user_id, template_version_id, assigned_for_date) acts as a
 *   last-line defense against duplicate inserts. The service uses the broader
 *   (user_id, template_code, assigned_for_date) lookup to decide whether to assign.
 * - No setters for version-pinning fields; only the status mutates via controlled
 *   transitions (answered / skipped / expired).
 */
@Entity
@Table(name = "daily_question_assignments")
public class DailyQuestionAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private DailyQuestionTemplate templateVersion;

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    @Column(name = "assigned_for_date", nullable = false)
    private LocalDate assignedForDate;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DailyQuestionAssignment() {
    }

    private DailyQuestionAssignment(UUID userId, DailyQuestionTemplate templateVersion,
                                    String templateCode, LocalDate assignedForDate,
                                    String timezone, AssignmentStatus status,
                                    Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.templateVersion = templateVersion;
        this.templateCode = templateCode;
        this.assignedForDate = assignedForDate;
        this.timezone = timezone;
        this.status = status;
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
        if (this.status == null) {
            this.status = AssignmentStatus.ASSIGNED;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Factory: creates a new ASSIGNED assignment.
     */
    public static DailyQuestionAssignment create(UUID userId, DailyQuestionTemplate templateVersion,
                                                 LocalDate assignedForDate, String timezone) {
        Instant now = Instant.now();
        return new DailyQuestionAssignment(
                userId,
                templateVersion,
                templateVersion.getCode(),
                assignedForDate,
                timezone,
                AssignmentStatus.ASSIGNED,
                now,
                now
        );
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public DailyQuestionTemplate getTemplateVersion() {
        return templateVersion;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public LocalDate getAssignedForDate() {
        return assignedForDate;
    }

    public String getTimezone() {
        return timezone;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Controlled transition: marks the assignment as answered.
     */
    public void markAnswered() {
        this.status = AssignmentStatus.ANSWERED;
    }

    /**
     * Controlled transition: marks the assignment as skipped.
     */
    public void markSkipped() {
        this.status = AssignmentStatus.SKIPPED;
    }
}
