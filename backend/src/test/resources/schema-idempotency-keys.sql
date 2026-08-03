-- H2-compatible DDL for idempotency_keys table.
-- Used by integration tests when Flyway is disabled (test profile).
-- Must stay in sync with V12__create_idempotency_keys.sql.

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(36)  NOT NULL,
    endpoint        VARCHAR(64)  NOT NULL,
    key_value       VARCHAR(64)  NOT NULL,
    response_status SMALLINT     NOT NULL,
    response_body   VARCHAR(4000) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP    NOT NULL,
    CONSTRAINT idempotency_keys_natural_key_unique UNIQUE (user_id, endpoint, key_value)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_user_endpoint_created
    ON idempotency_keys (user_id, endpoint, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at
    ON idempotency_keys (expires_at);