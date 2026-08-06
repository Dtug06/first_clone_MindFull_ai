package com.mindbridge.safety.domain;

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
 * A versioned safety keyword or regex rule used by the pre-filter
 * service to flag user content for downstream Safety Resolver
 * evaluation.
 *
 * <p>Schema invariants (see V13 migration):
 * <ul>
 *   <li>{@code (code, rule_version)} is {@code UNIQUE}.</li>
 *   <li>At most one row per {@code code} may be in status
 *       {@link SafetyRuleStatus#APPROVED} at a time (partial unique
 *       index).</li>
 *   <li>{@code match_type ∈ {KEYWORD, REGEX}},
 *       {@code status ∈ {DRAFT, PENDING_REVIEW, APPROVED, RETIRED}},
 *       {@code preliminary_risk ∈ [1, 4]} — all enforced by CHECK
 *       constraints.</li>
 * </ul>
 *
 * <p>Immutability contract (rule 30-database-ai-safety.mdc — Safety
 * history append-only where practical):
 * <ul>
 *   <li>{@link #pattern} and {@link #matchType} are immutable after
 *       creation. To change them, create a new rule version with
 *       {@code rule_version + 1}.</li>
 *   <li>Status transitions are controlled by {@link #submitForReview()},
 *       {@link #approve(UUID)}, and {@link #retire()} — no public setter
 *       for {@code status}.</li>
 * </ul>
 */
@Entity
@Table(name = "safety_keyword_rules")
public class SafetyKeywordRule {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 100, updatable = false)
    private String code;

    @Column(name = "rule_version", nullable = false, length = 50, updatable = false)
    private String ruleVersion;

    @Column(name = "pattern", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String pattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20, updatable = false)
    private MatchType matchType;

    @Column(name = "preliminary_risk", nullable = false, updatable = false)
    private short preliminaryRisk;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SafetyRuleStatus status;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** JPA optimistic lock — protects against concurrent status changes. */
    @Version
    @Column(name = "lock_version")
    private Long lockVersion;

    /** JPA-required no-arg constructor. Do not use directly. */
    protected SafetyKeywordRule() {
    }

    /**
     * Factory for creating a new draft rule. The rule starts in
     * {@link SafetyRuleStatus#DRAFT}; reviewers must call
     * {@link #submitForReview()} then {@link #approve(UUID)} before it
     * is eligible for pre-filter evaluation.
     */
    public static SafetyKeywordRule create(
            UUID id,
            String code,
            String ruleVersion,
            String pattern,
            MatchType matchType,
            short preliminaryRisk) {

        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(ruleVersion, "ruleVersion must not be null");
        Objects.requireNonNull(pattern, "pattern must not be null");
        Objects.requireNonNull(matchType, "matchType must not be null");

        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (ruleVersion.isBlank()) {
            throw new IllegalArgumentException("ruleVersion must not be blank");
        }
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("pattern must not be blank");
        }
        if (preliminaryRisk < 1 || preliminaryRisk > 4) {
            throw new IllegalArgumentException(
                    "preliminaryRisk must be in [1, 4] but was " + preliminaryRisk);
        }

        SafetyKeywordRule rule = new SafetyKeywordRule();
        rule.id = id;
        rule.code = code;
        rule.ruleVersion = ruleVersion;
        rule.pattern = pattern;
        rule.matchType = matchType;
        rule.preliminaryRisk = preliminaryRisk;
        rule.status = SafetyRuleStatus.DRAFT;
        return rule;
    }

    /** Transition: {@code DRAFT → PENDING_REVIEW}. */
    public void submitForReview() {
        if (status != SafetyRuleStatus.DRAFT) {
            throw new IllegalStateException(
                    "submitForReview only valid from DRAFT but was " + status);
        }
        this.status = SafetyRuleStatus.PENDING_REVIEW;
    }

    /**
     * Transition: {@code PENDING_REVIEW → APPROVED}. Sets
     * {@code approved_by} and {@code approved_at}.
     *
     * @param approverId the user id of the expert who approved the rule.
     */
    public void approve(UUID approverId) {
        if (status != SafetyRuleStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "approve only valid from PENDING_REVIEW but was " + status);
        }
        Objects.requireNonNull(approverId, "approverId must not be null");
        this.status = SafetyRuleStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = OffsetDateTime.now();
    }

    /** Transition: {@code APPROVED → RETIRED}. */
    public void retire() {
        if (status != SafetyRuleStatus.APPROVED) {
            throw new IllegalStateException(
                    "retire only valid from APPROVED but was " + status);
        }
        this.status = SafetyRuleStatus.RETIRED;
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

    // --- Getters (no public setters for immutable fields) ---

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public String getPattern() {
        return pattern;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public short getPreliminaryRisk() {
        return preliminaryRisk;
    }

    public SafetyRuleStatus getStatus() {
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
        if (!(o instanceof SafetyKeywordRule other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
