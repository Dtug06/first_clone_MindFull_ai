-- H2-compatible DDL for the chat_sessions table.
-- Used by integration tests when Flyway is disabled (test profile).
-- Must stay in sync with V4__create_chat_sessions.sql.

CREATE TABLE IF NOT EXISTS chat_sessions (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    title       VARCHAR(200),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    started_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at   TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_updated
    ON chat_sessions (user_id, updated_at DESC);
