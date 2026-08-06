-- H2-compatible DDL for the risk_state_history table.
-- Mirrors V14 migration. Used by integration tests when Flyway is
-- disabled (test profile).
--
-- Differences from PostgreSQL:
--   * TIMESTAMPTZ maps to TIMESTAMP WITH TIME ZONE in H2.
--   * JSONB maps to VARCHAR(8192) in H2 (we serialise/deserialise the
--     String[] in Java code via Hibernate's @JdbcTypeCode(SqlTypes.JSON)).
--     Hibernate 6 writes the JSON text; we read it back the same way.
--     Length 8192 is generous — even 50 reason codes fit.
--   * Partial unique indexes (WHERE status = 'APPROVED') are not used
--     here — risk_state_history has no equivalent partial constraint,
--     only plain composite indexes.
--   * The CHECK constraints on source_type mirror V14 verbatim — same
--     set of allowed source labels.

CREATE TABLE IF NOT EXISTS risk_state_history (
    id                  UUID            NOT NULL PRIMARY KEY,
    user_id             UUID            NOT NULL,
    risk_level          SMALLINT        NOT NULL,
    model_risk_level    SMALLINT        NULL,
    rule_risk_level     SMALLINT        NULL,
    current_risk_level  SMALLINT        NULL,
    source_type         VARCHAR(30)     NOT NULL,
    source_id           UUID            NULL,
    rule_version        VARCHAR(200)    NOT NULL,
    model_version       VARCHAR(100)    NULL,
    prompt_version      VARCHAR(50)     NULL,
    confidence          NUMERIC(4, 3)   NOT NULL,
    reason_codes        VARCHAR(8192)   NOT NULL,
    occurred_at         TIMESTAMP       NOT NULL,
    schema_version      VARCHAR(10)     NOT NULL DEFAULT 'V1',

    CONSTRAINT risk_state_history_risk_level        CHECK (risk_level BETWEEN 1 AND 4),
    CONSTRAINT risk_state_history_model_risk        CHECK (model_risk_level IS NULL OR model_risk_level BETWEEN 1 AND 4),
    CONSTRAINT risk_state_history_rule_risk         CHECK (rule_risk_level  IS NULL OR rule_risk_level  BETWEEN 1 AND 4),
    CONSTRAINT risk_state_history_current_risk      CHECK (current_risk_level IS NULL OR current_risk_level BETWEEN 1 AND 4),
    CONSTRAINT risk_state_history_confidence        CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT risk_state_history_source_type       CHECK (source_type IN ('KEYWORD_PRE_FILTER', 'LLM_CLASSIFIER', 'MANUAL_REVIEW'))
);

CREATE INDEX IF NOT EXISTS risk_state_history_user_occurred_desc
    ON risk_state_history (user_id, occurred_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS risk_state_history_user_level_occurred_desc
    ON risk_state_history (user_id, risk_level, occurred_at DESC);
