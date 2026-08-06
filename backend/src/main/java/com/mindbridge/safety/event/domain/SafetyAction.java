package com.mindbridge.safety.event.domain;

import com.mindbridge.safety.event.SafetyActionStatus;
import com.mindbridge.safety.event.SafetyActionType;
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
 * JPA entity for the safety_actions table (V17).
 *
 * <p>One Safety Event spawns N Safety Actions. Each action is
 * independent: one action FAILED MUST NOT block another SUCCEEDED
 * (G3-T11 Phase 1 decision C7). The action is created with status
 * PENDING by the chat pipeline at T11; runtime execution is owned by
 * the consuming module:
 * <ul>
 *   <li>SHOW_TEMPLATE - G3-T12</li>
 *   <li>BLOCK_MATCHING - G6</li>
 *   <li>FLAG_REVIEW - G3-T13</li>
 *   <li>PAUSE_PROGRAM - future CBT task</li>
 * </ul>
 *
 * <p><b>Append-friendly lifecycle:</b> rows are created at T11; the
 * executor updates status, error_message, and executed_at as work
 * progresses. T11 ships no transition code; transitions are owned by
 * the executor module that writes to this row. The SafetyEventService
 * only inserts PENDING rows.
 *
 * <p><b>Transition API (G3-T12):</b> the SHOW_TEMPLATE executor
 * (SafetyResponseTemplateExecutor) consumes PENDING actions for a
 * Safety Event and transitions them to SUCCEEDED / FAILED / SKIPPED
 * via the controlled methods below. Transitions are intentionally
 * narrow: only the status, error_message, and executed_at fields
 * change; id / safety_event_id / action_type / created_at stay
 * immutable.
 *
 * <p><b>NOTE on UPDATE:</b> this entity is designed to receive
 * updates (status transitions). Updates are restricted to controlled
 * transition methods below.
 */
@Entity
@Table(name = "safety_actions")
public class SafetyAction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "safety_event_id", nullable = false, updatable = false)
    private UUID safetyEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30, updatable = false)
    private SafetyActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SafetyActionStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * G3-T12: audit FK to {@code safety_response_templates.id}.
     * Written only via the {@code markSucceeded(...)
     * markFailed(...) markSkipped(...)} overloads below. Nullable
     * because PENDING rows do not have a template yet, and SKIPPED
     * rows intentionally stay NULL (the absence of a template is
     * itself the audit signal). FK uses ON DELETE SET NULL so a future
     * retention policy that hard-deletes a template row keeps the
     * audit action row meaningful (the {@code template_version} label
     * still tells ops what was shown).
     */
    @Column(name = "template_id")
    private UUID templateId;

    /**
     * G3-T12: audit label of the template version that was shown
     * (e.g. {@code "v1"}). VARCHAR(50) matches the parent table's
     * {@code safety_response_templates.template_version} so a JOIN
     * survives even after the FK target is retired. Nullable for the
     * same reasons as {@link #templateId}.
     */
    @Column(name = "template_version", length = 50)
    private String templateVersion;

    /** JPA-required no-arg constructor. Do not use directly. */
    protected SafetyAction() {
    }

    /**
     * Factory for a new PENDING action attached to a Safety Event.
     * Always creates status = PENDING; execution transitions belong
     * to the consuming module.
     */
    public static SafetyAction pending(
            UUID id,
            UUID safetyEventId,
            SafetyActionType actionType) {

        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(safetyEventId, "safetyEventId must not be null");
        Objects.requireNonNull(actionType, "actionType must not be null");

        SafetyAction row = new SafetyAction();
        row.id = id;
        row.safetyEventId = safetyEventId;
        row.actionType = actionType;
        row.status = SafetyActionStatus.PENDING;
        return row;
    }

    /**
     * Transition: PENDING -> SUCCEEDED. Records the timestamp. Used
     * by the SHOW_TEMPLATE executor when a fixed response template
     * was successfully resolved and shown to the user.
     */
    public void markSucceeded() {
        if (status != SafetyActionStatus.PENDING) {
            throw new IllegalStateException(
                    "markSucceeded only valid from PENDING but status was " + status);
        }
        this.status = SafetyActionStatus.SUCCEEDED;
        this.executedAt = OffsetDateTime.now();
        this.errorMessage = null;
    }

    /**
     * Transition: PENDING -> SUCCEEDED, recording which expert-approved
     * Safety response template was shown. G3-T12 overload.
     *
     * <p>Used by {@code SafetyResponseTemplateExecutor} (T12) when the
     * {@code ResolvedResponse} is non-empty. Both arguments are required
     * so the audit row can answer "which template version was shown"
     * later (per docs/04 §3.4 "Sử dụng fixed approved Safety Response"
     * + the G3-T12 acceptance criteria "audit sau này biết đã show đúng
     * bản duyệt nào").
     *
     * @param templateId      the approved {@code safety_response_templates.id}
     *                        row that was returned to the user (UUID, required)
     * @param templateVersion the approved row's {@code template_version}
     *                        label (non-blank, length 1..50 to match the
     *                        column on the parent table)
     */
    public void markSucceeded(UUID templateId, String templateVersion) {
        requireTemplate(templateId, templateVersion);
        if (status != SafetyActionStatus.PENDING) {
            throw new IllegalStateException(
                    "markSucceeded only valid from PENDING but status was " + status);
        }
        this.status = SafetyActionStatus.SUCCEEDED;
        this.executedAt = OffsetDateTime.now();
        this.errorMessage = null;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
    }

    /**
     * Transition: PENDING -> FAILED. Records the timestamp + error.
     * Used by the SHOW_TEMPLATE executor when an unexpected error
     * occurred while resolving / delivering the response. Distinct
     * from markSkipped which is reserved for did-not-run cases.
     */
    public void markFailed(String errorMessage) {
        if (status != SafetyActionStatus.PENDING) {
            throw new IllegalStateException(
                    "markFailed only valid from PENDING but status was " + status);
        }
        this.status = SafetyActionStatus.FAILED;
        this.executedAt = OffsetDateTime.now();
        this.errorMessage = errorMessage;
    }

    /**
     * Transition: PENDING -> FAILED, recording which template the executor
     * tried to show before the failure. G3-T12 overload.
     *
     * <p>Used by the SHOW_TEMPLATE executor when the template was found
     * but delivery to the chat pipeline raised an exception. The
     * {@code templateVersion} label is preserved for audit even though
     * the user did not actually see the response.
     */
    public void markFailed(UUID templateId, String templateVersion, String errorMessage) {
        requireTemplate(templateId, templateVersion);
        if (status != SafetyActionStatus.PENDING) {
            throw new IllegalStateException(
                    "markFailed only valid from PENDING but status was " + status);
        }
        this.status = SafetyActionStatus.FAILED;
        this.executedAt = OffsetDateTime.now();
        this.errorMessage = errorMessage;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
    }

    /**
     * Transition: PENDING -> SKIPPED. Used by the SHOW_TEMPLATE
     * executor when no approved template was found. Records a short
     * reason so ops can see the gap; never emits fabricated content.
     */
    public void markSkipped(String reason) {
        if (status != SafetyActionStatus.PENDING) {
            throw new IllegalStateException(
                    "markSkipped only valid from PENDING but status was " + status);
        }
        this.status = SafetyActionStatus.SKIPPED;
        this.executedAt = OffsetDateTime.now();
        this.errorMessage = reason;
    }

    /**
     * Transition: PENDING -> SKIPPED, recording a partial template
     * reference (or none) for audit. G3-T12 overload.
     *
     * <p>Used by the SHOW_TEMPLATE executor in two cases:
     * <ol>
     *   <li><b>No template resolved at all</b> - caller passes
     *       {@code (null, null)}; the row's template columns stay NULL
     *       and the {@code reason} explains the gap. This is the
     *       "TODO_EXPERT_REVIEW" sentinel case (per docs/04 §12).</li>
     *   <li><b>Partial match - default fallback used</b> - caller passes
     *       the default-row's {@code (templateId, templateVersion)} so
     *       audit can confirm "the locale default was served".</li>
     * </ol>
     *
     * <p>At least one of the {@code (templateId, templateVersion)}
     * pair must be present: passing both null is permitted (case 1),
     * but passing one null and one non-null is rejected because a
     * dangling label is worse than no label.
     */
    public void markSkipped(UUID templateId, String templateVersion, String reason) {
        if (templateId == null ^ templateVersion == null) {
            throw new IllegalArgumentException(
                    "templateId and templateVersion must both be null or both be non-null");
        }
        if (templateId != null) {
            validateTemplateVersion(templateVersion);
        }
        if (status != SafetyActionStatus.PENDING) {
            throw new IllegalStateException(
                    "markSkipped only valid from PENDING but status was " + status);
        }
        this.status = SafetyActionStatus.SKIPPED;
        this.executedAt = OffsetDateTime.now();
        this.errorMessage = reason;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
    }

    /**
     * Shared input-validation for the two-arg markXxx overloads.
     * Both fields must be present; templateVersion must be 1..50 chars
     * to match the parent table's column width.
     */
    private static void requireTemplate(UUID templateId, String templateVersion) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        Objects.requireNonNull(templateVersion, "templateVersion must not be null");
        validateTemplateVersion(templateVersion);
    }

    private static void validateTemplateVersion(String templateVersion) {
        if (templateVersion.isBlank()) {
            throw new IllegalArgumentException("templateVersion must not be blank");
        }
        if (templateVersion.length() > 50) {
            throw new IllegalArgumentException(
                    "templateVersion length must be <= 50 but was " + templateVersion.length());
        }
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = SafetyActionStatus.PENDING;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSafetyEventId() {
        return safetyEventId;
    }

    public SafetyActionType getActionType() {
        return actionType;
    }

    public SafetyActionStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyAction other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}