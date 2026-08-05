-- H2-compatible DDL for the user_daily_features table.
-- Mirrors V21 migration. Used by integration tests when Flyway is
-- disabled (test profile).
--
-- Differences from PostgreSQL (V21):
--   * JSONB -> VARCHAR(8192). The Phase-2 JdbcTest does not exercise
--     the JSONB column end-to-end (no JPA entity yet); we keep a
--     VARCHAR mirror so the column exists and is writable in the test
--     schema. Hibernate handles JSONB serialization via
--     @JdbcTypeCode(SqlTypes.JSON) in the future entity (G4-T04+).
--   * FK to users is dropped here, mirroring schema-safety-events.sql
--     and schema-risk-state-history.sql. Ownership is enforced at the
--     application layer (G4-T04+); the test sandbox does not need a
--     FK because no users table is loaded for this test.
--   * CURRENT_DATE / NOW() checks are retained verbatim — H2 supports
--     both expressions.
--   * IF NOT EXISTS guards throughout, because the H2 in-memory DB
--     persists across tests in the same JVM (DB_CLOSE_DELAY=-1).

CREATE TABLE IF NOT EXISTS user_daily_features (
    id                          UUID            NOT NULL PRIMARY KEY,

    -- Linkage (no FK in test mirror; see header)
    user_id                     UUID            NOT NULL,
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
    extra_features              VARCHAR(8192)   NULL,

    -- Audit
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- DoD §2: single-row-per-day (mirrors V21 unique constraint)
    CONSTRAINT user_daily_features_user_date_unique
        UNIQUE (user_id, feature_date),

    -- Range / domain checks (mirrors V21)
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
    CONSTRAINT user_daily_features_explicit_coverage_chk
        CHECK (explicit_coverage IS NULL OR explicit_coverage BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_inferred_confidence_chk
        CHECK (inferred_confidence IS NULL OR inferred_confidence BETWEEN 0 AND 1),
    CONSTRAINT user_daily_features_timezone_chk
        CHECK (timezone <> ''),
    CONSTRAINT user_daily_features_feature_date_chk
        CHECK (feature_date <= CURRENT_DATE AND feature_date >= DATE '2000-01-01'),
    CONSTRAINT user_daily_features_feature_version_chk
        CHECK (feature_version <> ''),
    CONSTRAINT user_daily_features_calculation_version_chk
        CHECK (calculation_version <> '')
);

CREATE INDEX IF NOT EXISTS idx_user_daily_features_user_date_desc
    ON user_daily_features (user_id, feature_date DESC);