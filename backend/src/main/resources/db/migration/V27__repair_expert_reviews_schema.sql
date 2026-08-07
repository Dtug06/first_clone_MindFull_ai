-- Repair migration for databases where V20 was recorded as applied while its
-- SQL content was collapsed into a single-line comment and created no table.
-- Never edit V20 after it has been applied; this migration is additive.

CREATE TABLE IF NOT EXISTS expert_reviews (
    id              UUID         PRIMARY KEY,
    safety_event_id UUID         NOT NULL REFERENCES safety_events(id) ON DELETE CASCADE,
    reviewer_id     UUID         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    decision        VARCHAR(30)  NOT NULL,
    note            TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT expert_reviews_event_reviewer_uq
        UNIQUE (safety_event_id, reviewer_id),
    CONSTRAINT expert_reviews_decision_chk
        CHECK (decision IN (
            'CONFIRM_RISK',
            'DOWNGRADE_RISK',
            'ESCALATE',
            'NO_ACTION',
            'CONTINUE_MONITORING',
            'REQUEST_FOLLOWUP',
            'DISMISS'
        ))
);

CREATE INDEX IF NOT EXISTS expert_reviews_event_idx
    ON expert_reviews (safety_event_id);

CREATE INDEX IF NOT EXISTS expert_reviews_reviewer_idx
    ON expert_reviews (reviewer_id);

CREATE INDEX IF NOT EXISTS expert_reviews_created_at_idx
    ON expert_reviews (created_at);
