-- H2-compatible DDL for job_runs and job_run_item_logs tables.
-- Mirror of V22__create_job_runs.sql adapted for H2 test context.
--
-- H2 differences from PostgreSQL:
--   - TIMESTAMP WITH TIME ZONE -> TIMESTAMP WITH TIME ZONE (supported in H2 1.5+)
--   - No native UUID type in H2 1.x -> VARCHAR(36) used; in H2 2.x UUID is supported
--   - Foreign key to users table dropped (no users in H2 test schema)
--   - CHECK constraints preserved where H2 supports them
--   - IF NOT EXISTS guards on ALTER/CREATE statements for @SpringBootTest reuse

-- job_runs table
CREATE TABLE IF NOT EXISTS job_runs (
    id              VARCHAR(36)     NOT NULL,
    job_name        VARCHAR(100)    NOT NULL,
    triggered_by    VARCHAR(20)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    target_user_id  VARCHAR(36),
    target_local_date DATE,
    date_from       DATE,
    date_to         DATE,
    started_at      TIMESTAMP WITH TIME ZONE  NOT NULL,
    finished_at     TIMESTAMP WITH TIME ZONE,
    users_attempted INTEGER         DEFAULT 0,
    users_succeeded INTEGER         DEFAULT 0,
    users_failed    INTEGER         DEFAULT 0,
    failure_summary_json VARCHAR(1000),
    failure_message VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL,
    CONSTRAINT job_runs_pkey PRIMARY KEY (id),
    CONSTRAINT job_runs_triggered_by_check CHECK (triggered_by IN ('SCHEDULED', 'CLI', 'MANUAL')),
    CONSTRAINT job_runs_status_check CHECK (status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_job_runs_job_name ON job_runs(job_name);
CREATE INDEX IF NOT EXISTS idx_job_runs_status ON job_runs(status);
CREATE INDEX IF NOT EXISTS idx_job_runs_started_at ON job_runs(started_at);

-- job_run_item_logs table
CREATE TABLE IF NOT EXISTS job_run_item_logs (
    id              VARCHAR(36)     NOT NULL,
    job_run_id      VARCHAR(36)     NOT NULL,
    user_id         VARCHAR(36),
    target_local_date DATE,
    status          VARCHAR(20)     NOT NULL,
    error_code      VARCHAR(50),
    error_message   VARCHAR(500),
    duration_ms     INTEGER,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL,
    CONSTRAINT job_run_item_logs_pkey PRIMARY KEY (id),
    CONSTRAINT job_run_item_logs_status_check CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_job_run_item_logs_job_run_id ON job_run_item_logs(job_run_id);
CREATE INDEX IF NOT EXISTS idx_job_run_item_logs_user_id ON job_run_item_logs(user_id);
