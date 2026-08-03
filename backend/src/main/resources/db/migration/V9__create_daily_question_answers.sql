-- V9: Daily Question Answers
-- Scope: G2-T06 — submit answer for a daily question assignment.
--
-- One row per submission. Per G2-T06 §2.3 (plan option A) — immutable:
--   - The (assignment_id) UNIQUE constraint is the DB-level safety net that
--     prevents two answers for the same assignment.
--   - If a user submits twice, the DB rejects the second INSERT and the
--     service surfaces a 409 Conflict.
--
-- Column choices follow docs/02_DATABASE_MVP.md §4.6:
--   answer_type   : NUMERIC | TEXT | OPTION  (matches DailyAnswerType enum)
--   numeric_value : NUMERIC   — for SCALE / NUMBER questions
--   text_value    : TEXT      — for TEXT questions
--   option_value  : VARCHAR(50) — for SINGLE_CHOICE questions; stores the
--                   option's value string (e.g. "1", "2"), NOT a FK to
--                   daily_question_options.id. This keeps answers decoupled
--                   from option PKs across template versions.
--   metadata      : JSONB     — reserved for future fields (location, device, etc.)
--
-- CHECK constraint enforces exactly-one-value-per-type at the DB layer as a
-- last-line safety net. The service layer validates the same rule with
-- friendlier error messages for the client.

CREATE TABLE daily_question_answers (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    assignment_id  UUID         NOT NULL,
    user_id        UUID         NOT NULL,
    answer_type    VARCHAR(20)  NOT NULL,
    numeric_value  NUMERIC      NULL,
    text_value     TEXT         NULL,
    option_value   VARCHAR(50)  NULL,
    answered_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    metadata       JSONB        NULL,

    CONSTRAINT daily_question_answers_pkey PRIMARY KEY (id),
    CONSTRAINT daily_question_answers_assignment_fk
        FOREIGN KEY (assignment_id)
        REFERENCES daily_question_assignments(id) ON DELETE CASCADE,
    CONSTRAINT daily_question_answers_user_fk
        FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT daily_question_answers_assignment_unique
        UNIQUE (assignment_id),
    CONSTRAINT daily_question_answers_type_check
        CHECK (answer_type IN ('NUMERIC', 'TEXT', 'OPTION')),
    CONSTRAINT daily_question_answers_exactly_one_value_check
        CHECK (
            (answer_type = 'NUMERIC' AND numeric_value IS NOT NULL
                                    AND text_value IS NULL
                                    AND option_value IS NULL)
         OR (answer_type = 'TEXT'    AND text_value IS NOT NULL
                                    AND numeric_value IS NULL
                                    AND option_value IS NULL)
         OR (answer_type = 'OPTION'  AND option_value IS NOT NULL
                                    AND numeric_value IS NULL
                                    AND text_value IS NULL)
        )
);

-- History lookup: "user X's answers, newest first"
CREATE INDEX idx_dqans_user_answered_at ON daily_question_answers (user_id, answered_at DESC);

-- Reverse lookup: "which assignment is answered?"
-- (UNIQUE on assignment_id already provides a btree index)