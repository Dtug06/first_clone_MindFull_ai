-- V15 — G3-T04: AI analysis runs (lifecycle trace of every ChatAnalysisProvider call)
--
-- Stores one row per AI analysis invocation. The lifecycle is
--   PENDING → RUNNING → SUCCEEDED | FAILED
-- Exactly one row is created per call to AiAnalysisRunService.startRun(...);
-- the same row is updated in place as the run progresses (no overwriting of
-- distinct historical runs — multiple rows per message_id are allowed for
-- reruns, per DB-MVP §5.1 rule "Reprocess tạo run mới").
--
-- Schema mirrors DB-MVP §5.1 with the following adjustments, all approved in
-- G3-T04 Phase 1 (2026-08-02):
--   * 4-state status (PENDING/RUNNING/SUCCEEDED/FAILED) instead of DB-MVP's
--     3-state (PENDING/SUCCEEDED/FAILED). RUNNING is added so the row
--     distinguishes "row created but provider not yet called" from "provider
--     invoked, awaiting response". Mirrors 10-backend.mdc §74 explicit
--     processing statuses.
--   * Hash columns split (input_hash / output_hash) instead of a single
--     raw-prompt column. Per G3-T04 Q2 user decision: SHA-256 hex 64-char,
--     raw content is NEVER stored in DB, log, or file. The hash is the
--     only on-disk audit handle.
--   * input_tokens / output_tokens are two separate BIGINT NULL columns
--     instead of DB-MVP's `token_usage` JSONB. Per rule 28 "Do not use JSONB
--     as a replacement for every typed field" + rule 29 "Frequently queried
--     metrics must use typed columns". T01 Mock returns null; T06 Real will
--     populate when the provider supports it.
--   * timestamps: created_at (row created) + started_at (provider invoked)
--     + completed_at (terminal). Operators can compute "time spent in
--     PENDING queue" = started_at - created_at.
--   * user_id is a denormalized snapshot captured at run-creation time from
--     the authenticated principal (not a foreign key). The source of truth
--     is still conversation_messages.user_id; this column exists for ops
--     dashboard queries that should not require a JOIN. Per rule 25 "Add
--     foreign keys deliberately" — we keep the message_id FK (referential
--     integrity enforced) and skip the user_id FK (snapshot semantic).
--   * error_code is constrained to the 3 existing AI ErrorCode enum values
--     (AI_PROVIDER_TIMEOUT, AI_PROVIDER_UNAVAILABLE, AI_ANALYSIS_OUTPUT_INVALID)
--     so the row cannot store arbitrary garbage text. error_summary is a
--     short VARCHAR(200) REDACTED human-readable note — NEVER raw chat.
--   * model_risk_level and confidence are pulled from the ChatAnalysisOutput
--     record so the row is self-sufficient for audit (no JOIN needed to
--     reconstruct "what did the model say?").
--
-- IMPORTANT invariants (enforced by SQL CHECK constraints):
--   * Status lifecycle: PENDING/RUNNING must have completed_at = NULL;
--     SUCCEEDED/FAILED must have completed_at NOT NULL.
--   * Success path: status = SUCCEEDED ⇒ output_hash IS NOT NULL.
--   * Failure path: status = FAILED ⇒ error_code IS NOT NULL.
--   * Timestamps ordered: started_at >= created_at, completed_at >= started_at.
--   * Hash format: SHA-256 hex 64 chars (lowercase). Enforced by regex.
--   * Risk level & confidence bounds: Same as existing rules 30 rules.
--   * message_id FK (default NO ACTION): deleting a conversation message
--     cannot silently orphan audit runs. Default NO ACTION matches V14 risk_state_history.
--     A future task may tighten this to RESTRICT if explicit preserve-on-delete is needed.

-- Out of scope (deferred to later tasks):
--   * chat_analysis_results table (DB-MVP §5.2) — separate task.
--   * risk_classifier_runs analog table for the RiskClassifierProvider
--     pipeline (T09) — separate task if approved.
--   * Retention / archival policy — TBD per expert approval.
--   * Polymorphic source design (source_type / source_id) — T04 only
--     supports chat analysis; future reuses require a new migration.

CREATE TABLE ai_analysis_runs (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    message_id          UUID            NOT NULL,
    user_id             UUID            NOT NULL,
    provider            VARCHAR(50)     NOT NULL,
    model               VARCHAR(100)    NOT NULL,
    prompt_version      VARCHAR(50)     NOT NULL,
    schema_version      VARCHAR(10)     NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    input_hash          VARCHAR(64)     NOT NULL,
    output_hash         VARCHAR(64)     NULL,
    error_code          VARCHAR(50)     NULL,
    error_summary       VARCHAR(200)    NULL,
    latency_ms          INTEGER         NOT NULL DEFAULT 0,
    input_tokens        BIGINT          NULL,
    output_tokens       BIGINT          NULL,
    model_risk_level    SMALLINT        NULL,
    confidence          NUMERIC(4, 3)   NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    started_at          TIMESTAMPTZ     NULL,
    completed_at        TIMESTAMPTZ     NULL,

    CONSTRAINT ai_analysis_runs_status_chk
        CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED')),
    CONSTRAINT ai_analysis_runs_input_hash_chk
        CHECK (input_hash ~ '^[a-f0-9]{64}$'),
    CONSTRAINT ai_analysis_runs_output_hash_chk
        CHECK (output_hash IS NULL OR output_hash ~ '^[a-f0-9]{64}$'),
    CONSTRAINT ai_analysis_runs_error_code_chk
        CHECK (error_code IS NULL OR error_code IN (
            'AI_PROVIDER_TIMEOUT',
            'AI_PROVIDER_UNAVAILABLE',
            'AI_ANALYSIS_OUTPUT_INVALID'
        )),
    CONSTRAINT ai_analysis_runs_error_summary_len_chk
        CHECK (error_summary IS NULL OR length(error_summary) <= 200),
    CONSTRAINT ai_analysis_runs_model_risk_level_chk
        CHECK (model_risk_level IS NULL OR model_risk_level BETWEEN 1 AND 4),
    CONSTRAINT ai_analysis_runs_confidence_chk
        CHECK (confidence IS NULL OR (confidence BETWEEN 0 AND 1)),
    CONSTRAINT ai_analysis_runs_latency_nonneg_chk
        CHECK (latency_ms >= 0),
    CONSTRAINT ai_analysis_runs_started_after_created_chk
        CHECK (started_at IS NULL OR started_at >= created_at),
    CONSTRAINT ai_analysis_runs_completed_after_started_chk
        CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at),
    CONSTRAINT ai_analysis_runs_terminal_chk
        CHECK (
            (status IN ('PENDING','RUNNING') AND completed_at IS NULL)
            OR (status IN ('SUCCEEDED','FAILED') AND completed_at IS NOT NULL)
        ),
    CONSTRAINT ai_analysis_runs_output_hash_required_on_success_chk
        CHECK (status <> 'SUCCEEDED' OR output_hash IS NOT NULL),
    CONSTRAINT ai_analysis_runs_error_required_on_failure_chk
        CHECK (status <> 'FAILED' OR error_code IS NOT NULL),
    CONSTRAINT ai_analysis_runs_message_fk
        FOREIGN KEY (message_id) REFERENCES conversation_messages(id)
);

-- Hot path: list runs for a single message (audit "why did this message get
-- this analysis?"). T04 service does not query this directly — it's the
-- repository hook for future REST consumers (T11+).
CREATE INDEX ai_analysis_runs_message_created_desc
    ON ai_analysis_runs (message_id, created_at DESC);

-- Ops dashboard: "show all FAILED runs from the last 24h".
CREATE INDEX ai_analysis_runs_status_created_desc
    ON ai_analysis_runs (status, created_at DESC);

-- Retention sweep: "find runs older than N days for archive / delete".
-- (No archival job ships in T04 — the index is here for a future task.)
CREATE INDEX ai_analysis_runs_created_at
    ON ai_analysis_runs (created_at);