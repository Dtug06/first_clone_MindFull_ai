-- H2-compatible DDL for the chat_analysis_results table.
-- Mirrors V16 migration. Used by integration tests when Flyway is
-- disabled (test profile).
--
-- Differences from PostgreSQL:
--   * JSONB -> VARCHAR(8192) in H2 (Hibernate handles JSON serialisation
--     via @JdbcTypeCode(SqlTypes.JSON) in the entity).
--   * Partial unique index not emulated here.
--   * FK to ai_analysis_runs and conversation_messages retained.

CREATE TABLE IF NOT EXISTS chat_analysis_results (
    id                      VARCHAR(36)                      NOT NULL PRIMARY KEY,
    analysis_run_id         VARCHAR(36)                      NOT NULL,
    conversation_message_id VARCHAR(36)                      NOT NULL,
    user_id                 VARCHAR(36)                      NOT NULL,
    topic                   VARCHAR(40)                      NOT NULL,
    emotion                 VARCHAR(20)                      NOT NULL,
    intent                  VARCHAR(20)                      NOT NULL,
    signals                 VARCHAR(8192)                    NOT NULL,
    evidence_spans          VARCHAR(8192)                    NOT NULL,
    model_risk_level        SMALLINT                        NOT NULL,
    confidence              NUMERIC(4, 3)                   NOT NULL,
    analysis_status         VARCHAR(20)                      NOT NULL,
    supersedes_id           VARCHAR(36),
    created_at              TIMESTAMP WITH TIME ZONE         NOT NULL
);

CREATE INDEX IF NOT EXISTS chat_analysis_results_user_created_desc
    ON chat_analysis_results (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS chat_analysis_results_supersedes_idx
    ON chat_analysis_results (supersedes_id);