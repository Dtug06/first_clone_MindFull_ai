package com.mindbridge.safety.resolver;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Append-only record of one Safety Resolver decision for one user.
 *
 * <p>Created exclusively by
 * {@code com.mindbridge.safety.resolver.service.SafetyResolverService.resolve(...)}
 * — every resolution writes exactly one row. The row captures both the
 * signals that produced the decision (keyword, classifier, previous
 * state) and the final resolved level so that downstream consumers
 * (audit, G6 matching gate) can reconstruct the call without
 * re-evaluating the resolver.
 *
 * <p><b>Immutability contract (append-only).</b> Per
 * docs/04_SAFETY_AND_CBT_RULES.md §28 ("Safety history should be
 * append-only where practical") and the V14 migration header, this
 * entity has:
 * <ul>
 *   <li>No public setters — only the {@link #record(...) factory} and
 *       getters.</li>
 *   <li>No {@code @PreUpdate} hook (the entity cannot be updated via
 *       JPA dirty checking once persisted).</li>
 *   <li>No {@code @PreRemove} hook (rows are not deleted; ownership
 *       cascade on the FK does the cleanup if a user is removed).</li>
 * </ul>
 * Application code that needs to "change" a row must insert a new row
 * — that is the only mutation surface.
 *
 * <p><b>Scope.</b> Per G3-T10 Phase 1 decision Q1, the granularity is
 * per user (one history per {@code users.id}). The current risk state
 * is always the row with the latest {@code occurred_at}; ties are
 * broken by {@code id DESC} (mirror of the G2 acceptance decision #2
 * fix on {@code ConsentEventRepository}).
 */
@Entity
@Table(name = "risk_state_history")
public class RiskStateHistory implements com.mindbridge.safety.event.domain.SafetyEvent.RiskStateRow {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Final resolved risk level (the column DB-MVP §6.1 calls
     *  {@code risk_level}). Always equals {@code final_risk_level} from
     *  the resolver — kept under the schema-defined name so queries
     *  match the DB-MVP §6.1 spec. */
    @Column(name = "risk_level", nullable = false, updatable = false)
    private short riskLevel;

    /** Signal from G3-T09 LLM risk classifier; null when the classifier
     *  is unavailable / timed out / returned malformed output. */
    @Column(name = "model_risk_level", updatable = false)
    private Short modelRiskLevel;

    /** Signal from G3-T08 keyword/regex pre-filter (max of matched
     *  rules, or 1 if no rule matched). */
    @Column(name = "rule_risk_level", updatable = false)
    private Short ruleRiskLevel;

    /** The user's risk level BEFORE this resolution (audit per
     *  docs/04_SAFETY_AND_CBT_RULES.md §5). Null for the very first
     *  resolution of a user. */
    @Column(name = "current_risk_level", updatable = false)
    private Short currentRiskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30, updatable = false)
    private RiskStateSourceType sourceType;

    @Column(name = "source_id", updatable = false)
    private UUID sourceId;

    /** Snapshot of the rule set used (PreFilterResult.ruleVersion,
     *  comma-separated {@code code@version} pairs). {@code "NONE"} when
     *  no rule was loaded. */
    @Column(name = "rule_version", nullable = false, length = 200, updatable = false)
    private String ruleVersion;

    /** Snapshot of the classifier implementation identifier (e.g.
     *  {@code "MOCK_V1"}). Null when the classifier did not contribute. */
    @Column(name = "model_version", length = 100, updatable = false)
    private String modelVersion;

    /** Snapshot of the classifier prompt version (e.g.
     *  {@code "DEMO_V0"}). Null when the classifier did not contribute. */
    @Column(name = "prompt_version", length = 50, updatable = false)
    private String promptVersion;

    @Column(name = "confidence", nullable = false, precision = 4, scale = 3, updatable = false)
    private BigDecimal confidence;

    /**
     * Structured reason codes per DB-MVP §6.1 and docs/04 §7. Stored
     * as JSONB in PostgreSQL (mapped here as a String array; Hibernate
     * 6 handles the conversion via {@code @JdbcTypeCode(SqlTypes.JSON)}).
     * Always non-empty — at minimum contains a code identifying the
     * path taken (e.g. {@code "MAX_WINS_L4"}, {@code
     * "MANUAL_REVIEW_REQUIRED"}). Codes produced by the two signals
     * (classifier {@code reasonCodes[]} and pre-filter rule code) are
     * combined in front.
     */
    @Column(name = "reason_codes", nullable = false, columnDefinition = "jsonb", updatable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String[] reasonCodes;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "schema_version", nullable = false, length = 10, updatable = false)
    private String schemaVersion;

    /** Current DTO schema version. Bump only when fields change. */
    public static final String CURRENT_SCHEMA_VERSION = "V1";

    /** JPA-required no-arg constructor. Do not use directly. */
    protected RiskStateHistory() {
    }

    /**
     * Factory for creating a new history row. Returns a detached
     * instance — the caller is expected to hand it to the repository
     * inside a {@code @Transactional} method so the {@code occurred_at}
     * timestamp is captured consistently with the rest of the resolver
     * decision.
     *
     * <p>This factory does <b>not</b> perform the max-wins computation
     * or the downgrade guard — those are the resolver service's job.
     * Callers that bypass the service to insert raw rows should not
     * exist; if you find yourself writing one, please re-read
     * docs/04_SAFETY_AND_CBT_RULES.md §5.
     */
    public static RiskStateHistory record(
            UUID id,
            UUID userId,
            short riskLevel,
            Short modelRiskLevel,
            Short ruleRiskLevel,
            Short currentRiskLevel,
            RiskStateSourceType sourceType,
            UUID sourceId,
            String ruleVersion,
            String modelVersion,
            String promptVersion,
            BigDecimal confidence,
            String[] reasonCodes,
            OffsetDateTime occurredAt) {

        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(ruleVersion, "ruleVersion must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");

        validateRiskLevel("riskLevel", riskLevel);
        validateOptionalRiskLevel("modelRiskLevel", modelRiskLevel);
        validateOptionalRiskLevel("ruleRiskLevel", ruleRiskLevel);
        validateOptionalRiskLevel("currentRiskLevel", currentRiskLevel);

        if (confidence.compareTo(BigDecimal.ZERO) < 0
                || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "confidence must be in [0.0, 1.0] but was " + confidence);
        }
        if (reasonCodes.length == 0) {
            throw new IllegalArgumentException(
                    "reasonCodes must not be empty — at minimum one path-code is required");
        }
        for (int i = 0; i < reasonCodes.length; i++) {
            String code = reasonCodes[i];
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException(
                        "reasonCodes[" + i + "] must not be null or blank");
            }
        }
        if (ruleVersion.isBlank()) {
            throw new IllegalArgumentException("ruleVersion must not be blank");
        }

        // Defensive copy so the caller cannot mutate the array via the
        // reference it handed us.
        String[] codesCopy = new String[reasonCodes.length];
        System.arraycopy(reasonCodes, 0, codesCopy, 0, reasonCodes.length);

        RiskStateHistory row = new RiskStateHistory();
        row.id = id;
        row.userId = userId;
        row.riskLevel = riskLevel;
        row.modelRiskLevel = modelRiskLevel;
        row.ruleRiskLevel = ruleRiskLevel;
        row.currentRiskLevel = currentRiskLevel;
        row.sourceType = sourceType;
        row.sourceId = sourceId;
        row.ruleVersion = ruleVersion;
        row.modelVersion = modelVersion;
        row.promptVersion = promptVersion;
        row.confidence = confidence;
        row.reasonCodes = codesCopy;
        row.occurredAt = occurredAt;
        row.schemaVersion = CURRENT_SCHEMA_VERSION;
        return row;
    }

    private static void validateRiskLevel(String name, short value) {
        if (value < 1 || value > 4) {
            throw new IllegalArgumentException(
                    name + " must be in [1, 4] but was " + value);
        }
    }

    private static void validateOptionalRiskLevel(String name, Short value) {
        if (value == null) {
            return;
        }
        validateRiskLevel(name, value);
    }

    /**
     * Capture the timestamp on first persist. Uses the value already
     * set by the factory if present, so the resolver can pass a Clock
     * that is consistent with the rest of its decision.
     */
    @PrePersist
    void onCreate() {
        // occurredAt is set by the factory from the resolver's Clock —
        // we do NOT override it here. schemaVersion is also fixed by
        // the factory. This hook exists only as a safety net for any
        // case where a row is constructed outside the factory.
        if (occurredAt == null) {
            occurredAt = OffsetDateTime.now();
        }
        if (schemaVersion == null) {
            schemaVersion = CURRENT_SCHEMA_VERSION;
        }
    }

    // --- Getters (no public setters — append-only). ---

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public short getRiskLevel() {
        return riskLevel;
    }

    public Short getModelRiskLevel() {
        return modelRiskLevel;
    }

    public Short getRuleRiskLevel() {
        return ruleRiskLevel;
    }

    public Short getCurrentRiskLevel() {
        return currentRiskLevel;
    }

    public RiskStateSourceType getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String[] getReasonCodes() {
        // Defensive copy — JPA may hand the same array reference to
        // multiple readers; we never want a caller mutating a row
        // in place via the getter.
        if (reasonCodes == null) {
            return new String[0];
        }
        String[] copy = new String[reasonCodes.length];
        System.arraycopy(reasonCodes, 0, copy, 0, reasonCodes.length);
        return copy;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RiskStateHistory other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
