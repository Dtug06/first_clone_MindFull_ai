-- H2-compatible DDL for the daily_question_answers table.
-- Used by integration tests when Flyway is disabled (test profile).
-- Must stay in sync with V9__create_daily_question_answers.sql.

CREATE TABLE IF NOT EXISTS daily_question_answers (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    assignment_id  VARCHAR(36)  NOT NULL,
    user_id        VARCHAR(36)  NOT NULL,
    answer_type    VARCHAR(20)  NOT NULL,
    numeric_value  NUMERIC      NULL,
    text_value     TEXT         NULL,
    option_value   VARCHAR(50)  NULL,
    answered_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata       VARCHAR(4000) NULL,
    CONSTRAINT daily_question_answers_assignment_unique UNIQUE (assignment_id),
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

CREATE INDEX IF NOT EXISTS idx_dqans_user_answered_at ON daily_question_answers (user_id, answered_at DESC);