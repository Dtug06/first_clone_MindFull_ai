package com.mindbridge.behavior.feature.job.persistence;

import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDailyFeatureUpsertServiceImpl implements UserDailyFeatureUpsertService {
    private static final Logger log = LoggerFactory.getLogger(UserDailyFeatureUpsertServiceImpl.class);
    private final DbDialect dialect;

    @PersistenceContext
    private EntityManager entityManager;

    public UserDailyFeatureUpsertServiceImpl(Environment environment) {
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        this.dialect = DbDialect.fromJdbcUrl(jdbcUrl);
        if (dialect == DbDialect.UNKNOWN) {
            throw new IllegalStateException(
                    "Unsupported or missing spring.datasource.url for G4 feature upsert");
        }
        log.info("G4-T05 Using database dialect={}", dialect);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID upsert(UserDailyFeature row) {
        if (row == null) throw new IllegalArgumentException("row must not be null");
        if (row.getUserId() == null || row.getFeatureDate() == null)
            throw new IllegalArgumentException("userId and featureDate must be set");
        return dialect == DbDialect.POSTGRESQL ? upsertPg(row) : upsertH2(row);
    }

    private UUID upsertPg(UserDailyFeature r) {
        String sql =
            "INSERT INTO user_daily_features (" +
                "id, user_id, feature_date, timezone," +
                "stress_score, stress_raw_value, stress_score_calculation_version," +
                "mood_score, mood_raw_value, mood_score_calculation_version," +
                "energy_score, energy_raw_value, energy_score_calculation_version," +
                "sleep_hours, sleep_quality_raw, sleep_score, sleep_score_calculation_version," +
                "anxiety_signal, anxiety_signal_confidence, anxiety_signal_calculation_version, anxiety_signal_source," +
                "anxiety_analysis_result_id," +
                "engagement_score, message_count, active_chat_session_count," +
                "checkin_assigned_count, checkin_completed_count, checkin_completion_ratio," +
                "engagement_score_calculation_version," +
                "exercise_completion_ratio, exercise_completion_calculation_version," +
                "max_risk_level, risk_event_count, max_risk_calculation_version," +
                "explicit_coverage, inferred_confidence," +
                "feature_version, calculation_version, extra_features, created_at" +
            ") VALUES (" +
                ":id, :userId, :featureDate, :timezone," +
                ":stressScore, :stressRawValue, :stressScoreCalculationVersion," +
                ":moodScore, :moodRawValue, :moodScoreCalculationVersion," +
                ":energyScore, :energyRawValue, :energyScoreCalculationVersion," +
                ":sleepHours, :sleepQualityRaw, :sleepScore, :sleepScoreCalculationVersion," +
                ":anxietySignal, :anxietySignalConfidence, :anxietySignalCalculationVersion, :anxietySignalSource," +
                ":anxietyAnalysisResultId," +
                ":engagementScore, :messageCount, :activeChatSessionCount," +
                ":checkinAssignedCount, :checkinCompletedCount, :checkinCompletionRatio," +
                ":engagementScoreCalculationVersion," +
                ":exerciseCompletionRatio, :exerciseCompletionCalculationVersion," +
                ":maxRiskLevel, :riskEventCount, :maxRiskCalculationVersion," +
                ":explicitCoverage, :inferredConfidence," +
                ":featureVersion, :calculationVersion, CAST(:extraFeatures AS jsonb), :createdAt" +
            ") ON CONFLICT (user_id, feature_date) DO UPDATE SET" +
                " stress_score = EXCLUDED.stress_score," +
                " stress_raw_value = EXCLUDED.stress_raw_value," +
                " stress_score_calculation_version = EXCLUDED.stress_score_calculation_version," +
                " mood_score = EXCLUDED.mood_score," +
                " mood_raw_value = EXCLUDED.mood_raw_value," +
                " mood_score_calculation_version = EXCLUDED.mood_score_calculation_version," +
                " energy_score = EXCLUDED.energy_score," +
                " energy_raw_value = EXCLUDED.energy_raw_value," +
                " energy_score_calculation_version = EXCLUDED.energy_score_calculation_version," +
                " sleep_hours = EXCLUDED.sleep_hours," +
                " sleep_quality_raw = EXCLUDED.sleep_quality_raw," +
                " sleep_score = EXCLUDED.sleep_score," +
                " sleep_score_calculation_version = EXCLUDED.sleep_score_calculation_version," +
                " anxiety_signal = EXCLUDED.anxiety_signal," +
                " anxiety_signal_confidence = EXCLUDED.anxiety_signal_confidence," +
                " anxiety_signal_calculation_version = EXCLUDED.anxiety_signal_calculation_version," +
                " anxiety_signal_source = EXCLUDED.anxiety_signal_source," +
                " anxiety_analysis_result_id = EXCLUDED.anxiety_analysis_result_id," +
                " engagement_score = EXCLUDED.engagement_score," +
                " message_count = EXCLUDED.message_count," +
                " active_chat_session_count = EXCLUDED.active_chat_session_count," +
                " checkin_assigned_count = EXCLUDED.checkin_assigned_count," +
                " checkin_completed_count = EXCLUDED.checkin_completed_count," +
                " checkin_completion_ratio = EXCLUDED.checkin_completion_ratio," +
                " engagement_score_calculation_version = EXCLUDED.engagement_score_calculation_version," +
                " exercise_completion_ratio = EXCLUDED.exercise_completion_ratio," +
                " exercise_completion_calculation_version = EXCLUDED.exercise_completion_calculation_version," +
                " max_risk_level = EXCLUDED.max_risk_level," +
                " risk_event_count = EXCLUDED.risk_event_count," +
                " max_risk_calculation_version = EXCLUDED.max_risk_calculation_version," +
                " explicit_coverage = EXCLUDED.explicit_coverage," +
                " inferred_confidence = EXCLUDED.inferred_confidence," +
                " feature_version = EXCLUDED.feature_version," +
                " calculation_version = EXCLUDED.calculation_version," +
                " extra_features = EXCLUDED.extra_features";
        entityManager.createNativeQuery(sql)
            .setParameter("id", r.getId())
            .setParameter("userId", r.getUserId())
            .setParameter("featureDate", java.sql.Date.valueOf(r.getFeatureDate()))
            .setParameter("timezone", r.getTimezone())
            .setParameter("stressScore", r.getStressScore())
            .setParameter("stressRawValue", r.getStressRawValue())
            .setParameter("stressScoreCalculationVersion", r.getStressScoreCalculationVersion())
            .setParameter("moodScore", r.getMoodScore())
            .setParameter("moodRawValue", r.getMoodRawValue())
            .setParameter("moodScoreCalculationVersion", r.getMoodScoreCalculationVersion())
            .setParameter("energyScore", r.getEnergyScore())
            .setParameter("energyRawValue", r.getEnergyRawValue())
            .setParameter("energyScoreCalculationVersion", r.getEnergyScoreCalculationVersion())
            .setParameter("sleepHours", r.getSleepHours())
            .setParameter("sleepQualityRaw", r.getSleepQualityRaw())
            .setParameter("sleepScore", r.getSleepScore())
            .setParameter("sleepScoreCalculationVersion", r.getSleepScoreCalculationVersion())
            .setParameter("anxietySignal", r.getAnxietySignal())
            .setParameter("anxietySignalConfidence", r.getAnxietySignalConfidence())
            .setParameter("anxietySignalCalculationVersion", r.getAnxietySignalCalculationVersion())
            .setParameter("anxietySignalSource", r.getAnxietySignalSource())
            .setParameter("anxietyAnalysisResultId", r.getAnxietyAnalysisResultId())
            .setParameter("engagementScore", r.getEngagementScore())
            .setParameter("messageCount", r.getMessageCount())
            .setParameter("activeChatSessionCount", r.getActiveChatSessionCount())
            .setParameter("checkinAssignedCount", r.getCheckinAssignedCount())
            .setParameter("checkinCompletedCount", r.getCheckinCompletedCount())
            .setParameter("checkinCompletionRatio", r.getCheckinCompletionRatio())
            .setParameter("engagementScoreCalculationVersion", r.getEngagementScoreCalculationVersion())
            .setParameter("exerciseCompletionRatio", r.getExerciseCompletionRatio())
            .setParameter("exerciseCompletionCalculationVersion", r.getExerciseCompletionCalculationVersion())
            .setParameter("maxRiskLevel", r.getMaxRiskLevel())
            .setParameter("riskEventCount", r.getRiskEventCount())
            .setParameter("maxRiskCalculationVersion", r.getMaxRiskCalculationVersion())
            .setParameter("explicitCoverage", r.getExplicitCoverage())
            .setParameter("inferredConfidence", r.getInferredConfidence())
            .setParameter("featureVersion", r.getFeatureVersion())
            .setParameter("calculationVersion", r.getCalculationVersion())
            .setParameter("extraFeatures", r.getExtraFeatures())
            .setParameter("createdAt", Timestamp.from(r.getCreatedAt().toInstant()))
            .executeUpdate();
        // Native query UUID handling: PostgreSQL returns UUID directly,
        // H2 returns byte[]. Handle both cases.
        Object idObj = entityManager.createNativeQuery(
                "SELECT id FROM user_daily_features WHERE user_id = :uid AND feature_date = :fdate")
            .setParameter("uid", r.getUserId())
            .setParameter("fdate", java.sql.Date.valueOf(r.getFeatureDate()))
            .getSingleResult();
        if (idObj instanceof UUID) {
            return (UUID) idObj;
        }
        // H2: byte[] -> UUID constructor
        return UUID.nameUUIDFromBytes((byte[]) idObj);
    }

    private UUID upsertH2(UserDailyFeature r) {
        // H2 returns UUID as byte[] from native queries — cast to VARCHAR to get String.
        String existingIdStr = (String) entityManager.createNativeQuery(
                "SELECT CAST(id AS VARCHAR) FROM user_daily_features WHERE user_id = :uid AND feature_date = :fdate")
            .setParameter("uid", r.getUserId())
            .setParameter("fdate", java.sql.Date.valueOf(r.getFeatureDate()))
            .getResultStream().findFirst().orElse(null);
        UUID existingId = existingIdStr != null ? UUID.fromString(existingIdStr) : null;
        UUID canonical = existingId != null ? existingId : r.getId();
        // H2 MERGE requires VALUES syntax — not SELECT subquery.
        String sql =
            "MERGE INTO user_daily_features (id, user_id, feature_date, timezone, " +
                "stress_score, stress_raw_value, stress_score_calculation_version, " +
                "mood_score, mood_raw_value, mood_score_calculation_version, " +
                "energy_score, energy_raw_value, energy_score_calculation_version, " +
                "sleep_hours, sleep_quality_raw, sleep_score, sleep_score_calculation_version, " +
                "anxiety_signal, anxiety_signal_confidence, anxiety_signal_calculation_version, " +
                "anxiety_signal_source, anxiety_analysis_result_id, " +
                "engagement_score, message_count, active_chat_session_count, " +
                "checkin_assigned_count, checkin_completed_count, checkin_completion_ratio, " +
                "engagement_score_calculation_version, " +
                "exercise_completion_ratio, exercise_completion_calculation_version, " +
                "max_risk_level, risk_event_count, max_risk_calculation_version, " +
                "explicit_coverage, inferred_confidence, " +
                "feature_version, calculation_version, extra_features, created_at) " +
            "KEY(id, user_id, feature_date) " +
            "VALUES (" +
                ":id, :userId, :featureDate, CAST(:timezone AS VARCHAR), " +
                ":stressScore, :stressRawValue, :stressScoreCalculationVersion, " +
                ":moodScore, :moodRawValue, :moodScoreCalculationVersion, " +
                ":energyScore, :energyRawValue, :energyScoreCalculationVersion, " +
                ":sleepHours, :sleepQualityRaw, :sleepScore, :sleepScoreCalculationVersion, " +
                ":anxietySignal, :anxietySignalConfidence, :anxietySignalCalculationVersion, " +
                ":anxietySignalSource, :anxietyAnalysisResultId, " +
                ":engagementScore, :messageCount, :activeChatSessionCount, " +
                ":checkinAssignedCount, :checkinCompletedCount, :checkinCompletionRatio, " +
                ":engagementScoreCalculationVersion, " +
                ":exerciseCompletionRatio, :exerciseCompletionCalculationVersion, " +
                ":maxRiskLevel, :riskEventCount, :maxRiskCalculationVersion, " +
                ":explicitCoverage, :inferredConfidence, " +
                ":featureVersion, :calculationVersion, :extraFeatures, " +
                "CAST(:createdAt AS TIMESTAMP WITH TIME ZONE))";
        entityManager.createNativeQuery(sql)
            .setParameter("id", canonical)
            .setParameter("userId", r.getUserId())
            .setParameter("featureDate", java.sql.Date.valueOf(r.getFeatureDate()))
            .setParameter("timezone", r.getTimezone())
            .setParameter("stressScore", r.getStressScore())
            .setParameter("stressRawValue", r.getStressRawValue())
            .setParameter("stressScoreCalculationVersion", r.getStressScoreCalculationVersion())
            .setParameter("moodScore", r.getMoodScore())
            .setParameter("moodRawValue", r.getMoodRawValue())
            .setParameter("moodScoreCalculationVersion", r.getMoodScoreCalculationVersion())
            .setParameter("energyScore", r.getEnergyScore())
            .setParameter("energyRawValue", r.getEnergyRawValue())
            .setParameter("energyScoreCalculationVersion", r.getEnergyScoreCalculationVersion())
            .setParameter("sleepHours", r.getSleepHours())
            .setParameter("sleepQualityRaw", r.getSleepQualityRaw())
            .setParameter("sleepScore", r.getSleepScore())
            .setParameter("sleepScoreCalculationVersion", r.getSleepScoreCalculationVersion())
            .setParameter("anxietySignal", r.getAnxietySignal())
            .setParameter("anxietySignalConfidence", r.getAnxietySignalConfidence())
            .setParameter("anxietySignalCalculationVersion", r.getAnxietySignalCalculationVersion())
            .setParameter("anxietySignalSource", r.getAnxietySignalSource())
            .setParameter("anxietyAnalysisResultId", r.getAnxietyAnalysisResultId())
            .setParameter("engagementScore", r.getEngagementScore())
            .setParameter("messageCount", r.getMessageCount())
            .setParameter("activeChatSessionCount", r.getActiveChatSessionCount())
            .setParameter("checkinAssignedCount", r.getCheckinAssignedCount())
            .setParameter("checkinCompletedCount", r.getCheckinCompletedCount())
            .setParameter("checkinCompletionRatio", r.getCheckinCompletionRatio())
            .setParameter("engagementScoreCalculationVersion", r.getEngagementScoreCalculationVersion())
            .setParameter("exerciseCompletionRatio", r.getExerciseCompletionRatio())
            .setParameter("exerciseCompletionCalculationVersion", r.getExerciseCompletionCalculationVersion())
            .setParameter("maxRiskLevel", r.getMaxRiskLevel())
            .setParameter("riskEventCount", r.getRiskEventCount())
            .setParameter("maxRiskCalculationVersion", r.getMaxRiskCalculationVersion())
            .setParameter("explicitCoverage", r.getExplicitCoverage())
            .setParameter("inferredConfidence", r.getInferredConfidence())
            .setParameter("featureVersion", r.getFeatureVersion())
            .setParameter("calculationVersion", r.getCalculationVersion())
            .setParameter("extraFeatures", r.getExtraFeatures())
            .setParameter("createdAt", Timestamp.from(r.getCreatedAt().toInstant()))
            .executeUpdate();
        return canonical;
    }

    private static OffsetDateTime toInstant(OffsetDateTime t) {
        return t == null ? OffsetDateTime.now() : t;
    }
}
