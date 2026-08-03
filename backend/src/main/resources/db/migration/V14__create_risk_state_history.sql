-- V14 — G3-T10: Risk state history (append-only)
--
-- Stores the resolved final risk level for a user after combining the
-- keyword/regex pre-filter signal (G3-T08), the LLM risk classifier
-- signal (G3-T09) and the user's current risk state. The Safety Resolver
-- (SafetyResolverService) writes exactly one row per resolution. Rows
-- are APPEND-ONLY — there is no update or delete path; the "current"
-- risk state is always the row with the latest occurred_at (tie-break
-- by id DESC). Schema mirrors DB-MVP §6.1 with the additional
-- audit columns (model_risk_level, rule_risk_level, current_risk_level,
-- confidence, source_type, source_id, rule_version,
-- model_version, prompt_version, schema_version) needed to reconstruct
-- every decision per docs/04_SAFETY_AND_CBT_RULES.md §5 ("Phải phân
-- biệt model_risk_level, rule_risk_level, final_risk_level").
--
-- The structured reason_codes column is mandatory per DB-MVP §6.1 and
-- docs/04 §7 (LLM Safety Output JSON has `reasonCodes: [...]`). It is
-- stored as JSONB in PostgreSQL so future consumers can group/filter by
-- code without re-parsing free text.
--
-- IMPORTANT invariants:
--   * Append-only — enforced at the application layer (no JPA setter
--     and no @PreUpdate/@PreRemove) and via the schema documented in
--     docs/04_SAFETY_AND_CBT_RULES.md §28 ("Safety history should be
--     append-only where practical").
--   * Scope: per user (Q1, G3-T10 Phase 1). The user_id FK ensures
--     ownership is derivable from the schema (DB-MVP §14).
--   * The final risk_level follows the rule:
--     final = max(ruleRisk, modelRisk)               -- max wins (Q2)
--     guard: if final < current, keep current         -- no auto-downgrade (Q3)
--   * reason_codes is a non-empty JSONB array combining the
--     classifier reasonCodes and any pre-filter rule code; downgrade
--     decisions add a sentinel `MANUAL_REVIEW_REQUIRED` code so audit
--     can distinguish max-wins vs guarded-downgrade.
--   * No seed rows — history begins empty per user; the first resolve
--     creates row #1 with current_risk_level = NULL.
--
-- Phase 3 review notes (documented here so future maintainers can
-- decide):
--   * ON DELETE CASCADE on user_id — if a user is hard-deleted, all
--     history rows vanish with them. This mildly conflicts with the
--     append-only invariant; documented as a known trade-off. If a
--     future task needs true append-only across user deletion, switch
--     to ON DELETE RESTRICT and rely on soft-delete via
--     users.status='DELETED' instead.
--   * No index on (user_id, source_type) — T11 (Safety Event) and
--     T13 (Expert Review) will query by source_type, so an additional
--     index should be added in a follow-up migration if query volume
--     justifies it. MVP scale (single-digit rows per user) does not
--     need it.

CREATE TABLE risk_state_history (
    id                  UUID            PRIMARY KEY,
    user_id             UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    risk_level          SMALLINT        NOT NULL,
    model_risk_level    SMALLINT        NULL,
    rule_risk_level     SMALLINT        NULL,
    current_risk_level  SMALLINT        NULL,
    source_type         VARCHAR(30)     NOT NULL,
    source_id           UUID            NULL,
    rule_version        VARCHAR(200)    NOT NULL,
    model_version       VARCHAR(100)    NULL,
    prompt_version      VARCHAR(50)     NULL,
    confidence          NUMERIC(4, 3)   NOT NULL,
    -- Per DB-MVP §6.1 and docs/04 §7, the structured reason codes are
    -- mandatory for audit. Stored as jsonb in PostgreSQL — a
    -- non-empty array. Combined from the classifier reasonCodes and
    -- any code produced by the pre-filter when a rule matched.
    reason_codes        JSONB           NOT NULL,
    occurred_at         TIMESTAMPTZ     NOT NULL,
    schema_version      VARCHAR(10)     NOT NULL DEFAULT 'V1',

    CONSTRAINT risk_state_history_risk_level        CHECK (risk_level BETWEEN 1 AND 4),
    CONSTRAINT risk_state_history_model_risk        CHECK (model_risk_level IS NULL OR model_risk_level BETWEEN 1 AND 4),
    CONSTRAINT risk_state_history_rule_risk         CHECK (rule_risk_level  IS NULL OR rule_risk_level  BETWEEN 1 AND 4),
    CONSTRAINT risk_state_history_current_risk      CHECK (current_risk_level IS NULL OR current_risk_level BETWEEN 1 AND 4),
    CONSTRAINT risk_state_history_confidence        CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT risk_state_history_source_type       CHECK (source_type IN ('KEYWORD_PRE_FILTER', 'LLM_CLASSIFIER', 'MANUAL_REVIEW'))
);

-- Hot path: SafetyResolverService.getCurrentRiskState(userId) and any
-- caller (e.g. matching G6) that needs "latest risk state for this user".
-- The secondary id DESC tie-break mirrors the G2 acceptance decision #2
-- fix (ConsentRepository timestamp tie-breaker) so behaviour stays
-- deterministic when two rows share an occurred_at (extremely rare but
-- possible in high-throughput tests).
CREATE INDEX risk_state_history_user_occurred_desc
    ON risk_state_history (user_id, occurred_at DESC, id DESC);

-- Secondary index for "all history rows for this user at risk level L"
-- (e.g. trend views, audit queries). Not a hot path; created because
-- the column is frequently filtered in support tooling.
CREATE INDEX risk_state_history_user_level_occurred_desc
    ON risk_state_history (user_id, risk_level, occurred_at DESC);
