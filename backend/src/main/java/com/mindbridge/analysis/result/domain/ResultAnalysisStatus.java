package com.mindbridge.analysis.result.domain;

/**
 * Lifecycle state of a {@code chat_analysis_results} row.
 *
 * <p>Design follows the same pattern as {@code AiAnalysisRunStatus} (T04):
 * explicit named states rather than boolean flags, with a helper that
 * communicates the business meaning.
 *
 * @see com.mindbridge.analysis.result.domain.ChatAnalysisResult
 */
public enum ResultAnalysisStatus {

    /**
     * The current authoritative result for its conversation message.
     * At most one ACTIVE row may exist per {@code conversation_message_id}
     * at any time (enforced by a PostgreSQL trigger and by the
     * application-layer in {@code ChatAnalysisResultService}).
     */
    ACTIVE,

    /**
     * Was previously ACTIVE; has been replaced by a newer result whose
     * {@code supersedes_id} points at this row. The supersedes chain
     * preserves full audit history of all analysis attempts.
     */
    SUPERSEDED,

    /**
     * Rejected post-write (e.g. schema validation failed after persist,
     * or an admin manually invalidated it). Never authoritative.
     */
    INVALIDATED;

    /**
     * Whether this status represents the authoritative result for its
     * message — i.e. whether downstream consumers may use it as the
     * current analysis output.
     */
    public boolean isAuthoritative() {
        return this == ACTIVE;
    }
}
