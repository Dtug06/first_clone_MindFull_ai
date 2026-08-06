-- H2-compatible DDL for DailyQuestionTemplate integration tests.
-- Must stay in sync with V6__create_daily_question_templates.sql
-- but uses H2-compatible types: VARCHAR(36) for UUID, TIMESTAMP for datetime.
-- Note: "value" column renamed to "option_value" because VALUE is a reserved keyword in H2.

CREATE TABLE IF NOT EXISTS daily_question_templates (
    id            VARCHAR(36)   NOT NULL PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL,
    version       INTEGER      NOT NULL,
    question_type VARCHAR(20)  NOT NULL,
    prompt        TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    scale_min     NUMERIC      NULL,
    scale_max     NUMERIC      NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT daily_question_templates_code_version_unique UNIQUE (code, version)
);

CREATE INDEX IF NOT EXISTS idx_dqt_code_status ON daily_question_templates (code, status);

CREATE TABLE IF NOT EXISTS daily_question_options (
    id            VARCHAR(36) NOT NULL PRIMARY KEY,
    template_id   VARCHAR(36) NOT NULL,
    option_value  VARCHAR(50) NOT NULL,
    label         TEXT        NOT NULL,
    order_index   INTEGER     NOT NULL,
    CONSTRAINT daily_question_options_template_value_unique UNIQUE (template_id, option_value),
    CONSTRAINT daily_question_options_template_fk
        FOREIGN KEY (template_id) REFERENCES daily_question_templates(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dqo_template_order ON daily_question_options (template_id, order_index ASC);
