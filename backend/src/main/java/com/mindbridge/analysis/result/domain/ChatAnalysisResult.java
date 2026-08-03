package com.mindbridge.analysis.result.domain;

import com.mindbridge.analysis.provider.AnalysisSchemaVersion;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.Emotion;
import com.mindbridge.analysis.provider.EvidenceSpan;
import com.mindbridge.analysis.provider.Intent;
import com.mindbridge.analysis.provider.Signal;
import com.mindbridge.analysis.provider.Topic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One row per {@link ChatAnalysisOutput} produced by a {@code ChatAnalysisProvider}
 * invocation, bound to one {@code ai_analysis_runs} row.
 *
 * <p><b>Immutability surface.</b> Per G3-T05 Phase 1 §4, the sole owner of
 * mutations is {@link ChatAnalysisResultService} (same package, so it can call
 * the package-private transition methods). The entity has NO public setters;
 * all field assignments flow through the {@link #create} factory and the
 * package-private transition methods. A reflection-scan test guards this
 * at the test layer.
 *
 * <p><b>Responsibility boundary.</b> This entity stores ONLY the chat analysis
 * model's own preliminary risk signal ({@code model_risk_level}). The
 * {@code rule_risk_level} and {@code final_risk_level} belong to
 * {@code risk_state_history} (V14, G3-T10 Safety Resolver) — they are NOT
 * stored here. This is the key invariant that keeps the two risk tracks
 * separate per docs/04 §5.
 *
 * <p><b>Versioning / rerun contract.</b>
 * <ul>
 *   <li>ACTIVE: current authoritative result for this message. At most one
 *       ACTIVE per {@code conversation_message_id} (enforced by PG trigger).</li>
 *   <li>SUPERSEDED: was ACTIVE; replaced by a newer result whose
 *       {@code supersedes_id} points at this row.</li>
 *   <li>INVALIDATED: rejected post-write. Never authoritative.</li>
 * </ul>
 *
 * <p><b>Schema version.</b> Mirrors
 * {@link AnalysisSchemaVersion#CURRENT_SCHEMA_VERSION} (= "V1").
 */
@Entity
@Table(name = "chat_analysis_results")
public class ChatAnalysisResult {

    /** Current schema version. Bump only when the column layout changes. */
    public static final String CURRENT_SCHEMA_VERSION = "V1";

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "analysis_run_id", nullable = false, updatable = false)
    private UUID analysisRunId;

    @Column(name = "conversation_message_id", nullable = false, updatable = false)
    private UUID conversationMessageId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "topic", nullable = false, length = 40, updatable = false)
    private String topic;

    @Column(name = "emotion", nullable = false, length = 20, updatable = false)
    private String emotion;

    @Column(name = "intent", nullable = false, length = 20, updatable = false)
    private String intent;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "signals", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String[] signals;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "evidence_spans", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String[] evidenceSpans;

    @Column(name = "model_risk_level", nullable = false, updatable = false)
    private Short modelRiskLevel;

    @Column(name = "confidence", nullable = false, precision = 4, scale = 3, updatable = false)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 20)
    private ResultAnalysisStatus analysisStatus;

    @Column(name = "supersedes_id", updatable = false)
    private UUID supersedesId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** JPA-required no-arg constructor. Do not use directly. */
    protected ChatAnalysisResult() {
    }

    // --- Getters (no public setters — immutability contract). ---

    public UUID getId()                       { return id; }
    public UUID getAnalysisRunId()             { return analysisRunId; }
    public UUID getConversationMessageId()     { return conversationMessageId; }
    public UUID getUserId()                   { return userId; }
    public String getTopic()                  { return topic; }
    public String getEmotion()                { return emotion; }
    public String getIntent()                 { return intent; }
    public String[] getSignals()              { return signals; }
    public String[] getEvidenceSpans()        { return evidenceSpans; }
    public Short getModelRiskLevel()          { return modelRiskLevel; }
    public BigDecimal getConfidence()         { return confidence; }
    public ResultAnalysisStatus getAnalysisStatus() { return analysisStatus; }
    public UUID getSupersedesId()             { return supersedesId; }
    public OffsetDateTime getCreatedAt()      { return createdAt; }

    // --- Transition methods (called by ChatAnalysisResultService). ---

    /**
     * Transition this row from ACTIVE to SUPERSEDED.
     * Called by {@code ChatAnalysisResultService} when a newer result replaces it.
     *
     * @param supersedesId the id of the row that is replacing this one.
     * @throws IllegalStateException if the current status is not ACTIVE.
     */
    public void markSuperseded(UUID supersedesId) {
        if (this.analysisStatus != ResultAnalysisStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot mark as SUPERSEDED: current status is " + this.analysisStatus);
        }
        this.analysisStatus = ResultAnalysisStatus.SUPERSEDED;
        this.supersedesId = supersedesId;
    }

    /**
     * Transition this row from ACTIVE to INVALIDATED.
     * Called by {@code ChatAnalysisResultService} during admin invalidation.
     *
     * @throws IllegalStateException if the current status is not ACTIVE.
     */
    public void markInvalidated() {
        if (this.analysisStatus != ResultAnalysisStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot mark as INVALIDATED: current status is " + this.analysisStatus);
        }
        this.analysisStatus = ResultAnalysisStatus.INVALIDATED;
    }

    // --- Factory. ---

    /**
     * Create a new ACTIVE result row from the given analysis output.
     *
     * <p>Maps {@link ChatAnalysisOutput} (singular fields) to DB columns
     * (mostly singular as well) per the mapping table in G3-T05 Phase 1 §5.
     * The JSONB arrays ({@code signals}, {@code evidence_spans}) are serialised
     * as string arrays in the entity and stored as JSONB in the DB.
     *
     * @param id            the row UUID.
     * @param runId         the ai_analysis_runs.id this result is bound to.
     * @param messageId     the conversation_messages.id this result is bound to.
     * @param userId        the owning user (denormalised from the message).
     * @param output        the structured output from the provider.
     * @param createdAt     the instant this row was created (clock-injected).
     * @return a new ACTIVE result row, ready to be persisted.
     */
    public static ChatAnalysisResult create(
            UUID id,
            UUID runId,
            UUID messageId,
            UUID userId,
            ChatAnalysisOutput output,
            OffsetDateTime createdAt) {

        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");

        validateRiskLevel("modelRiskLevel", output.modelRiskLevel());
        validateConfidence("confidence", output.confidence());

        ChatAnalysisResult row = new ChatAnalysisResult();
        row.id = id;
        row.analysisRunId = runId;
        row.conversationMessageId = messageId;
        row.userId = userId;
        row.topic = output.topic().name();
        row.emotion = output.emotion().name();
        row.intent = output.intent().name();
        row.signals = output.signals().stream()
                .map(Signal::name)
                .toArray(String[]::new);
        row.evidenceSpans = output.evidenceSpans().stream()
                .map(ChatAnalysisResult::serializeEvidenceSpan)
                .toArray(String[]::new);
        row.modelRiskLevel = (short) output.modelRiskLevel();
        row.confidence = BigDecimal.valueOf(output.confidence());
        row.analysisStatus = ResultAnalysisStatus.ACTIVE;
        row.supersedesId = null;
        row.createdAt = createdAt;
        return row;
    }

    /**
     * Map the entity's JSONB signal array to a typed List for the DTO.
     * Returns an empty list when the database returned null (defensive).
     */
    public List<String> getSignalsAsList() {
        return signals == null ? List.of() : List.of(signals);
    }

    /**
     * Map the entity's JSONB evidence_spans array to a typed List for the DTO.
     * Returns an empty list when the database returned null (defensive).
     */
    public List<String> getEvidenceSpansAsList() {
        return evidenceSpans == null ? List.of() : List.of(evidenceSpans);
    }

    // --- Private helpers. ---

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    private static void validateRiskLevel(String name, int value) {
        if (value < 1 || value > 4) {
            throw new IllegalArgumentException(
                    name + " must be in [1, 4] but was " + value);
        }
    }

    private static void validateConfidence(String name, double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    name + " must be in [0.0, 1.0] but was " + value);
        }
    }

    private static String serializeEvidenceSpan(EvidenceSpan span) {
        return String.format(
                "{\"start\":%d,\"end\":%d,\"textHash\":\"%s\"}",
                span.start(), span.end(), span.textHash());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatAnalysisResult other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
