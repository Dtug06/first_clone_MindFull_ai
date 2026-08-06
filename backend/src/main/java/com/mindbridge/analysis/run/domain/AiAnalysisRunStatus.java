package com.mindbridge.analysis.run.domain;

/**
 * Lifecycle status of an {@code ai_analysis_runs} row.
 *
 * <p>Mirrors and extends the processing statuses required by
 * {@code docs/02_DATABASE_MVP.md} §5.1. DB-MVP §5.1 lists three
 * statuses (PENDING/SUCCEEDED/FAILED); this enum adds {@link #RUNNING}
 * so the row can distinguish "created but provider not yet called"
 * from "provider invoked, awaiting response". This matches the
 * explicit processing statuses called out in
 * {@code 10-backend.mdc} §74.
 *
 * <p>Transitions:
 * <pre>
 *   PENDING  → RUNNING  → SUCCEEDED | FAILED
 * </pre>
 * The transitions are owned exclusively by
 * {@code com.mindbridge.analysis.run.service.AiAnalysisRunService}.
 * The setters on the entity are package-private; only the service
 * may mutate the status field.
 *
 * <p>DB-level invariant: the CHECK constraint
 * {@code ai_analysis_runs_terminal_chk} enforces that SUCCEEDED/FAILED
 * rows have {@code completed_at IS NOT NULL} and PENDING/RUNNING rows
 * have {@code completed_at IS NULL}. The enum value is stored as a
 * VARCHAR(20) string (see V15 migration).
 */
public enum AiAnalysisRunStatus {

    /** Row created, provider not yet invoked. Initial state. */
    PENDING,

    /** Provider invoked (analyze() call in progress). Transitive state. */
    RUNNING,

    /** Provider returned a valid {@code ChatAnalysisOutput}. Terminal state. */
    SUCCEEDED,

    /** Provider threw (timeout / unavailable / invalid output). Terminal state. */
    FAILED;

    /**
     * Whether this status is terminal — i.e. the run will not transition
     * further. Returned by the service as a convenience for callers that
     * want to short-circuit on terminal rows.
     */
    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }
}