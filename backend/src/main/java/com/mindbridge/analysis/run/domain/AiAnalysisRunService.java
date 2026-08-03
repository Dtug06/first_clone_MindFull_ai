package com.mindbridge.analysis.run.domain;

import com.mindbridge.analysis.provider.AnalysisSchemaVersion;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.ChatAnalysisPromptVersion;
import com.mindbridge.analysis.run.dto.AiRunSummary;
import com.mindbridge.analysis.run.exception.AiAnalysisRunHashException;
import com.mindbridge.analysis.run.repository.AiAnalysisRunRepository;
import com.mindbridge.analysis.run.service.AiRunErrorRedactor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.exception.ProviderUnavailableException;
import com.mindbridge.analysis.provider.ChatAnalysisInput;
import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.analysis.provider.RealLlmResponseContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sole owner of the {@code ai_analysis_runs} lifecycle.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Create a {@code PENDING} row before invoking the provider.</li>
 *   <li>Transition the row to {@code RUNNING} and stamp
 *       {@code started_at}.</li>
 *   <li>Invoke the provider (NO database transaction around the
 *       call — see 10-backend.mdc §73).</li>
 *   <li>On success, transition to {@code SUCCEEDED} with full
 *       metadata (output_hash, latency_ms, model_risk_level,
 *       confidence, completed_at).</li>
 *   <li>On failure, transition to {@code FAILED} with the error
 *       code and a REDACTED error summary (NEVER raw chat
 *       content).</li>
 * </ol>
 *
 * <p>Each transition uses {@link Propagation#REQUIRES_NEW} so the
 * DB transaction is short and the slow external call happens
 * outside any DB transaction.
 *
 * <p>Concurrency: multiple concurrent {@code startRun(...)} calls
 * with the same {@code messageId} create multiple rows (last-write
 * semantics). There's no UNIQUE constraint on message_id. This is
 * consistent with DB-MVP §5.1 "Reprocess tạo run mới".
 *
 * <p>Provider/model/prompt metadata: read from configuration
 * (defaults to mock + MOCK_V1 + v1 prompt). Real provider (G3-T06)
 * can override via env / config.
 */
@Service
public class AiAnalysisRunService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisRunService.class);

    private final ChatAnalysisProvider provider;
    private final AiAnalysisRunRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private final String providerName;
    private final String mockModelLabel;
    private final String promptVersion;

    public AiAnalysisRunService(
            ChatAnalysisProvider provider,
            AiAnalysisRunRepository repository,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${mindbridge.ai.analysis-run.provider-label:mock}") String providerName,
            @Value("${mindbridge.ai.analysis-run.mock-model:MOCK_V1}") String mockModelLabel,
            @Value("${mindbridge.ai.analysis-run.prompt-version:" + ChatAnalysisPromptVersion.CURRENT + "}") String promptVersion) {
        this.provider = provider;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.providerName = providerName;
        this.mockModelLabel = mockModelLabel;
        this.promptVersion = promptVersion;
    }

    /**
     * Run the provider for the given input, persisting lifecycle
     * state to {@code ai_analysis_runs}. Returns the snapshot
     * summarizing the final state. Never throws provider exceptions
     * — they are caught and recorded as a {@code FAILED} row.
     *
     * @param input the message metadata + content to analyse.
     * @return the final-state snapshot.
     */
    public AiRunSummary startRun(ChatAnalysisInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }

        // 1. Compute the input hash BEFORE creating the row so the
        //    hash is part of the immutable initial state.
        String inputHash = sha256Hex(input.content());

        // 2. Create the PENDING row in a short transaction.
        UUID runId = UUID.randomUUID();
        OffsetDateTime createdAt = now();
        AiAnalysisRun row = AiAnalysisRun.createPending(
                runId,
                input.messageId(),
                input.userId(),
                providerName,
                mockModelLabel,
                promptVersion,
                inputHash,
                createdAt);
        savePending(row);

        // 3. Transition to RUNNING in a new short transaction.
        OffsetDateTime startedAt = now();
        markRunning(runId, startedAt);

        // 4. Invoke the provider (NO DB transaction around this).
        long invokeStartNanos = System.nanoTime();
        ChatAnalysisOutput output;
        // We capture the response-context snapshot here, inside the
        // try block, because the finally below clears the per-thread
        // slot unconditionally. Reading the snapshot AFTER the try
        // would always see null.
        RealLlmResponseContext.Snapshot responseSnapshot = null;
        try {
            // TODO_T11_PLACE: callers (chat pipeline) MUST call
            // ConsentGuard.requireChatAnalysisConsent(input.userId())
            // BEFORE invoking startRun(...). This service intentionally
            // does not enforce it — the consent rule is a domain
            // concern of the pipeline, not the run lifecycle owner.
            output = provider.analyze(input);
            // Snapshot only on the success path. The real LLM provider
            // sets it as part of a successful 200 response; failure
            // paths leave it null (and we don't want to override on a
            // failure row because the row will be marked FAILED with
            // a different provider/model semantics).
            responseSnapshot = RealLlmResponseContext.current();
        } catch (ProviderTimeoutException ex) {
            int latencyMs = (int) elapsedMillis(invokeStartNanos);
            OffsetDateTime failedAt = now();
            String errorCode = com.mindbridge.common.exception.ErrorCode
                    .AI_PROVIDER_TIMEOUT.getCode();
            String summary = AiRunErrorRedactor.redact(ex.getMessage());
            markFailed(runId, errorCode, summary, latencyMs, failedAt);
            log.warn("ai_analysis_run failed runId={} status=FAILED code={} latencyMs={} summary={}",
                    runId, errorCode, latencyMs, summary);
            return requireSummary(runId);
        } catch (ProviderUnavailableException ex) {
            int latencyMs = (int) elapsedMillis(invokeStartNanos);
            OffsetDateTime failedAt = now();
            String errorCode = com.mindbridge.common.exception.ErrorCode
                    .AI_PROVIDER_UNAVAILABLE.getCode();
            String summary = AiRunErrorRedactor.redact(ex.getMessage());
            markFailed(runId, errorCode, summary, latencyMs, failedAt);
            log.warn("ai_analysis_run failed runId={} status=FAILED code={} latencyMs={} summary={}",
                    runId, errorCode, latencyMs, summary);
            return requireSummary(runId);
        } catch (InvalidAnalysisOutputException ex) {
            int latencyMs = (int) elapsedMillis(invokeStartNanos);
            OffsetDateTime failedAt = now();
            String errorCode = com.mindbridge.common.exception.ErrorCode
                    .AI_ANALYSIS_OUTPUT_INVALID.getCode();
            String summary = AiRunErrorRedactor.redact(ex.getMessage());
            markFailed(runId, errorCode, summary, latencyMs, failedAt);
            log.warn("ai_analysis_run failed runId={} status=FAILED code={} latencyMs={} summary={}",
                    runId, errorCode, latencyMs, summary);
            return requireSummary(runId);
        } catch (RuntimeException ex) {
            // Defensive: any unexpected exception is recorded as INVALID
            // output (closest matching code). The caller still gets a
            // snapshot so the outer pipeline can decide what to do.
            int latencyMs = (int) elapsedMillis(invokeStartNanos);
            OffsetDateTime failedAt = now();
            String errorCode = com.mindbridge.common.exception.ErrorCode
                    .AI_ANALYSIS_OUTPUT_INVALID.getCode();
            String summary = AiRunErrorRedactor.redact(ex.getMessage());
            markFailed(runId, errorCode, summary, latencyMs, failedAt);
            log.warn("ai_analysis_run failed runId={} status=FAILED code={} latencyMs={} summary={} causeClass={}",
                    runId, errorCode, latencyMs, summary, ex.getClass().getSimpleName());
            return requireSummary(runId);
        } finally {
            // Always clear the per-thread response context so a
            // subsequent (non-real) call on the same thread never
            // inherits a stale snapshot. Mock provider never calls
            // set() so this is a no-op for the mock path.
            RealLlmResponseContext.clear();
        }

        // 4a. If the real provider captured an actual model label
        //     via RealLlmResponseContext, apply the override to the
        //     RUNNING row so ai_analysis_runs.provider / model
        //     reflect the upstream identifier, not the configured
        //     placeholder. The mock provider does not populate the
        //     context so this branch is a no-op for it.
        //     Note: the snapshot was read INSIDE the try block above;
        //     the finally has already cleared the per-thread slot by
        //     the time we reach this line, so we use the local var.
        if (responseSnapshot != null) {
            applyRealProviderLabels(runId, responseSnapshot.provider(), responseSnapshot.model());
        }

        // 5. Transition to SUCCEEDED in a new short transaction.
        int latencyMs = (int) elapsedMillis(invokeStartNanos);
        OffsetDateTime completedAt = now();
        String outputHash = sha256OfOutput(output);
        // Note: inputTokens / outputTokens are not part of ChatAnalysisOutput
        // v1 (G3-T02 design decision — token usage is not surfaced at the
        // chat analysis layer). The columns are NULLABLE in V15 so the
        // values are stored as null. A future RealLlmChatAnalysisProvider
        // (G3-T06) may surface these by extending the output record.
        markSucceeded(
                runId,
                outputHash,
                latencyMs,
                null,  // inputTokens
                null,  // outputTokens
                (short) output.modelRiskLevel(),
                java.math.BigDecimal.valueOf(output.confidence()),
                completedAt);

        log.info("ai_analysis_run succeeded runId={} status=SUCCEEDED latencyMs={} modelRiskLevel={} confidence={}",
                runId, latencyMs, output.modelRiskLevel(), output.confidence());
        return requireSummary(runId);
    }

    // --- Private helpers (transaction boundaries). ---

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void savePending(AiAnalysisRun row) {
        repository.save(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markRunning(UUID runId, OffsetDateTime startedAt) {
        AiAnalysisRun row = repository.findById(runId)
                .orElseThrow(() -> new IllegalStateException(
                        "Row not found immediately after insert: " + runId));
        row.markRunning(startedAt);
        repository.save(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markFailed(
            UUID runId,
            String errorCode,
            String errorSummary,
            int latencyMs,
            OffsetDateTime completedAt) {
        AiAnalysisRun row = repository.findById(runId)
                .orElseThrow(() -> new IllegalStateException(
                        "Row not found immediately after insert: " + runId));
        row.markFailed(errorCode, errorSummary, latencyMs, completedAt);
        repository.save(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markSucceeded(
            UUID runId,
            String outputHash,
            int latencyMs,
            Long inputTokens,
            Long outputTokens,
            Short modelRiskLevel,
            java.math.BigDecimal confidence,
            OffsetDateTime completedAt) {
        AiAnalysisRun row = repository.findById(runId)
                .orElseThrow(() -> new IllegalStateException(
                        "Row not found immediately after insert: " + runId));
        row.markSucceeded(outputHash, latencyMs, inputTokens, outputTokens,
                modelRiskLevel, confidence, completedAt);
        repository.save(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    protected AiRunSummary requireSummary(UUID runId) {
        AiAnalysisRun row = repository.findById(runId)
                .orElseThrow(() -> new IllegalStateException(
                        "Row not found after completion: " + runId));
        return AiRunSummary.from(row);
    }

    /**
     * Override the {@code provider} and {@code model} columns on the
     * RUNNING row to record the actual identifiers returned by the
     * upstream LLM (G3-T06). No-op when called by the mock provider —
     * the mock never populates {@link RealLlmResponseContext} so the
     * service checks the snapshot before calling this helper.
     *
     * <p>Own transaction, short. Reuses the entity's package-private
     * mutator so the "only this service mutates the row" invariant
     * from G3-T04 is preserved.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void applyRealProviderLabels(UUID runId, String provider, String model) {
        AiAnalysisRun row = repository.findById(runId)
                .orElseThrow(() -> new IllegalStateException(
                        "Row not found when applying real provider labels: " + runId));
        row.overrideProviderAndModel(provider, model);
        repository.save(row);
    }

    // --- Hashing helpers (pure, no DB). ---

    private String sha256Hex(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is JDK-mandated; this branch is unreachable in
            // a compliant JVM. Surface as a clean failure.
            throw new AiAnalysisRunHashException(ex);
        }
    }

    private String sha256OfOutput(ChatAnalysisOutput output) {
        try {
            String canonical = objectMapper.writeValueAsString(output);
            return sha256Hex(canonical);
        } catch (JsonProcessingException ex) {
            throw new AiAnalysisRunHashException(ex);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    /**
     * The schema version embedded in every row. Mirrors
     * {@link AnalysisSchemaVersion#CURRENT_SCHEMA_VERSION} but
     * scoped to this entity. Kept as a static for the test layer.
     */
    public static String currentSchemaVersion() {
        return AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION;
    }
}