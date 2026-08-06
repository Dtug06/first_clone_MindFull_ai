-- H2-compatible DDL for the daily_question_assignments table.
-- Used by integration tests when Flyway is disabled (test profile).
-- Must stay in sync with V8__create_daily_question_assignments.sql.

CREATE TABLE IF NOT EXISTS daily_question_assignments (
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id             VARCHAR(36)  NOT NULL,
    template_version_id VARCHAR(36)  NOT NULL,
    template_code       VARCHAR(50)  NOT NULL,
    assigned_for_date   DATE         NOT NULL,
    timezone            VARCHAR(50)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ASSIGNED',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT daily_question_assignments_unique
        UNIQUE (user_id, template_version_id, assigned_for_date),
    CONSTRAINT daily_question_assignments_status_check
        CHECK (status IN ('ASSIGNED', 'ANSWERED', 'SKIPPED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_dqa_user_date ON daily_question_assignments (user_id, assigned_for_date);
CREATE INDEX IF NOT EXISTS idx_dqa_template_version ON daily_question_assignments (template_version_id);
CREATE INDEX IF NOT EXISTS idx_dqa_user_code_date ON daily_question_assignments (user_id, template_code, assigned_for_date);
