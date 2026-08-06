-- V5__create_conversation_messages.sql
-- MindBridge AI — G2-T02
-- Stores raw conversation messages per session.
-- Rules:
-- - Message must belong to a valid session.
-- - Session must belong to the user (enforced at service level, not DB level).
-- - redacted flag: T02 creates the column; actual redaction logic is G2-T03 scope.
-- - NO emotion/risk/conclusion AI columns (G2-T02 scope boundary).

CREATE TABLE conversation_messages (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    session_id  UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    role        VARCHAR(20) NOT NULL,
    content     TEXT        NOT NULL,
    redacted    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT conversation_messages_pkey       PRIMARY KEY (id),
    CONSTRAINT conversation_messages_session_fk FOREIGN KEY (session_id) REFERENCES chat_sessions(id),
    CONSTRAINT conversation_messages_user_fk    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT conversation_messages_role_check CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

-- Primary read path: list messages for a session ordered by creation time.
CREATE INDEX idx_conversation_messages_session_created
    ON conversation_messages (session_id, created_at ASC);

-- Secondary path: list messages for a user (e.g. user history).
CREATE INDEX idx_conversation_messages_user_created
    ON conversation_messages (user_id, created_at ASC);
