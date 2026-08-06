-- H2-compatible DDL for the ai_analysis_runs table.
-- Mirrors V15 migration. Used by integration tests when Flyway is
-- disabled (test profile).
--
-- Differences from PostgreSQL (V15):
--   * TIMESTAMPTZ → TIMESTAMP WITH TIME ZONE (H2 supports tz-aware).
--   * Regex CHECK uses REGEXP_LIKE (H2 v1.4.200+) — same pattern as V15.
--   * UUID native type retained (H2 supports it; matches
--     schema-risk-state-history.sql and schema-safety-keyword-rules.sql).
--   * FK to conversation_messages retained. The FK target is the
--     conversation_messages table in V5; tests using this schema
--     must also load schema-conversation-messages.sql OR rely on
--     migration order — see SafetyResolverIntegrationTest for the
--     `@Sql(scripts = "/schema-...")` pattern.
--   * No DEFAULT gen_random_uuid() — H2 tests set id explicitly via the
--     entity factory.

CREATE TABLE IF NOT EXISTS ai_analysis_runs (
    id                  UUID                      NOT NULL PRIMARY KEY,
    message_id          UUID                      NOT NULL,
    user_id             UUID                      NOT NULL,
    provider            VARCHAR(50)               NOT NULL,
    model               VARCHAR(100)              NOT NULL,
    prompt_version      VARCHAR(50)               NOT NULL,
    schema_version      VARCHAR(10)               NOT NULL,
    status              VARCHAR(20)               NOT NULL,
    input_hash          VARCHAR(64)               NOT NULL,
    output_hash         VARCHAR(64)               NULL,
    error_code          VARCHAR(50)               NULL,
    error_summary       VARCHAR(200)              NULL,
    latency_ms          INTEGER                   NOT NULL DEFAULT 0,
    input_tokens        BIGINT                    NULL,
    output_tokens       BIGINT                    NULL,
    model_risk_level    SMALLINT                  NULL,
    confidence          NUMERIC(4, 3)             NULL,
    created_at          TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at          TIMESTAMP WITH TIME ZONE  NULL,
    completed_at        TIMESTAMP WITH TIME ZONE  NULL,

    CONSTRAINT ai_analysis_runs_status_chk
        CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED')),
    CONSTRAINT ai_analysis_runs_input_hash_chk
        CHECK (REGEXP_LIKE(input_hash, '^[a-f0-9]{64}$')),
    CONSTRAINT ai_analysis_runs_output_hash_chk
        CHECK (output_hash IS NULL OR REGEXP_LIKE(output_hash, '^[a-f0-9]{64}$')),
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

CREATE INDEX IF NOT EXISTS ai_analysis_runs_message_created_desc
    ON ai_analysis_runs (message_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ai_analysis_runs_status_created_desc
    ON ai_analysis_runs (status, created_at DESC);

CREATE INDEX IF NOT EXISTS ai_analysis_runs_created_at
    ON ai_analysis_runs (created_at);