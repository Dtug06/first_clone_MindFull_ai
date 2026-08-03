package com.mindbridge.analysis.result.service;

import com.mindbridge.analysis.provider.AnalysisSchemaVersion;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.result.domain.ChatAnalysisResult;
import com.mindbridge.analysis.result.domain.ResultAnalysisStatus;
import com.mindbridge.analysis.result.dto.ChatAnalysisResultSummary;
import com.mindbridge.analysis.result.exception.ChatAnalysisResultStateException;
import com.mindbridge.analysis.result.repository.ChatAnalysisResultRepository;
import com.mindbridge.analysis.run.domain.AiAnalysisRun;
import com.mindbridge.analysis.run.domain.AiAnalysisRunStatus;
import com.mindbridge.analysis.run.repository.AiAnalysisRunRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sole owner of the {@code chat_analysis_results} lifecycle.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Persist a new result row from a {@code SUCCEEDED} analysis run.</li>
 *   <li>Transition the previously ACTIVE result (if any) for the same
 *       message to {@code SUPERSEDED} before writing the new row.</li>
 *   <li>Provide the "effective result" query (single ACTIVE row per message).</li>
 *   <li>Allow admin invalidation of an ACTIVE row.</li>
 * </ol>
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Only SUCCEEDED runs produce result rows. FAILED runs are not
 *       recorded — the run failure itself is the audit trail.</li>
 *   <li>The "at most one ACTIVE per message" invariant is enforced by
 *       both the application layer (this service) and the PostgreSQL trigger
 *       in V16. The trigger is the last line of defence.</li>
 *   <li>The supersedes_id chain is never pruned.</li>
 *   <li>Log messages intentionally omit raw chat content, user-provided
 *       signal text, and any SHA-256 textHash values.</li>
 * </ul>
 *
 * <p>Transaction boundary: the entire record-result operation (find old
 * ACTIVE, mark superseded, insert new row) runs in one transaction so
 * the trigger never sees a window with two ACTIVE rows.
 */
@Service
public class ChatAnalysisResultService {

    private static final Logger log = LoggerFactory.getLogger(ChatAnalysisResultService.class);

    private final ChatAnalysisResultRepository resultRepository;
    private final AiAnalysisRunRepository runRepository;
    private final Clock clock;

    public ChatAnalysisResultService(
            ChatAnalysisResultRepository resultRepository,
            AiAnalysisRunRepository runRepository,
            Clock clock) {
        this.resultRepository = resultRepository;
        this.runRepository = runRepository;
        this.clock = clock;
    }

    /**
     * Record the analysis output for a SUCCEEDED run.
     *
     * <p>If the message already has an ACTIVE result, it is transitioned
     * to SUPERSEDED (supersedes_id chain preserved). The new row becomes
     * ACTIVE. Both operations happen in one transaction so the DB trigger
     * never sees two ACTIVE rows simultaneously.
     *
     * <p>Only SUCCEEDED runs produce result rows. Calling this for a
     * non-SUCCEEDED run throws.
     *
     * @param runId  the id of the ai_analysis_runs row (must be SUCCEEDED).
     * @param output the structured output from the provider (already validated
     *               against the ChatAnalysisOutput record invariants).
     * @return the newly persisted result summary.
     * @throws IllegalArgumentException if {@code runId} is null.
     * @throws IllegalStateException   if the run is not SUCCEEDED.
     * @throws ChatAnalysisResultStateException if the trigger rejects the state transition.
     */
    @Transactional
    public ChatAnalysisResultSummary recordResult(UUID runId, ChatAnalysisOutput output) {
        if (runId == null) {
            throw new IllegalArgumentException("runId must not be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("output must not be null");
        }

        // 1. Load and validate the run.
        AiAnalysisRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Run not found: " + runId));

        if (run.getStatus() != AiAnalysisRunStatus.SUCCEEDED) {
            throw new IllegalStateException(
                    "Cannot record result for a non-SUCCEEDED run (runId=" + runId
                            + ", status=" + run.getStatus() + "). "
                            + "Only SUCCEEDED runs produce result rows.");
        }

        UUID messageId = run.getMessageId();
        UUID userId = run.getUserId();
        OffsetDateTime createdAt = OffsetDateTime.now(clock);

        // 2. Find and supersede the existing ACTIVE row (if any).
        //    The order (supersece first, then insert) ensures the trigger
        //    never sees two ACTIVE rows for the same message.
        Optional<ChatAnalysisResult> existingActive = resultRepository
                .findEffectiveByConversationMessageId(messageId);

        UUID newId = UUID.randomUUID();

        if (existingActive.isPresent()) {
            ChatAnalysisResult old = existingActive.get();
            old.markSuperseded(newId);
            resultRepository.save(old);
            log.info("chat_analysis_result superseded oldId={} supersededBy={} messageId={}",
                    old.getId(), newId, messageId);
        }

        // 3. Create and persist the new ACTIVE row.
        ChatAnalysisResult newRow = ChatAnalysisResult.create(
                newId,
                runId,
                messageId,
                userId,
                output,
                createdAt);

        ChatAnalysisResult saved = resultRepository.save(newRow);

        log.info("chat_analysis_result recorded id={} runId={} messageId={} "
                        + "topic={} emotion={} intent={} modelRiskLevel={} confidence={}",
                saved.getId(), runId, messageId,
                output.topic(), output.emotion(), output.intent(),
                output.modelRiskLevel(), output.confidence());

        return ChatAnalysisResultSummary.from(saved);
    }

    /**
     * Returns the current ACTIVE result for the given message, or empty
     * if no active result exists.
     *
     * @param messageId the conversation_messages.id to look up.
     * @return the single ACTIVE result, or empty.
     */
    @Transactional(readOnly = true)
    public Optional<ChatAnalysisResultSummary> getEffectiveResult(UUID messageId) {
        return resultRepository.findEffectiveByConversationMessageId(messageId)
                .map(ChatAnalysisResultSummary::from);
    }

    /**
     * Invalidate an ACTIVE result (admin path). The row transitions to
     * INVALIDATED. This does NOT automatically activate a previous SUPERSEDED
     * row — admin must decide which result should be authoritative next.
     *
     * @param resultId the result row to invalidate.
     * @param reason   the reason (audit note; not persisted, only logged).
     * @throws IllegalArgumentException if {@code resultId} is null.
     * @throws ChatAnalysisResultStateException if the row is not ACTIVE.
     */
    @Transactional
    public void invalidateResult(UUID resultId, String reason) {
        if (resultId == null) {
            throw new IllegalArgumentException("resultId must not be null");
        }

        ChatAnalysisResult row = resultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalStateException("Result not found: " + resultId));

        try {
            row.markInvalidated();
            resultRepository.save(row);
            log.info("chat_analysis_result invalidated id={} reason={}", resultId, reason);
        } catch (IllegalStateException ex) {
            throw new ChatAnalysisResultStateException(
                    "Cannot invalidate result id=" + resultId
                            + " (current status=" + row.getAnalysisStatus() + "): " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Returns the schema version embedded in every result row.
     * Mirrors {@link AnalysisSchemaVersion#CURRENT_SCHEMA_VERSION}.
     */
    public static String currentSchemaVersion() {
        return AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION;
    }
}
