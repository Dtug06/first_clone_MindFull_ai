package com.mindbridge.safety.response.domain;

import com.mindbridge.safety.response.SafetyResponseTemplateStatus;
import com.mindbridge.safety.response.exception.SafetyResponseTemplateInputException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code safety_response_templates} table (V18).
 *
 * <p>A versioned, expert-approved safety response template keyed by
 * (locale, risk_reason). The {@code SHOW_TEMPLATE} executor (also part of
 * G3-T12) reads rows in status {@link SafetyResponseTemplateStatus#APPROVED}
 * directly from this table  WITHOUT calling any LLM  so the L4 fixed
 * response flow stays available when the AI provider is unavailable
 * (DoD  4.3 and docs/04 section 3.4).
 *
 * <p><b>Schema invariants</b> (see V18 migration):
 * <ul>
 *   <li>{@code (code, template_version)} is {@code UNIQUE}. The natural
 *       key identifies a specific immutable version of a template family.</li>
 *   <li>{@code locale IN ('vi')} for MVP. Adding locales is a future
 *       migration.</li>
 *   <li>{@code risk_reason} matches the UPPER_SNAKE_CASE convention from
 *       the resolver reason codes (docs/04 section 7). The literal
 *       {@code 'DEFAULT'} is reserved for the per-locale fallback row
 *       (see {@link #isDefault}).</li>
 *   <li>{@code status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'RETIRED')}.
 *       Only APPROVED rows are returned to users.</li>
 *   <li>If {@code status = 'APPROVED'} then both {@code approved_by} and
 *       {@code approved_at} must be non-null.</li>
 *   <li>If {@code risk_reason = 'DEFAULT'} then {@code is_default = TRUE};
 *       otherwise {@code is_default = FALSE}.</li>
 *   <li>At most one APPROVED row per {@code (code, locale, risk_reason)},
 *       and at most one APPROVED default row per locale. Enforced by
 *       partial unique indexes in the PostgreSQL production schema and
 *       by the service-layer approval logic (mirrors V13 pattern).</li>
 * </ul>
 *
 * <p><b>Immutability contract</b> (rule 30-database-ai-safety.mdc
 * "Safety history append-only where practical"):
 * <ul>
 *   <li>{@link #code}, {@link #templateVersion}, {@link #locale},
 *       {@link #riskReason}, and {@link #isDefault} are immutable after
 *       creation. Changing them requires a new row with
 *       {@code template_version + 1}.</li>
 *   <li>{@link #content} is editable only while the row is
 *       {@link SafetyResponseTemplateStatus#DRAFT}; once the row enters
 *       {@link SafetyResponseTemplateStatus#PENDING_REVIEW} it is frozen
 *       (any further content change means submitting a new version).</li>
 *   <li>Status transitions go through the controlled methods
 *       {@link #submitForReview()}, {@link #approve(UUID)}, and
 *       {@link #retire()}. No public setter for {@code status}.</li>
 *   <li>Approval sets {@code approved_by} (UUID) and {@code approved_at}
 *       (now). The service layer enforces role-EXPERT-or-ADMIN before
 *       calling {@link #approve(UUID)}; this entity trusts that check.</li>
 * </ul>
 */
@Entity
@Table(name = "safety_response_templates")
public class SafetyResponseTemplate {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 100, updatable = false)
    private String code;

    @Column(name = "template_version", nullable = false, length = 50, updatable = false)
    private String templateVersion;

    @Column(name = "locale", nullable = false, length = 10, updatable = false)
    private String locale;

    @Column(name = "risk_reason", nullable = false, length = 100, updatable = false)
    private String riskReason;

    /**
     * The expert-authored response text. Editable only while
     * {@link #status} = {@code DRAFT}; frozen thereafter. Never raw chat
     * content  authored by reviewers and gated by the approval flow.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Marks the per-locale fallback row (the sentinel {@code risk_reason}
     * is always {@code 'DEFAULT'} when this is true). See V18 partial
     * unique index {@code safety_response_templates_one_default_per_locale_uq}.
     */
    @Column(name = "is_default", nullable = false, updatable = false)
    private boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SafetyResponseTemplateStatus status;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** JPA optimistic lock  protects against concurrent status changes. */
    @Version
    @Column(name = "lock_version")
    private Long lockVersion;

    /** JPA-required no-arg constructor. Do not use directly. */
    protected SafetyResponseTemplate() {
    }

    /**
     * Factory for a new DRAFT row. Defaults to status {@code DRAFT}; the
     * content must be non-blank (the entity never stores blank or
     * placeholder text on the hot path  the executor rejects such rows
     * even if they slip through approval).
     *
     * <p>Callers MUST pass {@code isDefault = true} IFF
     * {@code riskReason.equals("DEFAULT")}. The DB CHECK constraint
     * {@code safety_response_templates_default_marker_chk} (V18) will
     * reject mismatched values, but we validate here for fail-fast.
     *
     * @param id              the entity id (UUID)
     * @param code            template family code, e.g. {@code "SAFETY_LEVEL_4_VI_V1"}
     * @param templateVersion version label, e.g. {@code "v1"}; must be non-blank
     * @param locale          BCP-47-ish locale; MVP whitelist {@code "vi"}
     * @param riskReason      UPPER_SNAKE_CASE reason code (or the literal
     *                        {@code "DEFAULT"} sentinel for the fallback)
     * @param content         non-blank, expert-authored response text
     * @param isDefault       {@code true} only for the per-locale fallback row
     */
    public static SafetyResponseTemplate create(
            UUID id,
            String code,
            String templateVersion,
            String locale,
            String riskReason,
            String content,
            boolean isDefault) {

        Objects.requireNonNull(id, "id must not be null");
        requireNonBlank(code, "code");
        requireNonBlank(templateVersion, "templateVersion");
        requireNonBlank(locale, "locale");
        requireNonBlank(riskReason, "riskReason");
        requireNonBlank(content, "content");

        validateRiskReason(riskReason);
        validateLocale(locale);
        if (isDefault && !"DEFAULT".equals(riskReason)) {
            throw new SafetyResponseTemplateInputException(
                    "isDefault = true requires riskReason = 'DEFAULT' but was '"
                            + riskReason + "'");
        }
        if (!isDefault && "DEFAULT".equals(riskReason)) {
            throw new SafetyResponseTemplateInputException(
                    "riskReason = 'DEFAULT' requires isDefault = true");
        }

        SafetyResponseTemplate row = new SafetyResponseTemplate();
        row.id = id;
        row.code = code;
        row.templateVersion = templateVersion;
        row.locale = locale;
        row.riskReason = riskReason;
        row.content = content;
        row.isDefault = isDefault;
        row.status = SafetyResponseTemplateStatus.DRAFT;
        return row;
    }

    /**
     * Content edit gate. Allowed only while status = DRAFT. Once a row is
     * submitted for review, the content is frozen  any change must go
     * through a new version row.
     */
    public void updateContent(String newContent) {
        requireNonBlank(newContent, "newContent");
        if (status != SafetyResponseTemplateStatus.DRAFT) {
            throw new SafetyResponseTemplateInputException(
                    "updateContent only valid from DRAFT but status was " + status);
        }
        this.content = newContent;
    }

    /** Transition: {@code DRAFT  PENDING_REVIEW}. */
    public void submitForReview() {
        if (status != SafetyResponseTemplateStatus.DRAFT) {
            throw new SafetyResponseTemplateInputException(
                    "submitForReview only valid from DRAFT but status was " + status);
        }
        this.status = SafetyResponseTemplateStatus.PENDING_REVIEW;
    }

    /**
     * Transition: {@code PENDING_REVIEW  APPROVED}. Sets {@code approved_by}
     * and {@code approved_at}. The caller (service layer) MUST verify the
     * approver has role EXPERT or ADMIN before invoking this method; the
     * entity trusts the caller's authorization check.
     */
    public void approve(UUID approverId) {
        if (status != SafetyResponseTemplateStatus.PENDING_REVIEW) {
            throw new SafetyResponseTemplateInputException(
                    "approve only valid from PENDING_REVIEW but status was " + status);
        }
        Objects.requireNonNull(approverId, "approverId must not be null");
        this.status = SafetyResponseTemplateStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = OffsetDateTime.now();
    }

    /** Transition: {@code APPROVED  RETIRED}. */
    public void retire() {
        if (status != SafetyResponseTemplateStatus.APPROVED) {
            throw new SafetyResponseTemplateInputException(
                    "retire only valid from APPROVED but status was " + status);
        }
        this.status = SafetyResponseTemplateStatus.RETIRED;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new SafetyResponseTemplateInputException(
                    name + " must not be null or blank");
        }
    }

    private static void validateRiskReason(String riskReason) {
        // UPPER_SNAKE_CASE with first char A-Z, length 2..100. Mirrors the
        // docs/04 section 7 reason-code convention. "DEFAULT" sentinel is
        // explicitly allowed (it matches the regex).
        if (!riskReason.matches("^[A-Z][A-Z0-9_]{1,99}$")) {
            throw new SafetyResponseTemplateInputException(
                    "riskReason must match UPPER_SNAKE_CASE but was '" + riskReason + "'");
        }
    }

    private static void validateLocale(String locale) {
        if (!"vi".equals(locale)) {
            throw new SafetyResponseTemplateInputException(
                    "locale must be in MVP whitelist {'vi'} but was '" + locale + "'");
        }
    }

    // --- Getters (no public setters for immutable / transition-controlled fields) ---

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public String getLocale() {
        return locale;
    }

    public String getRiskReason() {
        return riskReason;
    }

    public String getContent() {
        return content;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public SafetyResponseTemplateStatus getStatus() {
        return status;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getLockVersion() {
        return lockVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyResponseTemplate other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
