-- V22 -- G4-T05: Job Runs and Job Run Item Logs (daily feature aggregation observability)
--
-- Two new tables for tracking scheduled + CLI/manual runs of the daily
-- feature aggregation job (G4-T05) and any future re-usable background
-- jobs (G4-T09+ user_behavior_profile_snapshots, G5 CBT reminders,
-- G6 Safety follow-ups).
--
-- Design rationale (G4-T05 Phase 1 plan, decisions confirmed by user):
--   * job_runs        : one row per job invocation (one schedule tick or
--                        one CLI execution). Holds counters + status.
--   * job_run_item_logs: one row per (job_run, user, date) the job tries
--                        to process. Holds per-user success/failure with
--                        error message.
--   * triggered_by enum captures the entry point (SCHEDULED vs CLI vs
--     MANUAL). Re-using the same table for future jobs means a single
--     audit timeline for ALL background work; the (job_name, started_at)
--     index supports both "latest run of X" and "history of runs of X".
--   * status enum is intentionally coarse (RUNNING / SUCCEEDED /
--     PARTIAL / FAILED). PARTIAL = some users failed, batch kept going
--     (the G4-T05 DoD §3 case). FAILED = all users in the batch failed.
--   * target_user_id + target_local_date allow CLI to target a specific
--     (user, date). Both NULL => batch run over all eligible users.
--   * users_attempted / users_succeeded / users_failed counters are
--     updated continuously as items complete (not just at job finish)
--     so a half-finished long-running job still reports useful progress
--     to operators watching via SELECT.
--
-- Why no audit_logs entry on every job run:
--   The job_runs table is THE log for job lifecycle; a parallel audit_logs
--   row would duplicate and complicate the timeline view. Operators look
--   at job_runs directly. audit_logs is reserved for security /
--   compliance events (auth / consent / safety). G2-T09 already codified
--   this separation.
--
-- Late-arriving data alignment with FEATURE_DICTIONARY section 9.2:
--   The job is idempotent: re-running for the same (user_id,
--   feature_date) uses ON CONFLICT (user_id, feature_date) DO UPDATE on
--   user_daily_features (T02+V21, target of T05). job_runs is NOT
--   idempotent (each invocation is a new row) -- operators want to see
--   the full history of attempts even when they all converge to the
--   same final feature row.
--
-- Out of scope (deferred to later tasks):
--   * Cross-job correlation id (request_id, parent_job_run_id). G4-T09+
--     will add if the project grows beyond 3 named jobs.
--   * Soft-delete / archival of old runs. Retention is a separate
--     retention task, not T05.
--   * Notifications (Slack/email) on FAILED status. Deferred to G4-T09+.
--
-- FK strategy mirrors V17/V20/V21:
--   * job_run_id FK with ON DELETE CASCADE (delete a job run -> delete
--     its item logs).
--   * target_user_id FK with ON DELETE CASCADE (delete a user -> their
--     job-run targeting rows are removed with them; matches V11 V14 V21
--     pattern). DELETED users are normally excluded from batch iteration
--     anyway (status != ACTIVE filter in the job query).
--
-- Indexes (hot paths):
--   * job_runs (job_name, started_at DESC): "latest run of X" + "history
--     of runs of X over last 7 days" for ops dashboards.
--   * job_run_item_logs (job_run_id): "why did this run produce N
--     failures" drill-down. Each run typically has <= ~1k items.

CREATE TABLE job_runs (
    id                      UUID            PRIMARY KEY,

    -- Identification
    job_name                VARCHAR(100)    NOT NULL,

    -- Entry point (per G4-T05 Phase 1 decision Q4)
    triggered_by            VARCHAR(20)     NOT NULL,

    -- Lifecycle status (allowed transitions enforced in service layer)
    status                  VARCHAR(20)     NOT NULL,

    -- Targeting (NULL = batch over all eligible users / dates)
    target_user_id          UUID            NULL
        REFERENCES users(id) ON DELETE CASCADE,
    target_local_date       DATE            NULL,
    target_date_from        DATE            NULL,
    target_date_to          DATE            NULL,

    -- Timing
    started_at              TIMESTAMPTZ     NOT NULL,
    finished_at             TIMESTAMPTZ     NULL,

    -- Counters (updated continuously as items complete)
    users_attempted         INTEGER         NOT NULL DEFAULT 0,
    users_succeeded         INTEGER         NOT NULL DEFAULT 0,
    users_failed            INTEGER         NOT NULL DEFAULT 0,

    -- Failure detail (concise + length-capped; per-user detail in job_run_item_logs)
    failure_summary_json    TEXT            NULL,
    failure_message         VARCHAR(1000)   NULL,

    -- Audit
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Enums
    CONSTRAINT job_runs_triggered_by_chk
        CHECK (triggered_by IN ('SCHEDULED', 'CLI', 'MANUAL')),
    CONSTRAINT job_runs_status_chk
        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED')),

    -- Counter sanity
    CONSTRAINT job_runs_counters_non_negative_chk
        CHECK (users_attempted >= 0
               AND users_succeeded >= 0
               AND users_failed >= 0),
    CONSTRAINT job_runs_succeeded_within_attempted_chk
        CHECK (users_succeeded + users_failed <= users_attempted),

    -- finished_at sync (RUNNING => NULL; terminal status => NOT NULL)
    CONSTRAINT job_runs_finished_at_chk
        CHECK ((status = 'RUNNING' AND finished_at IS NULL)
            OR (status IN ('SUCCEEDED', 'PARTIAL', 'FAILED') AND finished_at IS NOT NULL)),

    -- job_name cannot be blank
    CONSTRAINT job_runs_job_name_chk
        CHECK (job_name <> '')
);

CREATE INDEX idx_job_runs_job_name_started_desc
    ON job_runs (job_name, started_at DESC);

CREATE INDEX idx_job_runs_status_started_desc
    ON job_runs (status, started_at DESC);


CREATE TABLE job_run_item_logs (
    id                      UUID            PRIMARY KEY,

    -- Linkage (CASCADE: deleting a job run removes its item logs)
    job_run_id              UUID            NOT NULL
        REFERENCES job_runs(id) ON DELETE CASCADE,

    -- Item-level identification
    user_id                 UUID            NOT NULL,
    target_local_date       DATE            NOT NULL,

    -- Outcome
    status                  VARCHAR(20)     NOT NULL,

    -- Failure detail
    error_code              VARCHAR(50)     NULL,
    error_message           VARCHAR(1000)   NULL,

    -- Timing / observability
    duration_ms             INTEGER         NULL,

    -- Audit
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Enums
    CONSTRAINT job_run_item_logs_status_chk
        CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),

    -- positivity invariant
    CONSTRAINT job_run_item_logs_duration_non_negative_chk
        CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

CREATE INDEX idx_job_run_item_logs_job_run_id
    ON job_run_item_logs (job_run_id);

CREATE INDEX idx_job_run_item_logs_user_date
    ON job_run_item_logs (user_id, target_local_date);
