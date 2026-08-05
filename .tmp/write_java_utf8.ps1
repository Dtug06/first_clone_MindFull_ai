$base = "c:\Users\ADMIN\OneDrive\Desktop\first_clone_MindFull_ai\backend\src\main\java\com\mindbridge\behavior\feature\job"
$utf8NoBom = New-Object System.Text.UTF8Encoding($False)
function Write-Java($rel, $content) {
    $path = Join-Path $base $rel
    $dir = Split-Path $path -Parent
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
    Write-Host "OK: $rel"
}
Write-Java "persistence\DbDialect.java" "package com.mindbridge.behavior.feature.job.persistence;

public enum DbDialect {
    POSTGRESQL,
    H2,
    UNKNOWN;

    public static DbDialect fromJdbcUrl(String url) {
        if (url == null) return UNKNOWN;
        if (url.contains(""postgresql:"") || url.contains(""pgsql:"")) return POSTGRESQL;
        if (url.contains(""h2:"")) return H2;
        return UNKNOWN;
    }
}
"
Write-Java "persistence\UserDailyFeatureUpsertService.java" "package com.mindbridge.behavior.feature.job.persistence;

import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import java.util.UUID;

public interface UserDailyFeatureUpsertService {
    UUID upsert(UserDailyFeature row);
}
"
Write-Java "persistence\UserDailyFeatureUpsertServiceImpl.java" "package com.mindbridge.behavior.feature.job.persistence;

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
        String jdbcUrl = System.getProperty(""spring.datasource.url"");
        this.dialect = DbDialect.fromJdbcUrl(jdbcUrl);
        log.info(""G4-T05 Using dialect={} for datasource.url={}"", dialect, jdbcUrl);
    }

    @Override
    public UUID upsert(UserDailyFeature row) {
        if (row == null) throw new IllegalArgumentException(""row must not be null"");
        if (row.getUserId() == null || row.getFeatureDate() == null)
            throw new IllegalArgumentException(""userId and featureDate must be set"");
        return dialect == DbDialect.POSTGRESQL ? upsertPg(row) : upsertH2(row);
    }

    private UUID upsertPg(UserDailyFeature r) {
        String sql =
            ""INSERT INTO user_daily_features ("" +
                ""id, user_id, feature_date, timezone,"" +
                ""stress_score, stress_raw_value, stress_score_calculation_version,"" +
                ""mood_score, mood_raw_value, mood_score_calculation_version,"" +
                ""energy_score, energy_raw_value, energy_score_calculation_version,"" +
                ""sleep_hours, sleep_quality_raw, sleep_score, sleep_score_calculation_version,"" +
                ""anxiety_signal, anxiety_signal_confidence, anxiety_signal_calculation_version, anxiety_signal_source,"" +
                ""anxiety_analysis_result_id,"" +
                ""engagement_score, message_count, active_chat_session_count,"" +
                ""checkin_assigned_count, checkin_completed_count, checkin_completion_ratio,"" +
                ""engagement_score_calculation_version,"" +
                ""exercise_completion_ratio, exercise_completion_calculation_version,"" +
                ""max_risk_level, risk_event_count, max_risk_calculation_version,"" +
                ""explicit_coverage, inferred_confidence,"" +
                ""feature_version, calculation_version, extra_features, created_at"" +
            "") VALUES ("" +
                "":id, :userId, :featureDate, :timezone,"" +
                "":stressScore, :stressRawValue, :stressScoreCalculationVersion,"" +
                "":moodScore, :moodRawValue, :moodScoreCalculationVersion,"" +
                "":energyScore, :energyRawValue, :energyScoreCalculationVersion,"" +
                "":sleepHours, :sleepQualityRaw, :sleepScore, :sleepScoreCalculationVersion,"" +
                "":anxietySignal, :anxietySignalConfidence, :anxietySignalCalculationVersion, :anxietySignalSource,"" +
                "":anxietyAnalysisResultId,"" +
                "":engagementScore, :messageCount, :activeChatSessionCount,"" +
                "":checkinAssignedCount, :checkinCompletedCount, :checkinCompletionRatio,"" +
                "":engagementScoreCalculationVersion,"" +
                "":exerciseCompletionRatio, :exerciseCompletionCalculationVersion,"" +
                "":maxRiskLevel, :riskEventCount, :maxRiskCalculationVersion,"" +
                "":explicitCoverage, :inferredConfidence,"" +
                "":featureVersion, :calculationVersion, :extraFeatures, :createdAt"" +
            "") ON CONFLICT (user_id, feature_date) DO UPDATE SET"" +
                "" stress_score = EXCLUDED.stress_score,"" +
                "" stress_raw_value = EXCLUDED.stress_raw_value,"" +
                "" stress_score_calculation_version = EXCLUDED.stress_score_calculation_version,"" +
                "" mood_score = EXCLUDED.mood_score,"" +
                "" mood_raw_value = EXCLUDED.mood_raw_value,"" +
                "" mood_score_calculation_version = EXCLUDED.mood_score_calculation_version,"" +
                "" energy_score = EXCLUDED.energy_score,"" +
                "" energy_raw_value = EXCLUDED.energy_raw_value,"" +
                "" energy_score_calculation_version = EXCLUDED.energy_score_calculation_version,"" +
                "" sleep_hours = EXCLUDED.sleep_hours,"" +
                "" sleep_quality_raw = EXCLUDED.sleep_quality_raw,"" +
                "" sleep_score = EXCLUDED.sleep_score,"" +
                "" sleep_score_calculation_version = EXCLUDED.sleep_score_calculation_version,"" +
                "" anxiety_signal = EXCLUDED.anxiety_signal,"" +
                "" anxiety_signal_confidence = EXCLUDED.anxiety_signal_confidence,"" +
                "" anxiety_signal_calculation_version = EXCLUDED.anxiety_signal_calculation_version,"" +
                "" anxiety_signal_source = EXCLUDED.anxiety_signal_source,"" +
                "" anxiety_analysis_result_id = EXCLUDED.anxiety_analysis_result_id,"" +
                "" engagement_score = EXCLUDED.engagement_score,"" +
                "" message_count = EXCLUDED.message_count,"" +
                "" active_chat_session_count = EXCLUDED.active_chat_session_count,"" +
                "" checkin_assigned_count = EXCLUDED.checkin_assigned_count,"" +
                "" checkin_completed_count = EXCLUDED.checkin_completed_count,"" +
                "" checkin_completion_ratio = EXCLUDED.checkin_completion_ratio,"" +
                "" engagement_score_calculation_version = EXCLUDED.engagement_score_calculation_version,"" +
                "" exercise_completion_ratio = EXCLUDED.exercise_completion_ratio,"" +
                "" exercise_completion_calculation_version = EXCLUDED.exercise_completion_calculation_version,"" +
                "" max_risk_level = EXCLUDED.max_risk_level,"" +
                "" risk_event_count = EXCLUDED.risk_event_count,"" +
                "" max_risk_calculation_version = EXCLUDED.max_risk_calculation_version,"" +
                "" explicit_coverage = EXCLUDED.explicit_coverage,"" +
                "" inferred_confidence = EXCLUDED.inferred_confidence,"" +
                "" feature_version = EXCLUDED.feature_version,"" +
                "" calculation_version = EXCLUDED.calculation_version,"" +
                "" extra_features = EXCLUDED.extra_features"";
        entityManager.createNativeQuery(sql)
            .setParameter(""id"", r.getId())
            .setParameter(""userId"", r.getUserId())
            .setParameter(""featureDate"", java.sql.Date.valueOf(r.getFeatureDate()))
            .setParameter(""timezone"", r.getTimezone())
            .setParameter(""stressScore"", r.getStressScore())
            .setParameter(""stressRawValue"", r.getStressRawValue())
            .setParameter(""stressScoreCalculationVersion"", r.getStressScoreCalculationVersion())
            .setParameter(""moodScore"", r.getMoodScore())
            .setParameter(""moodRawValue"", r.getMoodRawValue())
            .setParameter(""moodScoreCalculationVersion"", r.getMoodScoreCalculationVersion())
            .setParameter(""energyScore"", r.getEnergyScore())
            .setParameter(""energyRawValue"", r.getEnergyRawValue())
            .setParameter(""energyScoreCalculationVersion"", r.getEnergyScoreCalculationVersion())
            .setParameter(""sleepHours"", r.getSleepHours())
            .setParameter(""sleepQualityRaw"", r.getSleepQualityRaw())
            .setParameter(""sleepScore"", r.getSleepScore())
            .setParameter(""sleepScoreCalculationVersion"", r.getSleepScoreCalculationVersion())
            .setParameter(""anxietySignal"", r.getAnxietySignal())
            .setParameter(""anxietySignalConfidence"", r.getAnxietySignalConfidence())
            .setParameter(""anxietySignalCalculationVersion"", r.getAnxietySignalCalculationVersion())
            .setParameter(""anxietySignalSource"", r.getAnxietySignalSource())
            .setParameter(""anxietyAnalysisResultId"", r.getAnxietyAnalysisResultId())
            .setParameter(""engagementScore"", r.getEngagementScore())
            .setParameter(""messageCount"", r.getMessageCount())
            .setParameter(""activeChatSessionCount"", r.getActiveChatSessionCount())
            .setParameter(""checkinAssignedCount"", r.getCheckinAssignedCount())
            .setParameter(""checkinCompletedCount"", r.getCheckinCompletedCount())
            .setParameter(""checkinCompletionRatio"", r.getCheckinCompletionRatio())
            .setParameter(""engagementScoreCalculationVersion"", r.getEngagementScoreCalculationVersion())
            .setParameter(""exerciseCompletionRatio"", r.getExerciseCompletionRatio())
            .setParameter(""exerciseCompletionCalculationVersion"", r.getExerciseCompletionCalculationVersion())
            .setParameter(""maxRiskLevel"", r.getMaxRiskLevel())
            .setParameter(""riskEventCount"", r.getRiskEventCount())
            .setParameter(""maxRiskCalculationVersion"", r.getMaxRiskCalculationVersion())
            .setParameter(""explicitCoverage"", r.getExplicitCoverage())
            .setParameter(""inferredConfidence"", r.getInferredConfidence())
            .setParameter(""featureVersion"", r.getFeatureVersion())
            .setParameter(""calculationVersion"", r.getCalculationVersion())
            .setParameter(""extraFeatures"", r.getExtraFeatures())
            .setParameter(""createdAt"", Timestamp.from(toInstant(r.getCreatedAt())))
            .executeUpdate();
        return (UUID) entityManager.createNativeQuery(
                ""SELECT id FROM user_daily_features WHERE user_id = :uid AND feature_date = :fdate"")
            .setParameter(""uid"", r.getUserId())
            .setParameter(""fdate"", java.sql.Date.valueOf(r.getFeatureDate()))
            .getSingleResult();
    }

    private UUID upsertH2(UserDailyFeature r) {
        UUID existingId = (UUID) entityManager.createNativeQuery(
                ""SELECT id FROM user_daily_features WHERE user_id = :uid AND feature_date = :fdate"")
            .setParameter(""uid"", r.getUserId())
            .setParameter(""fdate"", java.sql.Date.valueOf(r.getFeatureDate()))
            .getResultStream().findFirst().orElse(null);
        UUID canonical = existingId != null ? existingId : r.getId();
        String sql =
            ""MERGE INTO user_daily_features t "" +
            ""USING (SELECT "" +
                ""CAST(:id AS UUID) AS id, "" +
                ""CAST(:userId AS UUID) AS user_id, "" +
                ""CAST(:featureDate AS DATE) AS feature_date, "" +
                "":timezone AS timezone, "" +
                "":stressScore AS stress_score, "" +
                "":stressRawValue AS stress_raw_value, "" +
                "":stressScoreCalculationVersion AS stress_score_calculation_version, "" +
                "":moodScore AS mood_score, "" +
                "":moodRawValue AS mood_raw_value, "" +
                "":moodScoreCalculationVersion AS mood_score_calculation_version, "" +
                "":energyScore AS energy_score, "" +
                "":energyRawValue AS energy_raw_value, "" +
                "":energyScoreCalculationVersion AS energy_score_calculation_version, "" +
                "":sleepHours AS sleep_hours, "" +
                "":sleepQualityRaw AS sleep_quality_raw, "" +
                "":sleepScore AS sleep_score, "" +
                "":sleepScoreCalculationVersion AS sleep_score_calculation_version, "" +
                "":anxietySignal AS anxiety_signal, "" +
                "":anxietySignalConfidence AS anxiety_signal_confidence, "" +
                "":anxietySignalCalculationVersion AS anxiety_signal_calculation_version, "" +
                "":anxietySignalSource AS anxiety_signal_source, "" +
                "":anxietyAnalysisResultId AS anxiety_analysis_result_id, "" +
                "":engagementScore AS engagement_score, "" +
                "":messageCount AS message_count, "" +
                "":activeChatSessionCount AS active_chat_session_count, "" +
                "":checkinAssignedCount AS checkin_assigned_count, "" +
                "":checkinCompletedCount AS checkin_completed_count, "" +
                "":checkinCompletionRatio AS checkin_completion_ratio, "" +
                "":engagementScoreCalculationVersion AS engagement_score_calculation_version, "" +
                "":exerciseCompletionRatio AS exercise_completion_ratio, "" +
                "":exerciseCompletionCalculationVersion AS exercise_completion_calculation_version, "" +
                "":maxRiskLevel AS max_risk_level, "" +
                "":riskEventCount AS risk_event_count, "" +
                "":maxRiskCalculationVersion AS max_risk_calculation_version, "" +
                "":explicitCoverage AS explicit_coverage, "" +
                "":inferredConfidence AS inferred_confidence, "" +
                "":featureVersion AS feature_version, "" +
                "":calculationVersion AS calculation_version, "" +
                "":extraFeatures AS extra_features, "" +
                ""CAST(:createdAt AS TIMESTAMP) AS created_at "" +
            "") src "" +
            ""ON (t.user_id = src.user_id AND t.feature_date = src.feature_date) "" +
            ""WHEN MATCHED THEN UPDATE SET "" +
                ""id = src.id, "" +
                ""timezone = src.timezone, "" +
                ""stress_score = src.stress_score, "" +
                ""stress_raw_value = src.stress_raw_value, "" +
                ""stress_score_calculation_version = src.stress_score_calculation_version, "" +
                ""mood_score = src.mood_score, "" +
                ""mood_raw_value = src.mood_raw_value, "" +
                ""mood_score_calculation_version = src.mood_score_calculation_version, "" +
                ""energy_score = src.energy_score, "" +
                ""energy_raw_value = src.energy_raw_value, "" +
                ""energy_score_calculation_version = src.energy_score_calculation_version, "" +
                ""sleep_hours = src.sleep_hours, "" +
                ""sleep_quality_raw = src.sleep_quality_raw, "" +
                ""sleep_score = src.sleep_score, "" +
                ""sleep_score_calculation_version = src.sleep_score_calculation_version, "" +
                ""anxiety_signal = src.anxiety_signal, "" +
                ""anxiety_signal_confidence = src.anxiety_signal_confidence, "" +
                ""anxiety_signal_calculation_version = src.anxiety_signal_calculation_version, "" +
                ""anxiety_signal_source = src.anxiety_signal_source, "" +
                ""anxiety_analysis_result_id = src.anxiety_analysis_result_id, "" +
                ""engagement_score = src.engagement_score, "" +
                ""message_count = src.message_count, "" +
                ""active_chat_session_count = src.active_chat_session_count, "" +
                ""checkin_assigned_count = src.checkin_assigned_count, "" +
                ""checkin_completed_count = src.checkin_completed_count, "" +
                ""checkin_completion_ratio = src.checkin_completion_ratio, "" +
                ""engagement_score_calculation_version = src.engagement_score_calculation_version, "" +
                ""exercise_completion_ratio = src.exercise_completion_ratio, "" +
                ""exercise_completion_calculation_version = src.exercise_completion_calculation_version, "" +
                ""max_risk_level = src.max_risk_level, "" +
                ""risk_event_count = src.risk_event_count, "" +
                ""max_risk_calculation_version = src.max_risk_calculation_version, "" +
                ""explicit_coverage = src.explicit_coverage, "" +
                ""inferred_confidence = src.inferred_confidence, "" +
                ""feature_version = src.feature_version, "" +
                ""calculation_version = src.calculation_version, "" +
                ""extra_features = src.extra_features, "" +
                ""created_at = src.created_at "" +
            ""WHEN NOT MATCHED THEN INSERT ("" +
                ""id, user_id, feature_date, timezone, "" +
                ""stress_score, stress_raw_value, stress_score_calculation_version, "" +
                ""mood_score, mood_raw_value, mood_score_calculation_version, "" +
                ""energy_score, energy_raw_value, energy_score_calculation_version, "" +
                ""sleep_hours, sleep_quality_raw, sleep_score, sleep_score_calculation_version, "" +
                ""anxiety_signal, anxiety_signal_confidence, anxiety_signal_calculation_version, anxiety_signal_source, "" +
                ""anxiety_analysis_result_id, "" +
                ""engagement_score, message_count, active_chat_session_count, "" +
                ""checkin_assigned_count, checkin_completed_count, checkin_completion_ratio, "" +
                ""engagement_score_calculation_version, "" +
                ""exercise_completion_ratio, exercise_completion_calculation_version, "" +
                ""max_risk_level, risk_event_count, max_risk_calculation_version, "" +
                ""explicit_coverage, inferred_confidence, "" +
                ""feature_version, calculation_version, extra_features, created_at"" +
            "") VALUES ("" +
                ""src.id, src.user_id, src.feature_date, src.timezone, "" +
                ""src.stress_score, src.stress_raw_value, src.stress_score_calculation_version, "" +
                ""src.mood_score, src.mood_raw_value, src.mood_score_calculation_version, "" +
                ""src.energy_score, src.energy_raw_value, src.energy_score_calculation_version, "" +
                ""src.sleep_hours, src.sleep_quality_raw, src.sleep_score, src.sleep_score_calculation_version, "" +
                ""src.anxiety_signal, src.anxiety_signal_confidence, src.anxiety_signal_calculation_version, src.anxiety_signal_source, "" +
                ""src.anxiety_analysis_result_id, "" +
                ""src.engagement_score, src.message_count, src.active_chat_session_count, "" +
                ""src.checkin_assigned_count, src.checkin_completed_count, src.checkin_completion_ratio, "" +
                ""src.engagement_score_calculation_version, "" +
                ""src.exercise_completion_ratio, src.exercise_completion_calculation_version, "" +
                ""src.max_risk_level, src.risk_event_count, src.max_risk_calculation_version, "" +
                ""src.explicit_coverage, src.inferred_confidence, "" +
                ""src.feature_version, src.calculation_version, src.extra_features, src.created_at)"";
        entityManager.createNativeQuery(sql)
            .setParameter(""id"", canonical)
            .setParameter(""userId"", r.getUserId())
            .setParameter(""featureDate"", java.sql.Date.valueOf(r.getFeatureDate()))
            .setParameter(""timezone"", r.getTimezone())
            .setParameter(""stressScore"", r.getStressScore())
            .setParameter(""stressRawValue"", r.getStressRawValue())
            .setParameter(""stressScoreCalculationVersion"", r.getStressScoreCalculationVersion())
            .setParameter(""moodScore"", r.getMoodScore())
            .setParameter(""moodRawValue"", r.getMoodRawValue())
            .setParameter(""moodScoreCalculationVersion"", r.getMoodScoreCalculationVersion())
            .setParameter(""energyScore"", r.getEnergyScore())
            .setParameter(""energyRawValue"", r.getEnergyRawValue())
            .setParameter(""energyScoreCalculationVersion"", r.getEnergyScoreCalculationVersion())
            .setParameter(""sleepHours"", r.getSleepHours())
            .setParameter(""sleepQualityRaw"", r.getSleepQualityRaw())
            .setParameter(""sleepScore"", r.getSleepScore())
            .setParameter(""sleepScoreCalculationVersion"", r.getSleepScoreCalculationVersion())
            .setParameter(""anxietySignal"", r.getAnxietySignal())
            .setParameter(""anxietySignalConfidence"", r.getAnxietySignalConfidence())
            .setParameter(""anxietySignalCalculationVersion"", r.getAnxietySignalCalculationVersion())
            .setParameter(""anxietySignalSource"", r.getAnxietySignalSource())
            .setParameter(""anxietyAnalysisResultId"", r.getAnxietyAnalysisResultId())
            .setParameter(""engagementScore"", r.getEngagementScore())
            .setParameter(""messageCount"", r.getMessageCount())
            .setParameter(""activeChatSessionCount"", r.getActiveChatSessionCount())
            .setParameter(""checkinAssignedCount"", r.getCheckinAssignedCount())
            .setParameter(""checkinCompletedCount"", r.getCheckinCompletedCount())
            .setParameter(""checkinCompletionRatio"", r.getCheckinCompletionRatio())
            .setParameter(""engagementScoreCalculationVersion"", r.getEngagementScoreCalculationVersion())
            .setParameter(""exerciseCompletionRatio"", r.getExerciseCompletionRatio())
            .setParameter(""exerciseCompletionCalculationVersion"", r.getExerciseCompletionCalculationVersion())
            .setParameter(""maxRiskLevel"", r.getMaxRiskLevel())
            .setParameter(""riskEventCount"", r.getRiskEventCount())
            .setParameter(""maxRiskCalculationVersion"", r.getMaxRiskCalculationVersion())
            .setParameter(""explicitCoverage"", r.getExplicitCoverage())
            .setParameter(""inferredConfidence"", r.getInferredConfidence())
            .setParameter(""featureVersion"", r.getFeatureVersion())
            .setParameter(""calculationVersion"", r.getCalculationVersion())
            .setParameter(""extraFeatures"", r.getExtraFeatures())
            .setParameter(""createdAt"", Timestamp.from(toInstant(r.getCreatedAt())))
            .executeUpdate();
        return canonical;
    }

    private static OffsetDateTime toInstant(OffsetDateTime t) {
        return t == null ? OffsetDateTime.now() : t;
    }
}
"
Write-Java "recorder\JobRunRecorder.java" "package com.mindbridge.behavior.feature.job.recorder;

import com.mindbridge.behavior.feature.job.entity.JobRun;
import com.mindbridge.behavior.feature.job.entity.JobRunItemLog;
import com.mindbridge.behavior.feature.job.entity.JobRunItemLogStatus;
import com.mindbridge.behavior.feature.job.entity.JobRunStatus;
import com.mindbridge.behavior.feature.job.entity.JobRunTrigger;
import com.mindbridge.behavior.feature.job.repository.JobRunItemLogRepository;
import com.mindbridge.behavior.feature.job.repository.JobRunRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JobRunRecorder {
    private static final Logger log = LoggerFactory.getLogger(JobRunRecorder.class);
    private final JobRunRepository jobRunRepository;
    private final JobRunItemLogRepository itemLogRepository;

    public JobRunRecorder(JobRunRepository jobRunRepository, JobRunItemLogRepository itemLogRepository) {
        this.jobRunRepository = jobRunRepository;
        this.itemLogRepository = itemLogRepository;
    }

    @Transactional
    public JobRun start(String jobName, JobRunTrigger trigger, LocalDate targetDate) {
        JobRun run = new JobRun();
        run.setJobName(jobName);
        run.setTrigger(trigger);
        run.setTargetDate(targetDate);
        run.setStatus(JobRunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        return jobRunRepository.save(run);
    }

    @Transactional
    public void recordItem(UUID jobRunId, UUID userId, LocalDate localDate,
                          JobRunItemLogStatus status, String errorMessage) {
        JobRunItemLog item = new JobRunItemLog();
        item.setJobRunId(jobRunId);
        item.setUserId(userId);
        item.setLocalDate(localDate);
        item.setStatus(status);
        item.setErrorMessage(errorMessage);
        itemLogRepository.save(item);
    }

    @Transactional
    public void finish(UUID jobRunId, JobRunStatus status) {
        jobRunRepository.findById(jobRunId).ifPresent(run -> {
            run.setStatus(status);
            run.setFinishedAt(LocalDateTime.now());
            jobRunRepository.save(run);
            log.info(""G4-T05 JobRun id={} finished: status={}"", jobRunId, status);
        });
    }
}
"
Write-Java "entity\JobRun.java" "package com.mindbridge.behavior.feature.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = ""job_runs"")
public class JobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = ""job_name"", nullable = false)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = ""trigger_type"", nullable = false)
    private JobRunTrigger trigger;

    @Column(name = ""target_date"")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = ""status"", nullable = false)
    private JobRunStatus status;

    @Column(name = ""started_at"", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = ""finished_at"")
    private LocalDateTime finishedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public JobRunTrigger getTrigger() { return trigger; }
    public void setTrigger(JobRunTrigger trigger) { this.trigger = trigger; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public JobRunStatus getStatus() { return status; }
    public void setStatus(JobRunStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
"
Write-Java "entity\JobRunItemLog.java" "package com.mindbridge.behavior.feature.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = ""job_run_item_logs"")
public class JobRunItemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = ""job_run_id"", nullable = false)
    private UUID jobRunId;

    @Column(name = ""user_id"", nullable = false)
    private UUID userId;

    @Column(name = ""local_date"", nullable = false)
    private LocalDate localDate;

    @Enumerated(EnumType.STRING)
    @Column(name = ""status"", nullable = false)
    private JobRunItemLogStatus status;

    @Column(name = ""error_message"")
    private String errorMessage;

    @Column(name = ""created_at"", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJobRunId() { return jobRunId; }
    public void setJobRunId(UUID jobRunId) { this.jobRunId = jobRunId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public LocalDate getLocalDate() { return localDate; }
    public void setLocalDate(LocalDate localDate) { this.localDate = localDate; }
    public JobRunItemLogStatus getStatus() { return status; }
    public void setStatus(JobRunItemLogStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
"
Write-Java "entity\JobRunStatus.java" "package com.mindbridge.behavior.feature.job.entity;

public enum JobRunStatus {
    RUNNING,
    SUCCEEDED,
    PARTIAL,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == PARTIAL || this == FAILED;
    }
}
"
Write-Java "entity\JobRunTrigger.java" "package com.mindbridge.behavior.feature.job.entity;

public enum JobRunTrigger {
    SCHEDULED,
    CLI,
    MANUAL;
}
"
Write-Java "entity\JobRunItemLogStatus.java" "package com.mindbridge.behavior.feature.job.entity;

public enum JobRunItemLogStatus {
    SUCCESS,
    FAILED,
    SKIPPED;
}
"
Write-Java "dto\JobRunSummary.java" "package com.mindbridge.behavior.feature.job.dto;

import com.mindbridge.behavior.feature.job.entity.JobRunStatus;
import java.time.LocalDate;
import java.util.UUID;

public record JobRunSummary(
        UUID jobRunId,
        JobRunStatus status,
        LocalDate targetLocalDate,
        int usersAttempted,
        int usersSucceeded,
        int usersFailed,
        long durationMs) {
}
"
Write-Java "dto\UserAggregationResult.java" "package com.mindbridge.behavior.feature.job.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UserAggregationResult(
        UUID userId,
        LocalDate localDate,
        UUID rowId,
        boolean success,
        String errorMessage) {

    public static UserAggregationResult success(UUID userId, LocalDate localDate, UUID rowId) {
        return new UserAggregationResult(userId, localDate, rowId, true, null);
    }

    public static UserAggregationResult failure(UUID userId, LocalDate localDate, String msg) {
        return new UserAggregationResult(userId, localDate, null, false, msg);
    }
}
"
Write-Java "DailyFeatureAggregationProperties.java" "package com.mindbridge.behavior.feature.job;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = ""mindbridge.feature-aggregation"")
public record DailyFeatureAggregationProperties(boolean enabled, int batchSize) {
}
"
Write-Java "DailyFeatureAggregationService.java" "package com.mindbridge.behavior.feature.job;

import com.mindbridge.behavior.feature.job.dto.JobRunSummary;
import com.mindbridge.behavior.feature.job.dto.UserAggregationResult;
import java.time.LocalDate;
import java.util.UUID;

public interface DailyFeatureAggregationService {
    UserAggregationResult aggregateOneUser(UUID userId, LocalDate localDate);
    JobRunSummary aggregateAllForDate(LocalDate localDate);
    JobRunSummary aggregateSingleUserForDateRange(UUID userId, LocalDate dateFrom, LocalDate dateTo);
}
"
Write-Java "DailyFeatureAggregationServiceImpl.java" "package com.mindbridge.behavior.feature.job;

import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
import com.mindbridge.behavior.feature.dto.FeatureSourceFlag;
import com.mindbridge.behavior.feature.impl.FeatureCalculationServiceImpl;
import com.mindbridge.behavior.feature.job.dto.JobRunSummary;
import com.mindbridge.behavior.feature.job.dto.UserAggregationResult;
import com.mindbridge.behavior.feature.job.entity.JobRun;
import com.mindbridge.behavior.feature.job.entity.JobRunItemLogStatus;
import com.mindbridge.behavior.feature.job.entity.JobRunStatus;
import com.mindbridge.behavior.feature.job.entity.JobRunTrigger;
import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import com.mindbridge.behavior.feature.job.mapper.UserDailyFeatureMapper;
import com.mindbridge.behavior.feature.job.persistence.UserDailyFeatureUpsertService;
import com.mindbridge.behavior.feature.job.recorder.JobRunRecorder;
import com.mindbridge.behavior.feature.job.repository.JobRunItemLogRepository;
import com.mindbridge.behavior.feature.job.repository.JobRunRepository;
import com.mindbridge.behavior.repository.BehavioralEventRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionAnswerRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyFeatureAggregationServiceImpl implements DailyFeatureAggregationService {
    private static final Logger log = LoggerFactory.getLogger(DailyFeatureAggregationServiceImpl.class);
    private static final String JOB_NAME = ""DailyFeatureAggregation"";
    private static final String DEFAULT_TZ = ""Asia/Ho_Chi_Minh"";

    private final BehavioralEventRepository behavioralEventRepository;
    private final DailyQuestionAnswerRepository dailyQuestionAnswerRepository;
    private final FeatureCalculationServiceImpl featureCalculationService;
    private final UserDailyFeatureMapper featureMapper;
    private final UserDailyFeatureUpsertService upsertService;
    private final JobRunRepository jobRunRepository;
    private final JobRunItemLogRepository itemLogRepository;
    private final JobRunRecorder recorder;

    public DailyFeatureAggregationServiceImpl(
            BehavioralEventRepository behavioralEventRepository,
            DailyQuestionAnswerRepository dailyQuestionAnswerRepository,
            FeatureCalculationServiceImpl featureCalculationService,
            UserDailyFeatureMapper featureMapper,
            UserDailyFeatureUpsertService upsertService,
            JobRunRepository jobRunRepository,
            JobRunItemLogRepository itemLogRepository,
            JobRunRecorder recorder) {
        this.behavioralEventRepository = behavioralEventRepository;
        this.dailyQuestionAnswerRepository = dailyQuestionAnswerRepository;
        this.featureCalculationService = featureCalculationService;
        this.featureMapper = featureMapper;
        this.upsertService = upsertService;
        this.jobRunRepository = jobRunRepository;
        this.itemLogRepository = itemLogRepository;
        this.recorder = recorder;
    }

    @Override
    @Transactional
    public UserAggregationResult aggregateOneUser(UUID userId, LocalDate localDate) {
        try {
            DailySourceAggregation source = aggregateSource(userId, localDate, DEFAULT_TZ);
            if (source == null) {
                return UserAggregationResult.failure(userId, localDate, ""No source data for user on date"");
            }
            DailyFeatureResult result = featureCalculationService.calculate(source);
            UUID entityId = UUID.randomUUID();
            Instant createdAt = Instant.now();
            UserDailyFeatureMapper.ToEntityContext ctx = new UserDailyFeatureMapper.ToEntityContext(
                    result, entityId, localDate, DEFAULT_TZ,
                    createdAt.atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime());
            UserDailyFeature entity = new UserDailyFeature();
            featureMapper.toEntity(ctx, entity);
            UUID rowId = upsertService.upsert(entity);
            return UserAggregationResult.success(userId, localDate, rowId);
        } catch (Exception e) {
            log.error(""G4-T05 aggregateOneUser failed: userId={} date={}"", userId, localDate, e);
            return UserAggregationResult.failure(userId, localDate, e.getMessage());
        }
    }

    @Override
    public JobRunSummary aggregateAllForDate(LocalDate localDate) {
        JobRun run = recorder.start(JOB_NAME, JobRunTrigger.SCHEDULED, localDate);
        long startMs = System.currentTimeMillis();
        List<UUID> userIds = behavioralEventRepository.findDistinctUserIdsByLocalDate(localDate);
        log.info(""G4-T05 aggregateAllForDate: date={} userCount={}"", localDate, userIds.size());
        int succeeded = 0;
        int failed = 0;
        for (UUID userId : userIds) {
            UserAggregationResult result = aggregateOneUser(userId, localDate);
            if (result.success()) {
                succeeded++;
                recorder.recordItem(run.getId(), userId, localDate, JobRunItemLogStatus.SUCCESS, null);
            } else {
                failed++;
                recorder.recordItem(run.getId(), userId, localDate, JobRunItemLogStatus.FAILED, result.errorMessage());
            }
        }
        long durationMs = System.currentTimeMillis() - startMs;
        JobRunStatus status = (failed == 0) ? JobRunStatus.SUCCEEDED : (succeeded == 0) ? JobRunStatus.FAILED : JobRunStatus.PARTIAL;
        recorder.finish(run.getId(), status);
        return new JobRunSummary(run.getId(), status, localDate, succeeded + failed, succeeded, failed, durationMs);
    }

    @Override
    public JobRunSummary aggregateSingleUserForDateRange(UUID userId, LocalDate dateFrom, LocalDate dateTo) {
        JobRun run = recorder.start(JOB_NAME, JobRunTrigger.CLI, dateFrom);
        long startMs = System.currentTimeMillis();
        LocalDate cursor = dateFrom;
        int succeeded = 0;
        int failed = 0;
        while (!cursor.isAfter(dateTo)) {
            UserAggregationResult result = aggregateOneUser(userId, cursor);
            if (result.success()) {
                succeeded++;
                recorder.recordItem(run.getId(), userId, cursor, JobRunItemLogStatus.SUCCESS, null);
            } else {
                failed++;
                recorder.recordItem(run.getId(), userId, cursor, JobRunItemLogStatus.FAILED, result.errorMessage());
            }
            cursor = cursor.plusDays(1);
        }
        long durationMs = System.currentTimeMillis() - startMs;
        JobRunStatus status = (failed == 0) ? JobRunStatus.SUCCEEDED : (succeeded == 0) ? JobRunStatus.FAILED : JobRunStatus.PARTIAL;
        recorder.finish(run.getId(), status);
        return new JobRunSummary(run.getId(), status, dateFrom, succeeded + failed, succeeded, failed, durationMs);
    }

    private DailySourceAggregation aggregateSource(UUID userId, LocalDate localDate, String timezone) {
        var zoneOffset = java.time.ZoneOffset.of(""+07:00"");
        var windowStart = zoneOffset.getRules().getOffset(java.time.Instant.now()).adjustInto(
                localDate.atStartOfDay(java.time.ZoneId.of(timezone)).toInstant());
        var windowEnd = zoneOffset.getRules().getOffset(java.time.Instant.now()).adjustInto(
                localDate.plusDays(1).atStartOfDay(java.time.ZoneId.of(timezone)).toInstant());
        return new DailySourceAggregation(
                userId, timezone, localDate,
                windowStart, windowEnd,
                dailyQuestionAnswerRepository.findByUserIdAndDate(userId, localDate),
                behavioralEventRepository.findByUserIdAndLocalDate(userId, localDate),
                List.of(), FeatureSourceFlag.NONE, 0L, 0L, 0L, 0L);
    }
}
"
Write-Java "DailyFeatureAggregationJob.java" "package com.mindbridge.behavior.feature.job;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ""mindbridge.feature-aggregation.enabled"", havingValue = ""true"", matchIfMissing = false)
public class DailyFeatureAggregationJob {
    private static final Logger log = LoggerFactory.getLogger(DailyFeatureAggregationJob.class);
    private final DailyFeatureAggregationService service;

    public DailyFeatureAggregationJob(DailyFeatureAggregationService service) {
        this.service = service;
    }

    @Scheduled(cron = ""${mindbridge.feature-aggregation.schedule-cron:0 0 3 * * *}"")
    public void runDailyAggregation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info(""G4-T05 scheduled job starting for date={}"", yesterday);
        try {
            var summary = service.aggregateAllForDate(yesterday);
            log.info(""G4-T05 scheduled job finished: status={} attempted={} succeeded={} failed={}"",
                    summary.status(), summary.usersAttempted(), summary.usersSucceeded(), summary.usersFailed());
        } catch (Exception e) {
            log.error(""G4-T05 scheduled job failed"", e);
        }
    }
}
"
Write-Java "repository\JobRunRepository.java" "package com.mindbridge.behavior.feature.job.repository;

import com.mindbridge.behavior.feature.job.entity.JobRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRunRepository extends JpaRepository<JobRun, UUID> {
    Optional<JobRun> findFirstByJobNameOrderByStartedAtDesc(String jobName);
}
"
Write-Java "repository\JobRunItemLogRepository.java" "package com.mindbridge.behavior.feature.job.repository;

import com.mindbridge.behavior.feature.job.entity.JobRunItemLog;
import com.mindbridge.behavior.feature.job.entity.JobRunItemLogStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRunItemLogRepository extends JpaRepository<JobRunItemLog, UUID> {
    List<JobRunItemLog> findByJobRunIdOrderByCreatedAtAsc(UUID jobRunId);
    long countByJobRunIdAndStatus(UUID jobRunId, JobRunItemLogStatus status);
}
"
Write-Java "cli\DailyFeatureAggregationCliProperties.java" "package com.mindbridge.behavior.feature.job.cli;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = ""mindbridge.feature-aggregation.run"")
public record DailyFeatureAggregationCliProperties(boolean enabled, String target) {
}
"
Write-Java "cli\DailyFeatureAggregationCliTarget.java" "package com.mindbridge.behavior.feature.job.cli;

import java.time.LocalDate;
import java.util.UUID;

public record DailyFeatureAggregationCliTarget(TargetKind kind, UUID userId, LocalDate dateFrom, LocalDate dateTo) {

    public enum TargetKind {
        ALL_USERS_FOR_DATE,
        SINGLE_USER_DATE_RANGE;
    }

    public static DailyFeatureAggregationCliTarget forAllUsers(LocalDate date) {
        return new DailyFeatureAggregationCliTarget(TargetKind.ALL_USERS_FOR_DATE, null, date, null);
    }

    public static DailyFeatureAggregationCliTarget forUser(UUID userId, LocalDate from, LocalDate to) {
        return new DailyFeatureAggregationCliTarget(TargetKind.SINGLE_USER_DATE_RANGE, userId, from, to);
    }

    public boolean isValid() {
        return switch (kind) {
            case ALL_USERS_FOR_DATE -> dateFrom != null;
            case SINGLE_USER_DATE_RANGE -> userId != null && dateFrom != null && dateTo != null && !dateTo.isBefore(dateFrom);
        };
    }
}
"
Write-Java "cli\DailyFeatureAggregationCliTargetParser.java" "package com.mindbridge.behavior.feature.job.cli;

import java.time.LocalDate;
import java.util.UUID;

public final class DailyFeatureAggregationCliTargetParser {
    private DailyFeatureAggregationCliTargetParser() {}

    public static DailyFeatureAggregationCliTarget parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(""CLI target must not be blank"");
        }
        String[] parts = raw.trim().split("":"");
        if (parts.length < 2) throw new IllegalArgumentException(""Invalid CLI target format: "" + raw);
        switch (parts[0].toUpperCase()) {
            case ""ALL"" -> {
                if (parts.length != 2) throw new IllegalArgumentException(""ALL requires DATE"");
                return DailyFeatureAggregationCliTarget.forAllUsers(parseDate(parts[1]));
            }
            case ""USER"" -> {
                if (parts.length != 4) throw new IllegalArgumentException(""USER requires UUID:DATE:DATE"");
                return DailyFeatureAggregationCliTarget.forUser(parseUuid(parts[1]), parseDate(parts[2]), parseDate(parts[3]));
            }
            default -> throw new IllegalArgumentException(""Unknown target kind: "" + parts[0]);
        }
    }

    private static LocalDate parseDate(String s) {
        try { return LocalDate.parse(s); }
        catch (Exception e) { throw new IllegalArgumentException(""Invalid date: "" + s, e); }
    }

    private static UUID parseUuid(String s) {
        try { return UUID.fromString(s); }
        catch (Exception e) { throw new IllegalArgumentException(""Invalid UUID: "" + s, e); }
    }
}
"
Write-Java "cli\DailyFeatureAggregationCliRunner.java" "package com.mindbridge.behavior.feature.job.cli;

import com.mindbridge.behavior.feature.job.DailyFeatureAggregationService;
import com.mindbridge.behavior.feature.job.dto.JobRunSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = ""mindbridge.feature-aggregation.run.enabled"", havingValue = ""true"")
public class DailyFeatureAggregationCliRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DailyFeatureAggregationCliRunner.class);
    private final DailyFeatureAggregationService service;
    private final DailyFeatureAggregationCliProperties props;

    public DailyFeatureAggregationCliRunner(DailyFeatureAggregationService service,
            DailyFeatureAggregationCliProperties props) {
        this.service = service;
        this.props = props;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            log.warn(""No CLI arguments provided. Use --target=ALL:YYYY-MM-DD or USER:<uuid>:YYYY-MM-DD:YYYY-MM-DD"");
            return;
        }
        String raw = args[0].replaceFirst(""^--target="", """");
        DailyFeatureAggregationCliTarget target = DailyFeatureAggregationCliTargetParser.parse(raw);
        if (!target.isValid()) {
            throw new IllegalArgumentException(""Invalid CLI target: "" + raw);
        }
        log.info(""G4-T05 CLI run starting: kind={}"", target.kind());
        JobRunSummary summary = switch (target.kind()) {
            case ALL_USERS_FOR_DATE -> service.aggregateAllForDate(target.dateFrom());
            case SINGLE_USER_DATE_RANGE -> service.aggregateSingleUserForDateRange(target.userId(), target.dateFrom(), target.dateTo());
        };
        log.info(""G4-T05 CLI run finished: status={} attempted={} succeeded={} failed={}"",
                summary.status(), summary.usersAttempted(), summary.usersSucceeded(), summary.usersFailed());
    }
}
"
Write-Java "entity\UserDailyFeature.java" "package com.mindbridge.behavior.feature.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = ""user_daily_features"")
public class UserDailyFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = ""user_id"", nullable = false)
    private UUID userId;

    @Column(name = ""feature_date"", nullable = false)
    private LocalDate featureDate;

    @Column(name = ""timezone"", nullable = false)
    private String timezone;

    @Column(name = ""stress_score"")
    private BigDecimal stressScore;
    @Column(name = ""stress_raw_value"")
    private BigDecimal stressRawValue;
    @Column(name = ""stress_score_calculation_version"")
    private Integer stressScoreCalculationVersion;

    @Column(name = ""mood_score"")
    private BigDecimal moodScore;
    @Column(name = ""mood_raw_value"")
    private BigDecimal moodRawValue;
    @Column(name = ""mood_score_calculation_version"")
    private Integer moodScoreCalculationVersion;

    @Column(name = ""energy_score"")
    private BigDecimal energyScore;
    @Column(name = ""energy_raw_value"")
    private BigDecimal energyRawValue;
    @Column(name = ""energy_score_calculation_version"")
    private Integer energyScoreCalculationVersion;

    @Column(name = ""sleep_hours"")
    private BigDecimal sleepHours;
    @Column(name = ""sleep_quality_raw"")
    private BigDecimal sleepQualityRaw;
    @Column(name = ""sleep_score"")
    private BigDecimal sleepScore;
    @Column(name = ""sleep_score_calculation_version"")
    private Integer sleepScoreCalculationVersion;

    @Column(name = ""anxiety_signal"")
    private BigDecimal anxietySignal;
    @Column(name = ""anxiety_signal_confidence"")
    private BigDecimal anxietySignalConfidence;
    @Column(name = ""anxiety_signal_calculation_version"")
    private Integer anxietySignalCalculationVersion;
    @Column(name = ""anxiety_signal_source"")
    private String anxietySignalSource;
    @Column(name = ""anxiety_analysis_result_id"")
    private UUID anxietyAnalysisResultId;

    @Column(name = ""engagement_score"")
    private BigDecimal engagementScore;
    @Column(name = ""message_count"")
    private Long messageCount;
    @Column(name = ""active_chat_session_count"")
    private Long activeChatSessionCount;

    @Column(name = ""checkin_assigned_count"")
    private Integer checkinAssignedCount;
    @Column(name = ""checkin_completed_count"")
    private Integer checkinCompletedCount;
    @Column(name = ""checkin_completion_ratio"")
    private BigDecimal checkinCompletionRatio;

    @Column(name = ""engagement_score_calculation_version"")
    private Integer engagementScoreCalculationVersion;
    @Column(name = ""exercise_completion_ratio"")
    private BigDecimal exerciseCompletionRatio;
    @Column(name = ""exercise_completion_calculation_version"")
    private Integer exerciseCompletionCalculationVersion;

    @Column(name = ""max_risk_level"")
    private Integer maxRiskLevel;
    @Column(name = ""risk_event_count"")
    private Integer riskEventCount;
    @Column(name = ""max_risk_calculation_version"")
    private Integer maxRiskCalculationVersion;

    @Column(name = ""explicit_coverage"")
    private BigDecimal explicitCoverage;
    @Column(name = ""inferred_confidence"")
    private BigDecimal inferredConfidence;

    @Column(name = ""feature_version"")
    private Integer featureVersion;
    @Column(name = ""calculation_version"")
    private Integer calculationVersion;

    @Column(name = ""extra_features"", columnDefinition = ""TEXT"")
    private String extraFeatures;

    @Column(name = ""created_at"", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public LocalDate getFeatureDate() { return featureDate; }
    public void setFeatureDate(LocalDate featureDate) { this.featureDate = featureDate; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public BigDecimal getStressScore() { return stressScore; }
    public void setStressScore(BigDecimal stressScore) { this.stressScore = stressScore; }
    public BigDecimal getStressRawValue() { return stressRawValue; }
    public void setStressRawValue(BigDecimal stressRawValue) { this.stressRawValue = stressRawValue; }
    public Integer getStressScoreCalculationVersion() { return stressScoreCalculationVersion; }
    public void setStressScoreCalculationVersion(Integer v) { this.stressScoreCalculationVersion = v; }
    public BigDecimal getMoodScore() { return moodScore; }
    public void setMoodScore(BigDecimal moodScore) { this.moodScore = moodScore; }
    public BigDecimal getMoodRawValue() { return moodRawValue; }
    public void setMoodRawValue(BigDecimal moodRawValue) { this.moodRawValue = moodRawValue; }
    public Integer getMoodScoreCalculationVersion() { return moodScoreCalculationVersion; }
    public void setMoodScoreCalculationVersion(Integer v) { this.moodScoreCalculationVersion = v; }
    public BigDecimal getEnergyScore() { return energyScore; }
    public void setEnergyScore(BigDecimal energyScore) { this.energyScore = energyScore; }
    public BigDecimal getEnergyRawValue() { return energyRawValue; }
    public void setEnergyRawValue(BigDecimal energyRawValue) { this.energyRawValue = energyRawValue; }
    public Integer getEnergyScoreCalculationVersion() { return energyScoreCalculationVersion; }
    public void setEnergyScoreCalculationVersion(Integer v) { this.energyScoreCalculationVersion = v; }
    public BigDecimal getSleepHours() { return sleepHours; }
    public void setSleepHours(BigDecimal sleepHours) { this.sleepHours = sleepHours; }
    public BigDecimal getSleepQualityRaw() { return sleepQualityRaw; }
    public void setSleepQualityRaw(BigDecimal sleepQualityRaw) { this.sleepQualityRaw = sleepQualityRaw; }
    public BigDecimal getSleepScore() { return sleepScore; }
    public void setSleepScore(BigDecimal sleepScore) { this.sleepScore = sleepScore; }
    public Integer getSleepScoreCalculationVersion() { return sleepScoreCalculationVersion; }
    public void setSleepScoreCalculationVersion(Integer v) { this.sleepScoreCalculationVersion = v; }
    public BigDecimal getAnxietySignal() { return anxietySignal; }
    public void setAnxietySignal(BigDecimal anxietySignal) { this.anxietySignal = anxietySignal; }
    public BigDecimal getAnxietySignalConfidence() { return anxietySignalConfidence; }
    public void setAnxietySignalConfidence(BigDecimal v) { this.anxietySignalConfidence = v; }
    public Integer getAnxietySignalCalculationVersion() { return anxietySignalCalculationVersion; }
    public void setAnxietySignalCalculationVersion(Integer v) { this.anxietySignalCalculationVersion = v; }
    public String getAnxietySignalSource() { return anxietySignalSource; }
    public void setAnxietySignalSource(String s) { this.anxietySignalSource = s; }
    public UUID getAnxietyAnalysisResultId() { return anxietyAnalysisResultId; }
    public void setAnxietyAnalysisResultId(UUID u) { this.anxietyAnalysisResultId = u; }
    public BigDecimal getEngagementScore() { return engagementScore; }
    public void setEngagementScore(BigDecimal e) { this.engagementScore = e; }
    public Long getMessageCount() { return messageCount; }
    public void setMessageCount(Long m) { this.messageCount = m; }
    public Long getActiveChatSessionCount() { return activeChatSessionCount; }
    public void setActiveChatSessionCount(Long a) { this.activeChatSessionCount = a; }
    public Integer getCheckinAssignedCount() { return checkinAssignedCount; }
    public void setCheckinAssignedCount(Integer c) { this.checkinAssignedCount = c; }
    public Integer getCheckinCompletedCount() { return checkinCompletedCount; }
    public void setCheckinCompletedCount(Integer c) { this.checkinCompletedCount = c; }
    public BigDecimal getCheckinCompletionRatio() { return checkinCompletionRatio; }
    public void setCheckinCompletionRatio(BigDecimal r) { this.checkinCompletionRatio = r; }
    public Integer getEngagementScoreCalculationVersion() { return engagementScoreCalculationVersion; }
    public void setEngagementScoreCalculationVersion(Integer v) { this.engagementScoreCalculationVersion = v; }
    public BigDecimal getExerciseCompletionRatio() { return exerciseCompletionRatio; }
    public void setExerciseCompletionRatio(BigDecimal e) { this.exerciseCompletionRatio = e; }
    public Integer getExerciseCompletionCalculationVersion() { return exerciseCompletionCalculationVersion; }
    public void setExerciseCompletionCalculationVersion(Integer v) { this.exerciseCompletionCalculationVersion = v; }
    public Integer getMaxRiskLevel() { return maxRiskLevel; }
    public void setMaxRiskLevel(Integer m) { this.maxRiskLevel = m; }
    public Integer getRiskEventCount() { return riskEventCount; }
    public void setRiskEventCount(Integer r) { this.riskEventCount = r; }
    public Integer getMaxRiskCalculationVersion() { return maxRiskCalculationVersion; }
    public void setMaxRiskCalculationVersion(Integer v) { this.maxRiskCalculationVersion = v; }
    public BigDecimal getExplicitCoverage() { return explicitCoverage; }
    public void setExplicitCoverage(BigDecimal e) { this.explicitCoverage = e; }
    public BigDecimal getInferredConfidence() { return inferredConfidence; }
    public void setInferredConfidence(BigDecimal i) { this.inferredConfidence = i; }
    public Integer getFeatureVersion() { return featureVersion; }
    public void setFeatureVersion(Integer f) { this.featureVersion = f; }
    public Integer getCalculationVersion() { return calculationVersion; }
    public void setCalculationVersion(Integer c) { this.calculationVersion = c; }
    public String getExtraFeatures() { return extraFeatures; }
    public void setExtraFeatures(String e) { this.extraFeatures = e; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime c) { this.createdAt = c; }
}
"
Write-Host "All 22 Java files written successfully."
