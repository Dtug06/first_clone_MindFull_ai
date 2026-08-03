package com.mindbridge.analysis.run.domain;

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
 * One row per invocation of a {@code ChatAnalysisProvider}. The lifecycle
 * is {@code PENDING → RUNNING → SUCCEEDED | FAILED}. The row is created
 * by {@code AiAnalysisRunService.startRun(...)} and updated in place as
 * the run progresses; multiple rows per {@code message_id} are allowed
 * for reruns (no UNIQUE constraint on message_id) per DB-MVP §5.1 rule
 * "Reprocess tạo run mới".
 *
 * <p><b>Immutability surface.</b> Per G3-T04 Phase 1 §2.1 the only
 * mutator is {@code com.mindbridge.analysis.run.domain.AiAnalysisRunService}
 * (same package as the entity so it can call the package-private
 * transition methods). The entity has NO setters at all (private fields,
 * accessed only via getters and the package-private transition methods),
 * so the compiler prevents any other package from changing the row. A
 * reflection-scan test guards this guarantee at the test layer (see
 * {@code AiAnalysisRunIntegrationTest}).
 *
 * <p><b>Append-only at the SQL level?</b> No. Unlike
 * {@code com.mindbridge.safety.resolver.RiskStateHistory} (which is
 * append-only per docs/04 §28), this row undergoes state transitions
 * (PENDING → RUNNING → SUCCEEDED/FAILED). However, the historical
 * record is preserved: the inspectable final state at
 * {@code completed_at} is the audit handle, and reruns create new
 * rows rather than updating old ones.
 *
 * <p>Schema invariants (V15 migration):
 * <ul>
 *   <li>Status enum: PENDING/RUNNING/SUCCEEDED/FAILED (CHECK constraint).</li>
 *   <li>Hash format: SHA-256 hex 64 chars (regex CHECK constraint).</li>
 *   <li>Succeded rows must have {@code output_hash} NOT NULL; failed rows
 *       must have {@code error_code} NOT NULL.</li>
 *   <li>Terminal rows must have {@code completed_at} NOT NULL.</li>
 *   <li>Timestamps ordered: {@code started_at >= created_at},
 *       {@code completed_at >= started_at}.</li>
 *   <li>{@code message_id} FK (default {@code NO ACTION}) to conversation_messages — see
 *       {@code V14__create_risk_state_history.sql} for the same pattern. A future
 *       retention task decides whether audit rows stay behind after a message deletion.</li>
 * </ul>
 *
 * <p><b>Schema version constant.</b> Bump only when the column layout
 * changes. The constant is also written to the row so audit can
 * reconstruct the row shape if the schema evolves.
 */
@Entity
@Table(name = "ai_analysis_runs")
public class AiAnalysisRun {

    /** Current schema version. Bump only when columns change. */
    public static final String CURRENT_SCHEMA_VERSION = "V1";

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 50, updatable = false)
    private String promptVersion;

    @Column(name = "schema_version", nullable = false, length = 10, updatable = false)
    private String schemaVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiAnalysisRunStatus status;

    @Column(name = "input_hash", nullable = false, length = 64, updatable = false)
    private String inputHash;

    @Column(name = "output_hash", length = 64)
    private String outputHash;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_summary", length = 200)
    private String errorSummary;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;

    @Column(name = "model_risk_level")
    private Short modelRiskLevel;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /** JPA-required no-arg constructor. Do not use directly. */
    protected AiAnalysisRun() {
    }

    /**
     * Factory for creating a new run row in the {@code PENDING} state.
     * Sets the immutable columns (id, message_id, user_id, provider,
     * model, prompt_version, schema_version, input_hash, created_at)
     * and leaves the lifecycle fields (status, output_hash, error_*,
     * latency_ms, started_at, completed_at = null) for the service to
     * populate as the run progresses.
     *
     * <p>Validates field invariants up front so a corrupt row cannot
     * be persisted. The DB-level CHECK constraints are a second line
     * of defence; this factory catches violations at unit-test time.
     */
    public static AiAnalysisRun createPending(
            UUID id,
            UUID messageId,
            UUID userId,
            String provider,
            String model,
            String promptVersion,
            String inputHash,
            OffsetDateTime createdAt) {

        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(promptVersion, "promptVersion must not be null");
        Objects.requireNonNull(inputHash, "inputHash must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");

        validateHashLength(inputHash, "inputHash");
        validateLabelLength(provider, "provider", 50);
        validateLabelLength(model, "model", 100);
        validateLabelLength(promptVersion, "promptVersion", 50);

        AiAnalysisRun row = new AiAnalysisRun();
        row.id = id;
        row.messageId = messageId;
        row.userId = userId;
        row.provider = provider;
        row.model = model;
        row.promptVersion = promptVersion;
        row.schemaVersion = CURRENT_SCHEMA_VERSION;
        row.status = AiAnalysisRunStatus.PENDING;
        row.inputHash = inputHash;
        row.latencyMs = 0;
        row.createdAt = createdAt;
        return row;
    }

    /**
     * Transition to {@code RUNNING}. Idempotent: if the row is already
     * RUNNING the call is a no-op. If the row is SUCCEEDED or FAILED,
     * throws — terminal rows cannot transition.
     *
     * @param startedAt the time the provider was invoked.
     */
    void markRunning(OffsetDateTime startedAt) {
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        if (status == AiAnalysisRunStatus.RUNNING) {
            return;
        }
        if (status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot transition from terminal status " + status + " to RUNNING");
        }
        if (startedAt.isBefore(createdAt)) {
            // DB CHECK enforces started_at >= created_at; we duplicate
            // it here so unit tests catch regressions before persistence.
            throw new IllegalArgumentException(
                    "startedAt must not be before createdAt");
        }
        this.status = AiAnalysisRunStatus.RUNNING;
        this.startedAt = startedAt;
    }

    /**
     * Transition to {@code SUCCEEDED}. Sets the output hash, latency,
     * risk level, confidence, and completed_at. Idempotent only if the
     * inbound output hash matches the already-stored one (otherwise
     * throws — we never overwrite a successful run with a different
     * output).
     *
     * @param outputHash SHA-256 hex of the canonical JSON serialization.
     * @param latencyMs wall-clock time the provider spent.
     * @param modelRiskLevel 1..4 or null if not provided.
     * @param confidence 0..1 or null if not provided.
     * @param completedAt the time the provider returned.
     */
    void markSucceeded(
            String outputHash,
            int latencyMs,
            Long inputTokens,
            Long outputTokens,
            Short modelRiskLevel,
            BigDecimal confidence,
            OffsetDateTime completedAt) {
        Objects.requireNonNull(outputHash, "outputHash must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        validateHashLength(outputHash, "outputHash");
        validateLatency(latencyMs);
        validateOptionalRiskLevel("modelRiskLevel", modelRiskLevel);
        validateOptionalConfidence("confidence", confidence);
        validateCompletedAt(completedAt);

        if (status == AiAnalysisRunStatus.SUCCEEDED) {
            if (!outputHash.equals(this.outputHash)) {
                throw new IllegalStateException(
                        "Cannot overwrite an already-SUCCEEDED run with a different outputHash");
            }
            return;
        }
        if (status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot transition from terminal status " + status + " to SUCCEEDED");
        }

        this.status = AiAnalysisRunStatus.SUCCEEDED;
        this.outputHash = outputHash;
        this.latencyMs = latencyMs;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.modelRiskLevel = modelRiskLevel;
        this.confidence = confidence;
        this.completedAt = completedAt;
        this.errorCode = null;
        this.errorSummary = null;
    }

    /**
     * Transition to {@code FAILED}. Sets the error code, error summary,
     * latency, and completed_at. The error summary is the redacted
     * human-readable note; this method does NOT add the raw chat
     * content — redactor is the caller's responsibility.
     *
     * @param errorCode the ErrorCode string (constrained to the 3 AI codes).
     * @param errorSummary a short, REDACTED message (max 200 chars). NEVER raw chat.
     * @param latencyMs wall-clock time spent before the exception.
     * @param completedAt the time the exception was caught.
     */
    void markFailed(
            String errorCode,
            String errorSummary,
            int latencyMs,
            OffsetDateTime completedAt) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        validateErrorCode(errorCode);
        validateErrorSummary(errorSummary);
        validateLatency(latencyMs);
        validateCompletedAt(completedAt);

        if (status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot transition from terminal status " + status + " to FAILED");
        }

        this.status = AiAnalysisRunStatus.FAILED;
        this.errorCode = errorCode;
        this.errorSummary = errorSummary;
        this.latencyMs = latencyMs;
        this.completedAt = completedAt;
        this.outputHash = null;
    }

    /**
     * Override the {@code provider} and {@code model} columns with the
     * ACTUAL labels returned by the upstream LLM.
     *
     * <p>Used by {@code AiAnalysisRunService} in G3-T06 when the active
     * {@link com.mindbridge.analysis.provider.ChatAnalysisProvider} is a
     * real LLM provider: the configured {@code provider} /
     * {@code mock-model} label written at {@code createPending(...)} time
     * is a placeholder; the upstream's actual identifier (which may differ
     * from the configured one, e.g. an OpenAI snapshot alias) is written
     * here just before the row is finalised.
     *
     * <p>Package-private on purpose. Only {@code AiAnalysisRunService}
     * (same package) is allowed to call it — preserves the "only the
     * service mutates the row" invariant from G3-T04.
     *
     * <p>Validates the same length constraints as {@link #createPending}.
     */
    void overrideProviderAndModel(String provider, String model) {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(model, "model must not be null");
        validateLabelLength(provider, "provider", 50);
        validateLabelLength(model, "model", 100);
        this.provider = provider;
        this.model = model;
    }

    @PrePersist
    void onCreate() {
        // Schema version safety net — the factory always sets it, but
        // if a row is constructed outside the factory (e.g. by a future
        // spring data initializer), default to V1.
        if (schemaVersion == null) {
            schemaVersion = CURRENT_SCHEMA_VERSION;
        }
    }

    // --- Getters (no setters on lifecycle fields). ---

    public UUID getId() { return id; }
    public UUID getMessageId() { return messageId; }
    public UUID getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public String getPromptVersion() { return promptVersion; }
    public String getSchemaVersion() { return schemaVersion; }
    public AiAnalysisRunStatus getStatus() { return status; }
    public String getInputHash() { return inputHash; }
    public String getOutputHash() { return outputHash; }
    public String getErrorCode() { return errorCode; }
    public String getErrorSummary() { return errorSummary; }
    public int getLatencyMs() { return latencyMs; }
    public Long getInputTokens() { return inputTokens; }
    public Long getOutputTokens() { return outputTokens; }
    public Short getModelRiskLevel() { return modelRiskLevel; }
    public BigDecimal getConfidence() { return confidence; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }

    // --- Validation helpers (private). ---

    private static void validateHashLength(String value, String name) {
        if (value.length() != 64) {
            throw new IllegalArgumentException(
                    name + " must be 64 hex chars (SHA-256) but was length " + value.length());
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                throw new IllegalArgumentException(
                        name + " must be lowercase hex (0-9 a-f) but char at index " + i + " is '"
                                + c + "'");
            }
        }
    }

    private static void validateLabelLength(String value, String name, int maxLength) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    name + " exceeds max length " + maxLength + " (was " + value.length() + ")");
        }
    }

    private static void validateLatency(int latencyMs) {
        if (latencyMs < 0) {
            throw new IllegalArgumentException(
                    "latencyMs must be >= 0 but was " + latencyMs);
        }
    }

    private static void validateOptionalRiskLevel(String name, Short value) {
        if (value == null) return;
        if (value < 1 || value > 4) {
            throw new IllegalArgumentException(
                    name + " must be in [1, 4] but was " + value);
        }
    }

    private static void validateOptionalConfidence(String name, BigDecimal value) {
        if (value == null) return;
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    name + " must be in [0.0, 1.0] but was " + value);
        }
    }

    private static void validateErrorCode(String value) {
        switch (value) {
            case "AI_PROVIDER_TIMEOUT":
            case "AI_PROVIDER_UNAVAILABLE":
            case "AI_ANALYSIS_OUTPUT_INVALID":
                return;
            default:
                throw new IllegalArgumentException(
                        "errorCode must be one of AI_PROVIDER_TIMEOUT / AI_PROVIDER_UNAVAILABLE / "
                                + "AI_ANALYSIS_OUTPUT_INVALID but was " + value);
        }
    }

    private static void validateErrorSummary(String value) {
        if (value == null) return;
        if (value.length() > 200) {
            throw new IllegalArgumentException(
                    "errorSummary exceeds max length 200 (was " + value.length() + ")");
        }
    }

    private void validateCompletedAt(OffsetDateTime completedAt) {
        if (startedAt != null && completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "completedAt must not be before startedAt");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AiAnalysisRun other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}