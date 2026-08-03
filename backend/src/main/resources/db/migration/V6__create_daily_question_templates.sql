-- V6: Daily Question Template Catalog
-- Scope: template + options tables + seed MVP data (5 questions)
-- Versioning: admin update creates new version row; old approved row → RETIRED

CREATE TABLE daily_question_templates (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    code          VARCHAR(50) NOT NULL,
    version       INTEGER     NOT NULL DEFAULT 1,
    question_type VARCHAR(20) NOT NULL,
    prompt        TEXT        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT daily_question_templates_pkey PRIMARY KEY (id),
    CONSTRAINT daily_question_templates_code_version_unique UNIQUE (code, version)
);

CREATE INDEX idx_dqt_code_status ON daily_question_templates (code, status);
CREATE INDEX idx_dqt_status    ON daily_question_templates (status);

CREATE TABLE daily_question_options (
    id          UUID    NOT NULL DEFAULT gen_random_uuid(),
    template_id UUID    NOT NULL,
    value       VARCHAR(50) NOT NULL,
    label       TEXT        NOT NULL,
    order_index INTEGER     NOT NULL,

    CONSTRAINT daily_question_options_pkey PRIMARY KEY (id),
    CONSTRAINT daily_question_options_template_value_unique UNIQUE (template_id, value),
    CONSTRAINT daily_question_options_template_fk
        FOREIGN KEY (template_id)
        REFERENCES daily_question_templates(id) ON DELETE CASCADE
);

CREATE INDEX idx_dqo_template_order ON daily_question_options (template_id, order_index ASC);

-- =============================================================================
-- Seed: 5 MVP templates (all APPROVED, version 1 — immediately usable)
-- =============================================================================

-- STRESS: SCALE 1-5
INSERT INTO daily_question_templates (id, code, version, question_type, prompt, status)
VALUES ('00000000-0000-0000-0000-000000000001',
        'STRESS', 1, 'SCALE',
        'Hôm nay bạn cảm thấy mức stress của mình như thế nào?',
        'APPROVED');

-- MOOD: SINGLE_CHOICE with 5 options
INSERT INTO daily_question_templates (id, code, version, question_type, prompt, status)
VALUES ('00000000-0000-0000-0000-000000000002',
        'MOOD', 1, 'SINGLE_CHOICE',
        'Tâm trạng hôm nay của bạn như thế nào?',
        'APPROVED');

INSERT INTO daily_question_options (template_id, value, label, order_index)
VALUES
    ('00000000-0000-0000-0000-000000000002', '1', 'Rất tệ',   1),
    ('00000000-0000-0000-0000-000000000002', '2', 'Tệ',       2),
    ('00000000-0000-0000-0000-000000000002', '3', 'Bình thường', 3),
    ('00000000-0000-0000-0000-000000000002', '4', 'Tốt',      4),
    ('00000000-0000-0000-0000-000000000002', '5', 'Rất tốt',  5);

-- SLEEP: NUMBER (hours)
INSERT INTO daily_question_templates (id, code, version, question_type, prompt, status)
VALUES ('00000000-0000-0000-0000-000000000003',
        'SLEEP', 1, 'NUMBER',
        'Bạn ngủ bao nhiêu giờ đêm qua?',
        'APPROVED');

-- ENERGY: SCALE 1-5
INSERT INTO daily_question_templates (id, code, version, question_type, prompt, status)
VALUES ('00000000-0000-0000-0000-000000000004',
        'ENERGY', 1, 'SCALE',
        'Mức năng lượng của bạn hôm nay như thế nào?',
        'APPROVED');

-- OPEN: TEXT (free text)
INSERT INTO daily_question_templates (id, code, version, question_type, prompt, status)
VALUES ('00000000-0000-0000-0000-000000000005',
        'OPEN', 1, 'TEXT',
        'Có điều gì bạn muốn chia sẻ hôm nay không?',
        'APPROVED');
