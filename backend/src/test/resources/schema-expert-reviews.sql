-- H2-compatible DDL for expert_reviews. Mirrors V20.
-- Production keeps foreign keys; the isolated H2 fixture verifies service logic.

CREATE TABLE IF NOT EXISTS expert_reviews (
    id UUID NOT NULL PRIMARY KEY,
    safety_event_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    decision VARCHAR(30) NOT NULL,
    note CLOB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

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

-- @Sql executes this fixture before every nested test method. Keep each test
-- isolated so pagination and review-count assertions do not depend on order.
DELETE FROM expert_reviews;
DELETE FROM safety_event_sources;
DELETE FROM safety_actions;
DELETE FROM safety_events;
