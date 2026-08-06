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

@Service
public class UserDailyFeatureUpsertServiceImpl implements UserDailyFeatureUpsertService {
    private static final Logger log = LoggerFactory.getLogger(UserDailyFeatureUpsertServiceImpl.class);
    private final DbDialect dialect;

    @PersistenceContext
    private EntityManager entityManager;

    public UserDailyFeatureUpsertServiceImpl() {
        String jdbcUrl = System.getProperty("spring.datasource.url");
        this.dialect = DbDialect.fromJdbcUrl(jdbcUrl);
        log.info("G4-T05 Using dialect={} for datasource.url={}", dialect, jdbcUrl);
    }

    @Override
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
                ":featureVersion, :calculationVersion, :extraFeatures, :createdAt" +
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
        return (UUID) entityManager.createNativeQuery(
                "SELECT id FROM user_daily_features WHERE user_id = :uid AND feature_date = :fdate")
            .setParameter("uid", r.getUserId())
            .setParameter("fdate", java.sql.Date.valueOf(r.getFeatureDate()))
            .getSingleResult();
    }

    private UUID upsertH2(UserDailyFeature r) {
        UUID existingId = (UUID) entityManager.createNativeQuery(
                "SELECT id FROM user_daily_features WHERE user_id = :uid AND feature_date = :fdate")
            .setParameter("uid", r.getUserId())
            .setParameter("fdate", java.sql.Date.valueOf(r.getFeatureDate()))
            .getResultStream().findFirst().orElse(null);
        UUID canonical = existingId != null ? existingId : r.getId();
        String sql =
            "MERGE INTO user_daily_features t " +
            "USING (SELECT " +
                "CAST(:id AS UUID) AS id, " +
                "CAST(:userId AS UUID) AS user_id, " +
                "CAST(:featureDate AS DATE) AS feature_date, " +
                ":timezone AS timezone, " +
                ":stressScore AS stress_score, " +
                ":stressRawValue AS stress_raw_value, " +
                ":stressScoreCalculationVersion AS stress_score_calculation_version, " +
                ":moodScore AS mood_score, " +
                ":moodRawValue AS mood_raw_value, " +
                ":moodScoreCalculationVersion AS mood_score_calculation_version, " +
                ":energyScore AS energy_score, " +
                ":energyRawValue AS energy_raw_value, " +
                ":energyScoreCalculationVersion AS energy_score_calculation_version, " +
                ":sleepHours AS sleep_hours, " +
                ":sleepQualityRaw AS sleep_quality_raw, " +
                ":sleepScore AS sleep_score, " +
                ":sleepScoreCalculationVersion AS sleep_score_calculation_version, " +
                ":anxietySignal AS anxiety_signal, " +
                ":anxietySignalConfidence AS anxiety_signal_confidence, " +
                ":anxietySignalCalculationVersion AS anxiety_signal_calculation_version, " +
                ":anxietySignalSource AS anxiety_signal_source, " +
                ":anxietyAnalysisResultId AS anxiety_analysis_result_id, " +
                ":engagementScore AS engagement_score, " +
                ":messageCount AS message_count, " +
                ":activeChatSessionCount AS active_chat_session_count, " +
                ":checkinAssignedCount AS checkin_assigned_count, " +
                ":checkinCompletedCount AS checkin_completed_count, " +
                ":checkinCompletionRatio AS checkin_completion_ratio, " +
                ":engagementScoreCalculationVersion AS engagement_score_calculation_version, " +
                ":exerciseCompletionRatio AS exercise_completion_ratio, " +
                ":exerciseCompletionCalculationVersion AS exercise_completion_calculation_version, " +
                ":maxRiskLevel AS max_risk_level, " +
                ":riskEventCount AS risk_event_count, " +
                ":maxRiskCalculationVersion AS max_risk_calculation_version, " +
                ":explicitCoverage AS explicit_coverage, " +
                ":inferredConfidence AS inferred_confidence, " +
                ":featureVersion AS feature_version, " +
                ":calculationVersion AS calculation_version, " +
                ":extraFeatures AS extra_features, " +
                "CAST(:createdAt AS TIMESTAMP) AS created_at " +
            ") src " +
            "ON (t.user_id = src.user_id AND t.feature_date = src.feature_date) " +
            "WHEN MATCHED THEN UPDATE SET " +
                "id = src.id, " +
                "timezone = src.timezone, " +
                "stress_score = src.stress_score, " +
                "stress_raw_value = src.stress_raw_value, " +
                "stress_score_calculation_version = src.stress_score_calculation_version, " +
                "mood_score = src.mood_score, " +
                "mood_raw_value = src.mood_raw_value, " +
                "mood_score_calculation_version = src.mood_score_calculation_version, " +
                "energy_score = src.energy_score, " +
                "energy_raw_value = src.energy_raw_value, " +
                "energy_score_calculation_version = src.energy_score_calculation_version, " +
                "sleep_hours = src.sleep_hours, " +
                "sleep_quality_raw = src.sleep_quality_raw, " +
                "sleep_score = src.sleep_score, " +
                "sleep_score_calculation_version = src.sleep_score_calculation_version, " +
                "anxiety_signal = src.anxiety_signal, " +
                "anxiety_signal_confidence = src.anxiety_signal_confidence, " +
                "anxiety_signal_calculation_version = src.anxiety_signal_calculation_version, " +
                "anxiety_signal_source = src.anxiety_signal_source, " +
                "anxiety_analysis_result_id = src.anxiety_analysis_result_id, " +
                "engagement_score = src.engagement_score, " +
                "message_count = src.message_count, " +
                "active_chat_session_count = src.active_chat_session_count, " +
                "checkin_assigned_count = src.checkin_assigned_count, " +
                "checkin_completed_count = src.checkin_completed_count, " +
                "checkin_completion_ratio = src.checkin_completion_ratio, " +
                "engagement_score_calculation_version = src.engagement_score_calculation_version, " +
                "exercise_completion_ratio = src.exercise_completion_ratio, " +
                "exercise_completion_calculation_version = src.exercise_completion_calculation_version, " +
                "max_risk_level = src.max_risk_level, " +
                "risk_event_count = src.risk_event_count, " +
                "max_risk_calculation_version = src.max_risk_calculation_version, " +
                "explicit_coverage = src.explicit_coverage, " +
                "inferred_confidence = src.inferred_confidence, " +
                "feature_version = src.feature_version, " +
                "calculation_version = src.calculation_version, " +
                "extra_features = src.extra_features, " +
                "created_at = src.created_at " +
            "WHEN NOT MATCHED THEN INSERT (" +
                "id, user_id, feature_date, timezone, " +
                "stress_score, stress_raw_value, stress_score_calculation_version, " +
                "mood_score, mood_raw_value, mood_score_calculation_version, " +
                "energy_score, energy_raw_value, energy_score_calculation_version, " +
                "sleep_hours, sleep_quality_raw, sleep_score, sleep_score_calculation_version, " +
                "anxiety_signal, anxiety_signal_confidence, anxiety_signal_calculation_version, anxiety_signal_source, " +
                "anxiety_analysis_result_id, " +
                "engagement_score, message_count, active_chat_session_count, " +
                "checkin_assigned_count, checkin_completed_count, checkin_completion_ratio, " +
                "engagement_score_calculation_version, " +
                "exercise_completion_ratio, exercise_completion_calculation_version, " +
                "max_risk_level, risk_event_count, max_risk_calculation_version, " +
                "explicit_coverage, inferred_confidence, " +
                "feature_version, calculation_version, extra_features, created_at" +
            ") VALUES (" +
                "src.id, src.user_id, src.feature_date, src.timezone, " +
                "src.stress_score, src.stress_raw_value, src.stress_score_calculation_version, " +
                "src.mood_score, src.mood_raw_value, src.mood_score_calculation_version, " +
                "src.energy_score, src.energy_raw_value, src.energy_score_calculation_version, " +
                "src.sleep_hours, src.sleep_quality_raw, src.sleep_score, src.sleep_score_calculation_version, " +
                "src.anxiety_signal, src.anxiety_signal_confidence, src.anxiety_signal_calculation_version, src.anxiety_signal_source, " +
                "src.anxiety_analysis_result_id, " +
                "src.engagement_score, src.message_count, src.active_chat_session_count, " +
                "src.checkin_assigned_count, src.checkin_completed_count, src.checkin_completion_ratio, " +
                "src.engagement_score_calculation_version, " +
                "src.exercise_completion_ratio, src.exercise_completion_calculation_version, " +
                "src.max_risk_level, src.risk_event_count, src.max_risk_calculation_version, " +
                "src.explicit_coverage, src.inferred_confidence, " +
                "src.feature_version, src.calculation_version, src.extra_features, src.created_at)";
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
