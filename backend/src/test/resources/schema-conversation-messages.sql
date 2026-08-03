-- H2-compatible DDL for the conversation_messages table.
-- Used by integration tests when Flyway is disabled (test profile).
-- Must stay in sync with V5__create_conversation_messages.sql.

CREATE TABLE IF NOT EXISTS conversation_messages (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    session_id  VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    content     TEXT         NOT NULL,
    redacted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conversation_messages_session_created
    ON conversation_messages (session_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_conversation_messages_user_created
    ON conversation_messages (user_id, created_at ASC);
