-- V21 — G4-T02: User Daily Features (typed feature store, single-row-per-day)
--
-- One row per (user_id, feature_date). Holds the daily aggregate for the
-- 8 MVP features defined in docs/analysis/FEATURE_DICTIONARY_v1.md §5,
-- plus the raw inputs and companion metadata required by §3 (Explicit /
-- Inferred / Behavioral classification) and §6 (per-feature specs).
--
-- Design rationale (G4-T02 Phase 1 plan, decisions confirmed by user):
--   * UNIQUE (user_id, feature_date) — single-row-per-day (plan option B).
--     Late-arriving data idempotency per FEATURE_DICTIONARY §9.2 is met
--     by UPSERT semantics at the application layer (G4-T04+); the DB
--     layer guarantees no two rows exist for the same calendar day.
--   * Each derived feature value is stored as a TYPED column (NUMERIC,
--     SMALLINT, etc.) so the 7/30-day dashboard query (DoD §1) never
--     has to read extra_features JSONB for any of the 8 catalog
--     features. JSONB is reserved for experimental fields that are not
--     part of the dashboard contract (rule 30-database-ai-safety.mdc §3:
--     "Do not use JSONB as a replacement for every typed field").
--   * feature_version records the FEATURE_DICTIONARY schema version
--     ('feature_dictionary_v1'); calculation_version records a composite
--     of the 5 per-feature calculation versions in use (see
--     FEATURE_DICTIONARY §2.2). Each derived feature also carries its
--     own per-feature <feature>_calculation_version companion column
--     so audit / recompute can pinpoint exactly which formula produced
--     each value.
--   * explicit_coverage / inferred_confidence follow DB-MVP §7.1
--     literally. explicit_coverage is 0.000 / 1.000 for the 4 explicit
--     features in MVP (no partial-coverage semantics until a future
--     task adds it); inferred_confidence is MAX(confidence) of the
--     contributing chat_analysis_results rows for anxiety_signal.
--   * timezone mirrors users.timezone at the moment the row was
--     computed (FEATURE_DICTIONARY §8.7 — keep the TZ value that was
--     actually used so future timezone changes do not silently
--     rewrite history).
--   * max_risk_level is NEVER defaulted to 1 when missing
--     (FEATURE_DICTIONARY §6.8.4 mandatory rule). NULL means UNKNOWN,
--     not "level 1 = normal".
--
-- Out of scope (G4-T02 = schema only, no JPA entity / service):
--   * The 8 feature calculators (stress / mood / energy / sleep /
--     anxiety_signal / engagement / exercise_completion / max_risk)
--     live in G4-T04 and later; this migration only persists the
--     shape of the result.
--   * Recompute / idempotency logic lives in G4-T04+.
--   * Window aggregations (7d / 30d) live in G4-T06.
--   * user_behavior_profile_snapshots (§7.3) is the audit-only table
--     that preserves calculation_version history; G4-T09+.
--
-- Idempotency (DoD §2):
--   UNIQUE (user_id, feature_date) prevents the application from
--   accidentally inserting a second row for the same calendar day. A
--   recompute MUST use UPSERT (ON CONFLICT ... DO UPDATE) — see
--   G4-T04 plan.
--
-- Migration from empty DB:
--   Verified at Phase 2 by G4-T02_JdbcTest running the same DDL
--   against an in-memory H2 mirror (schema-user-daily-features.sql)
--   with the FK to users dropped (per project convention; see
--   schema-safety-events.sql header). Flyway-disabled test profile
--   uses the H2 mirror; production runs this migration against
--   PostgreSQL 17 via Flyway.
--
-- CHECK inventory (every value-range constraint from
-- FEATURE_DICTIONARY §6 is materialised):
--   * stress       : stress_score [0,1],  stress_raw_value [1,5]
--   * mood         : mood_score   [0,1],  mood_raw_value IN {1..5}
--   * energy       : energy_score [0,1],  energy_raw_value [1,5]
--   * sleep        : sleep_hours  [0,24], sleep_quality_raw [1,5],
--                    sleep_score  [0,1]
--   * anxiety      : anxiety_signal [0,1], anxiety_signal_confidence [0,1],
--                    anxiety_signal_source IN {CHAT_ANALYSIS,
--                    KEYWORD_PRE_FILTER, COMBINED, NONE}
--   * engagement   : engagement_score [0,1], message_count >= 0,
--                    active_chat_session_count >= 0,
--                    checkin_assigned_count >= 0,
--                    checkin_completed_count >= 0,
--                    checkin_completion_ratio [0,1] OR NULL
--   * exercise     : exercise_completion_ratio [0,1] OR NULL
--                    (G5 not shipped yet; always NULL MVP)
--   * max_risk     : max_risk_level [1,4], risk_event_count >= 0
--   * metadata     : coverage [0,1], confidence [0,1],
--                    timezone non-blank,
--                    feature_date <= CURRENT_DATE,
--                    feature_date >= DATE '2000-01-01' (sanity floor),
--                    feature_version / calculation_version non-blank
--
-- FK policy:
--   user_id REFERENCES users(id) ON DELETE CASCADE — mirrors V11
--   behavioral_events and V14 risk_state_history. Same trade-off:
--   hard-deleting a user removes their feature history; documented in
--   V14 header as "mild conflict with append-only invariant, accepted
--   because soft-delete via users.status='DELETED' is the planned
--   retention path".
--
-- Indexes (hot paths):
--   * user_id + feature_date DESC : the 7d/30d dashboard query
--     (DoD §1) and the UPSERT key. Composite descending because the
--     dashboard always wants "latest N days first".

CREATE TABLE user_daily_features (
    id                          UUID            PRIMARY KEY,

    -- Linkage
    user_id                     UUID            NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,
    feature_date                DATE            NOT NULL,
    timezone                    VARCHAR(50)     NOT NULL,

    -- stress (explicit, FEATURE_DICTIONARY §6.1)
    stress_score                NUMERIC(4, 3)   NULL,
    stress_raw_value            NUMERIC         NULL,
    stress_score_calculation_version  VARCHAR(50) NULL,

    -- mood (explicit, §6.2)
    mood_score                  NUMERIC(4, 3)   NULL,
    mood_raw_value              VARCHAR(50)     NULL,
    mood_score_calculation_version    VARCHAR(50) NULL,

    -- energy (explicit, §6.3)
    energy_score                NUMERIC(4, 3)   NULL,
    energy_raw_value            NUMERIC         NULL,
    energy_score_calculation_version  VARCHAR(50) NULL,

    -- sleep (explicit duration only MVP, §6.4)
    sleep_hours                 NUMERIC(4, 2)   NULL,
    sleep_quality_raw           SMALLINT        NULL,
    sleep_score                 NUMERIC(4, 3)   NULL,
    sleep_score_calculation_version   VARCHAR(50) NULL,

    -- anxiety_signal (inferred, §6.5)
    anxiety_signal              NUMERIC(4, 3)   NULL,
    anxiety_signal_confidence   NUMERIC(4, 3)   NULL,
    anxiety_signal_source       VARCHAR(20)     NULL,
    anxiety_signal_calculation_version  VARCHAR(50) NULL,
    anxiety_analysis_result_id  UUID            NULL,

    -- engagement (behavioral, §6.6)
    engagement_score            NUMERIC(4, 3)   NULL,
    message_count               INTEGER         NULL,
    active_chat_session_count   INTEGER         NULL,
    checkin_assigned_count      INTEGER         NULL,
    checkin_completed_count     INTEGER         NULL,
    checkin_completion_ratio    NUMERIC(4, 3)   NULL,
    engagement_score_calculation_version  VARCHAR(50) NULL,

    -- exercise_completion (behavioral, FUTURE G5, §6.7)
    exercise_completion_ratio   NUMERIC(5, 4)   NULL,
    exercise_completion_calculation_version  VARCHAR(50) NULL,

    -- max_risk (safety-derived observable, §6.8)
    max_risk_level              SMALLINT        NULL,
    risk_event_count            INTEGER         NULL,
    max_risk_calculation_version  VARCHAR(50)   NULL,

    -- Coverage / confidence summary (DB-MVP §7.1)
    explicit_coverage           NUMERIC(4, 3)   NULL,
    inferred_confidence         NUMERIC(4, 3)   NULL,

    -- Versioning + extension bucket
    feature_version             VARCHAR(50)     NOT NULL DEFAULT 'feature_dictionary_v1',
    calculation_version         VARCHAR(200)    NOT NULL,
    extra_features              JSONB           NULL,

    -- Audit
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- DoD §2: no two rows for the same calendar day (single-row-per-day).
    CONSTRAINT user_daily_features_user_date_unique
        UNIQUE (user_id, feature_date),

    -- Range / domain checks for the 8 catalog features
    CONSTRAINT user_daily_features_stress_score_chk
        CHECK (stress_score IS NULL OR stress_score BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_stress_raw_value_chk
        CHECK (stress_raw_value IS NULL OR stress_raw_value BETWEEN 1 AND 5),
    CONSTRAINT user_daily_features_mood_score_chk
        CHECK (mood_score IS NULL OR mood_score BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_mood_raw_value_chk
        CHECK (mood_raw_value IS NULL OR mood_raw_value IN ('1','2','3','4','5')),
    CONSTRAINT user_daily_features_energy_score_chk
        CHECK (energy_score IS NULL OR energy_score BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_energy_raw_value_chk
        CHECK (energy_raw_value IS NULL OR energy_raw_value BETWEEN 1 AND 5),
    CONSTRAINT user_daily_features_sleep_hours_chk
        CHECK (sleep_hours IS NULL OR (sleep_hours >= 0 AND sleep_hours <= 24)),
    CONSTRAINT user_daily_features_sleep_quality_raw_chk
        CHECK (sleep_quality_raw IS NULL OR sleep_quality_raw BETWEEN 1 AND 5),
    CONSTRAINT user_daily_features_sleep_score_chk
        CHECK (sleep_score IS NULL OR sleep_score BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_anxiety_signal_chk
        CHECK (anxiety_signal IS NULL OR anxiety_signal BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_anxiety_signal_confidence_chk
        CHECK (anxiety_signal_confidence IS NULL OR anxiety_signal_confidence BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_anxiety_signal_source_chk
        CHECK (anxiety_signal_source IS NULL OR anxiety_signal_source IN
               ('CHAT_ANALYSIS','KEYWORD_PRE_FILTER','COMBINED','NONE')),
    CONSTRAINT user_daily_features_engagement_score_chk
        CHECK (engagement_score IS NULL OR engagement_score BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_message_count_chk
        CHECK (message_count IS NULL OR message_count >= 0),
    CONSTRAINT user_daily_features_active_chat_session_count_chk
        CHECK (active_chat_session_count IS NULL OR active_chat_session_count >= 0),
    CONSTRAINT user_daily_features_checkin_assigned_count_chk
        CHECK (checkin_assigned_count IS NULL OR checkin_assigned_count >= 0),
    CONSTRAINT user_daily_features_checkin_completed_count_chk
        CHECK (checkin_completed_count IS NULL OR checkin_completed_count >= 0),
    CONSTRAINT user_daily_features_checkin_completion_ratio_chk
        CHECK (checkin_completion_ratio IS NULL OR checkin_completion_ratio BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_exercise_completion_ratio_chk
        CHECK (exercise_completion_ratio IS NULL OR exercise_completion_ratio BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_max_risk_level_chk
        CHECK (max_risk_level IS NULL OR max_risk_level BETWEEN 1 AND 4),
    CONSTRAINT user_daily_features_risk_event_count_chk
        CHECK (risk_event_count IS NULL OR risk_event_count >= 0),

    -- Coverage / confidence summary constraints
    CONSTRAINT user_daily_features_explicit_coverage_chk
        CHECK (explicit_coverage IS NULL OR explicit_coverage BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_inferred_confidence_chk
        CHECK (inferred_confidence IS NULL OR inferred_confidence BETWEEN 0 AND 1),

    -- Timezone + date sanity
    CONSTRAINT user_daily_features_timezone_chk
        CHECK (timezone <> ''),
    CONSTRAINT user_daily_features_feature_date_chk
        CHECK (feature_date <= CURRENT_DATE AND feature_date >= DATE '2000-01-01'),

    -- Version strings must be non-blank (DB-MVP §7.1 lists both as required)
    CONSTRAINT user_daily_features_feature_version_chk
        CHECK (feature_version <> ''),
    CONSTRAINT user_daily_features_calculation_version_chk
        CHECK (calculation_version <> '')
);

-- DoD §1 hot path: 7d/30d dashboard query orders by feature_date DESC
-- and never touches extra_features. Composite index covers WHERE
-- user_id = ? AND feature_date BETWEEN ? AND ? ORDER BY feature_date DESC.
CREATE INDEX idx_user_daily_features_user_date_desc
    ON user_daily_features (user_id, feature_date DESC);