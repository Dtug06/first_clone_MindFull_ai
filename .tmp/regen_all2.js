const fs = require('fs');
const path = require('path');

const PYTHON_SCRIPT = `# -*- coding: utf-8 -*-
import os

ROOT = r'c:\\Users\\ADMIN\\OneDrive\\Desktop\\first_clone_MindFull_ai\\backend\\src\\main\\java\\com\\mindbridge\\behavior\\feature\\job'

# Java file templates
FILES = {
    'entity/UserDailyFeature.java': '''package com.mindbridge.behavior.feature.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_daily_features")
public class UserDailyFeature {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "feature_date", nullable = false, updatable = false)
    private LocalDate featureDate;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @Column(name = "stress_score", precision = 4, scale = 3)
    private BigDecimal stressScore;

    @Column(name = "stress_raw_value")
    private BigDecimal stressRawValue;

    @Column(name = "stress_score_calculation_version", length = 50)
    private String stressScoreCalculationVersion;

    @Column(name = "mood_score", precision = 4, scale = 3)
    private BigDecimal moodScore;

    @Column(name = "mood_raw_value", length = 50)
    private String moodRawValue;

    @Column(name = "mood_score_calculation_version", length = 50)
    private String moodScoreCalculationVersion;

    @Column(name = "energy_score", precision = 4, scale = 3)
    private BigDecimal energyScore;

    @Column(name = "energy_raw_value")
    private BigDecimal energyRawValue;

    @Column(name = "energy_score_calculation_version", length = 50)
    private String energyScoreCalculationVersion;

    @Column(name = "sleep_hours", precision = 4, scale = 2)
    private BigDecimal sleepHours;

    @Column(name = "sleep_quality_raw")
    private Short sleepQualityRaw;

    @Column(name = "sleep_score", precision = 4, scale = 3)
    private BigDecimal sleepScore;

    @Column(name = "sleep_score_calculation_version", length = 50)
    private String sleepScoreCalculationVersion;

    @Column(name = "anxiety_signal", precision = 4, scale = 3)
    private BigDecimal anxietySignal;

    @Column(name = "anxiety_signal_confidence", precision = 4, scale = 3)
    private BigDecimal anxietySignalConfidence;

    @Column(name = "anxiety_signal_source", length = 20)
    private String anxietySignalSource;

    @Column(name = "anxiety_signal_calculation_version", length = 50)
    private String anxietySignalCalculationVersion;

    @Column(name = "anxiety_analysis_result_id")
    private UUID anxietyAnalysisResultId;

    @Column(name = "engagement_score", precision = 4, scale = 3)
    private BigDecimal engagementScore;

    @Column(name = "message_count")
    private Integer messageCount;

    @Column(name = "active_chat_session_count")
    private Integer activeChatSessionCount;

    @Column(name = "checkin_assigned_count")
    private Integer checkinAssignedCount;

    @Column(name = "checkin_completed_count")
    private Integer checkinCompletedCount;

    @Column(name = "checkin_completion_ratio", precision = 4, scale = 3)
    private BigDecimal checkinCompletionRatio;

    @Column(name = "engagement_score_calculation_version", length = 50)
    private String engagementScoreCalculationVersion;

    @Column(name = "exercise_completion_ratio", precision = 5, scale = 4)
    private BigDecimal exerciseCompletionRatio;

    @Column(name = "exercise_completion_calculation_version", length = 50)
    private String exerciseCompletionCalculationVersion;

    @Column(name = "max_risk_level")
    private Short maxRiskLevel;

    @Column(name = "risk_event_count")
    private Integer riskEventCount;

    @Column(name = "max_risk_calculation_version", length = 50)
    private String maxRiskCalculationVersion;

    @Column(name = "explicit_coverage", precision = 4, scale = 3)
    private BigDecimal explicitCoverage;

    @Column(name = "inferred_confidence", precision = 4, scale = 3)
    private BigDecimal inferredConfidence;

    @Column(name = "feature_version", nullable = false, length = 50)
    private String featureVersion;

    @Column(name = "calculation_version", nullable = false, length = 200)
    private String calculationVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_features", columnDefinition = "jsonb")
    private String extraFeatures;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected UserDailyFeature() {}

    public static UserDailyFeature create(UUID id, OffsetDateTime createdAt) {
        UserDailyFeature e = new UserDailyFeature();
        e.id = id;
        e.createdAt = createdAt;
        e.featureVersion = "feature_dictionary_v1";
        return e;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getFeatureDate() { return featureDate; }
    public String getTimezone() { return timezone; }
    public BigDecimal getStressScore() { return stressScore; }
    public BigDecimal getStressRawValue() { return stressRawValue; }
    public String getStressScoreCalculationVersion() { return stressScoreCalculationVersion; }
    public BigDecimal getMoodScore() { return moodScore; }
    public String getMoodRawValue() { return moodRawValue; }
    public String getMoodScoreCalculationVersion() { return moodScoreCalculationVersion; }
    public BigDecimal getEnergyScore() { return energyScore; }
    public BigDecimal getEnergyRawValue() { return energyRawValue; }
    public String getEnergyScoreCalculationVersion() { return energyScoreCalculationVersion; }
    public BigDecimal getSleepHours() { return sleepHours; }
    public Short getSleepQualityRaw() { return sleepQualityRaw; }
    public BigDecimal getSleepScore() { return sleepScore; }
    public String getSleepScoreCalculationVersion() { return sleepScoreCalculationVersion; }
    public BigDecimal getAnxietySignal() { return anxietySignal; }
    public BigDecimal getAnxietySignalConfidence() { return anxietySignalConfidence; }
    public String getAnxietySignalSource() { return anxietySignalSource; }
    public String getAnxietySignalCalculationVersion() { return anxietySignalCalculationVersion; }
    public UUID getAnxietyAnalysisResultId() { return anxietyAnalysisResultId; }
    public BigDecimal getEngagementScore() { return engagementScore; }
    public Integer getMessageCount() { return messageCount; }
    public Integer getActiveChatSessionCount() { return activeChatSessionCount; }
    public Integer getCheckinAssignedCount() { return checkinAssignedCount; }
    public Integer getCheckinCompletedCount() { return checkinCompletedCount; }
    public BigDecimal getCheckinCompletionRatio() { return checkinCompletionRatio; }
    public String getEngagementScoreCalculationVersion() { return engagementScoreCalculationVersion; }
    public BigDecimal getExerciseCompletionRatio() { return exerciseCompletionRatio; }
    public String getExerciseCompletionCalculationVersion() { return exerciseCompletionCalculationVersion; }
    public Short getMaxRiskLevel() { return maxRiskLevel; }
    public Integer getRiskEventCount() { return riskEventCount; }
    public String getMaxRiskCalculationVersion() { return maxRiskCalculationVersion; }
    public BigDecimal getExplicitCoverage() { return explicitCoverage; }
    public BigDecimal getInferredConfidence() { return inferredConfidence; }
    public String getFeatureVersion() { return featureVersion; }
    public String getCalculationVersion() { return calculationVersion; }
    public String getExtraFeatures() { return extraFeatures; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    void setUserId(UUID v) { this.userId = v; }
    void setFeatureDate(LocalDate v) { this.featureDate = v; }
    void setTimezone(String v) { this.timezone = v; }
    void setStressScore(BigDecimal v) { this.stressScore = v; }
    void setStressRawValue(BigDecimal v) { this.stressRawValue = v; }
    void setStressScoreCalculationVersion(String v) { this.stressScoreCalculationVersion = v; }
    void setMoodScore(BigDecimal v) { this.moodScore = v; }
    void setMoodRawValue(String v) { this.moodRawValue = v; }
    void setMoodScoreCalculationVersion(String v) { this.moodScoreCalculationVersion = v; }
    void setEnergyScore(BigDecimal v) { this.energyScore = v; }
    void setEnergyRawValue(BigDecimal v) { this.energyRawValue = v; }
    void setEnergyScoreCalculationVersion(String v) { this.energyScoreCalculationVersion = v; }
    void setSleepHours(BigDecimal v) { this.sleepHours = v; }
    void setSleepQualityRaw(Short v) { this.sleepQualityRaw = v; }
    void setSleepScore(BigDecimal v) { this.sleepScore = v; }
    void setSleepScoreCalculationVersion(String v) { this.sleepScoreCalculationVersion = v; }
    void setAnxietySignal(BigDecimal v) { this.anxietySignal = v; }
    void setAnxietySignalConfidence(BigDecimal v) { this.anxietySignalConfidence = v; }
    void setAnxietySignalSource(String v) { this.anxietySignalSource = v; }
    void setAnxietySignalCalculationVersion(String v) { this.anxietySignalCalculationVersion = v; }
    void setAnxietyAnalysisResultId(UUID v) { this.anxietyAnalysisResultId = v; }
    void setEngagementScore(BigDecimal v) { this.engagementScore = v; }
    void setMessageCount(Integer v) { this.messageCount = v; }
    void setActiveChatSessionCount(Integer v) { this.activeChatSessionCount = v; }
    void setCheckinAssignedCount(Integer v) { this.checkinAssignedCount = v; }
    void setCheckinCompletedCount(Integer v) { this.checkinCompletedCount = v; }
    void setCheckinCompletionRatio(BigDecimal v) { this.checkinCompletionRatio = v; }
    void setEngagementScoreCalculationVersion(String v) { this.engagementScoreCalculationVersion = v; }
    void setExerciseCompletionRatio(BigDecimal v) { this.exerciseCompletionRatio = v; }
    void setExerciseCompletionCalculationVersion(String v) { this.exerciseCompletionCalculationVersion = v; }
    void setMaxRiskLevel(Short v) { this.maxRiskLevel = v; }
    void setRiskEventCount(Integer v) { this.riskEventCount = v; }
    void setMaxRiskCalculationVersion(String v) { this.maxRiskCalculationVersion = v; }
    void setExplicitCoverage(BigDecimal v) { this.explicitCoverage = v; }
    void setInferredConfidence(BigDecimal v) { this.inferredConfidence = v; }
    void setCalculationVersion(String v) { this.calculationVersion = v; }
    void setExtraFeatures(String v) { this.extraFeatures = v; }
}
'''.strip(),

    'mapper/UserDailyFeatureMapper.java': '''package com.mindbridge.behavior.feature.job.mapper;

import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.FeatureSource;
import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserDailyFeatureMapper {

    UserDailyFeatureMapper INSTANCE = Mappers.getMapper(UserDailyFeatureMapper.class);

    @Mapping(target = "id", source = "ctx.entityId")
    @Mapping(target = "userId", source = "ctx.result.userId")
    @Mapping(target = "featureDate", source = "ctx.featureDate")
    @Mapping(target = "timezone", source = "ctx.timezone")
    @Mapping(target = "stressScore", source = "ctx.result.stressScore")
    @Mapping(target = "stressRawValue", source = "ctx.result.stressRawValue")
    @Mapping(target = "stressScoreCalculationVersion", source = "ctx.result.stressCalculationVersion")
    @Mapping(target = "moodScore", source = "ctx.result.moodScore")
    @Mapping(target = "moodRawValue", source = "ctx.result.moodRawLabel")
    @Mapping(target = "moodScoreCalculationVersion", source = "ctx.result.moodCalculationVersion")
    @Mapping(target = "energyScore", source = "ctx.result.energyScore")
    @Mapping(target = "energyRawValue", source = "ctx.result.energyRawValue")
    @Mapping(target = "energyScoreCalculationVersion", source = "ctx.result.energyCalculationVersion")
    @Mapping(target = "sleepHours", source = "ctx.result.sleepDurationHours")
    @Mapping(target = "sleepQualityRaw", source = "ctx.result.sleepQualityRaw")
    @Mapping(target = "sleepScore", source = "ctx.result.sleepScore")
    @Mapping(target = "sleepScoreCalculationVersion", source = "ctx.result.sleepCalculationVersion")
    @Mapping(target = "anxietySignal", source = "ctx.result.anxietyScore")
    @Mapping(target = "anxietySignalConfidence", source = "ctx.result.anxietyConfidence")
    @Mapping(target = "anxietySignalSource", expression = "java(mapAnxietySource(ctx.result().anxietySignalSource()))")
    @Mapping(target = "anxietySignalCalculationVersion", source = "ctx.result.anxietyCalculationVersion")
    @Mapping(target = "anxietyAnalysisResultId", source = "ctx.result.anxietyAnalysisResultId")
    @Mapping(target = "engagementScore", source = "ctx.result.engagementScore")
    @Mapping(target = "messageCount", source = "ctx.result.messageCount")
    @Mapping(target = "activeChatSessionCount", source = "ctx.result.activeChatSessionCount")
    @Mapping(target = "checkinAssignedCount", source = "ctx.result.checkinAssignedCount")
    @Mapping(target = "checkinCompletedCount", source = "ctx.result.checkinCompletedCount")
    @Mapping(target = "checkinCompletionRatio", source = "ctx.result.checkinCompletionRatio")
    @Mapping(target = "engagementScoreCalculationVersion", source = "ctx.result.engagementCalculationVersion")
    @Mapping(target = "exerciseCompletionRatio", source = "ctx.result.exerciseRatio")
    @Mapping(target = "exerciseCompletionCalculationVersion", source = "ctx.result.exerciseCalculationVersion")
    @Mapping(target = "maxRiskLevel", source = "ctx.result.riskLevel")
    @Mapping(target = "riskEventCount", source = "ctx.result.riskEventCount")
    @Mapping(target = "maxRiskCalculationVersion", source = "ctx.result.riskCalculationVersion")
    @Mapping(target = "explicitCoverage", source = "ctx.result.explicitCoverage")
    @Mapping(target = "inferredConfidence", source = "ctx.result.inferredConfidence")
    @Mapping(target = "calculationVersion", source = "ctx.result.calculationVersion")
    @Mapping(target = "featureVersion", ignore = true)
    @Mapping(target = "extraFeatures", ignore = true)
    @Mapping(target = "createdAt", source = "ctx.createdAt")
    void toEntity(ToEntityContext ctx, @MappingTarget UserDailyFeature target);

    record ToEntityContext(
            DailyFeatureResult result,
            UUID entityId,
            LocalDate featureDate,
            String timezone,
            OffsetDateTime createdAt) {
    }

    default String mapAnxietySource(FeatureSource src) {
        if (src == null) return "NONE";
        switch (src) {
            case INFERRED: return "CHAT_ANALYSIS";
            default: return "NONE";
        }
    }
}
'''.strip(),

    'DailyFeatureAggregationServiceImpl.java': '''package com.mindbridge.behavior.feature.job;

import com.mindbridge.behavior.feature.DailySourceAggregationService;
import com.mindbridge.behavior.feature.FeatureCalculationService;
import com.mindbridge.behavior.feature.config.FeatureConfig;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
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
import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyFeatureAggregationServiceImpl implements DailyFeatureAggregationService {

    private static final Logger log = LoggerFactory.getLogger(DailyFeatureAggregationServiceImpl.class);
    static final String JOB_NAME = "daily_feature_aggregation_v1";

    private final UserRepository userRepository;
    private final DailySourceAggregationService sourceService;
    private final FeatureCalculationService calculator;
    private final UserDailyFeatureMapper mapper;
    private final UserDailyFeatureUpsertService upsertService;
    private final JobRunRecorder recorder;
    private final DailyFeatureAggregationProperties properties;
    private final Clock clock;

    public DailyFeatureAggregationServiceImpl(UserRepository userRepository,
            DailySourceAggregationService sourceService,
            FeatureCalculationService calculator,
            UserDailyFeatureMapper mapper,
            UserDailyFeatureUpsertService upsertService,
            JobRunRecorder recorder,
            DailyFeatureAggregationProperties properties,
            Clock clock) {
        this.userRepository = userRepository;
        this.sourceService = sourceService;
        this.calculator = calculator;
        this.mapper = mapper;
        this.upsertService = upsertService;
        this.recorder = recorder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserAggregationResult aggregateOneUser(UUID userId, LocalDate localDate) {
        if (userId == null || localDate == null) {
            throw new IllegalArgumentException("userId and localDate must be non-null");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("User not found or not ACTIVE: " + userId);
        }
        String timezone = user.getTimezone() == null ? "UTC" : user.getTimezone();

        DailySourceAggregation source = sourceService.aggregateForDay(userId, timezone, localDate);
        DailyFeatureResult calc = calculator.calculateForDay(source, FeatureConfig.defaults());

        UUID entityId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC);
        UserDailyFeature row = UserDailyFeature.create(entityId, now);
        mapper.toEntity(
                new UserDailyFeatureMapper.ToEntityContext(calc, entityId, localDate, timezone, now),
                row);

        UUID canonicalId = upsertService.upsert(row);
        log.debug("G4-T05 aggregateOneUser userId={} date={} rowId={}", userId, localDate, canonicalId);
        return UserAggregationResult.success(userId, localDate, canonicalId);
    }

    @Override
    public JobRunSummary aggregateAllForDate(LocalDate localDate) {
        UUID jobRunId = UUID.randomUUID();
        OffsetDateTime startedAt = OffsetDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC);
        JobRun startRow = recorder.start(jobRunId, JOB_NAME, JobRunTrigger.SCHEDULED,
                startedAt, null, localDate, null, null);
        BatchResult result = processAllUsers(java.util.Collections.singletonList(localDate), startRow);
        return finalizeRun(startRow, result, startedAt);
    }

    @Override
    public JobRunSummary aggregateSingleUserForDateRange(UUID userId, LocalDate dateFrom, LocalDate dateTo) {
        UUID jobRunId = UUID.randomUUID();
        OffsetDateTime startedAt = OffsetDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC);
        JobRun startRow = recorder.start(jobRunId, JOB_NAME, JobRunTrigger.CLI,
                startedAt, userId, null, dateFrom, dateTo);
        BatchResult result = processOneUser(userId, dateFrom, dateTo, startRow);
        return finalizeRun(startRow, result, startedAt);
    }

    private BatchResult processAllUsers(List<LocalDate> dates, JobRun startRow) {
        BatchResult agg = new BatchResult();
        long t0 = System.currentTimeMillis();
        long totalUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        log.info("G4-T05 batch start jobRunId={} dates={} totalUsers={} batchSize={}",
                startRow.getId(), dates, totalUsers, properties.batchSize());
        int page = 0;
        while (true) {
            List<User> chunk = userRepository.findByStatusOrderByIdAsc(
                    User.UserStatus.ACTIVE, PageRequest.of(page, properties.batchSize()));
            if (chunk.isEmpty()) break;
            for (User user : chunk) {
                for (LocalDate date : dates) {
                    handleOne(user.getId(), date, startRow, agg);
                }
            }
            page++;
        }
        agg.durationMs = System.currentTimeMillis() - t0;
        log.info("G4-T05 batch end jobRunId={} attempted={} succeeded={} failed={} durationMs={}",
                startRow.getId(), agg.attempted, agg.succeeded, agg.failed, agg.durationMs);
        return agg;
    }

    private BatchResult processOneUser(UUID userId, LocalDate from, LocalDate to, JobRun startRow) {
        BatchResult agg = new BatchResult();
        long t0 = System.currentTimeMillis();
        log.info("G4-T05 single-user start jobRunId={} userId={} from={} to={}",
                startRow.getId(), userId, from, to);
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            handleOne(userId, d, startRow, agg);
        }
        agg.durationMs = System.currentTimeMillis() - t0;
        return agg;
    }

    private void handleOne(UUID userId, LocalDate date, JobRun startRow, BatchResult agg) {
        long itemStart = System.currentTimeMillis();
        OffsetDateTime createdAt = OffsetDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC);
        try {
            UserAggregationResult res = aggregateOneUser(userId, date);
            agg.succeeded++;
            recorder.recordItemLog(UUID.randomUUID(), startRow.getId(), userId, date,
                    JobRunItemLogStatus.SUCCESS, null, null,
                    (int) (System.currentTimeMillis() - itemStart), createdAt);
            recorder.incrementCounters(startRow.getId(), true);
            agg.attempted++;
        } catch (RuntimeException re) {
            agg.attempted++;
            agg.failed++;
            String msg = re.getMessage() == null ? re.getClass().getSimpleName() : re.getMessage();
            log.warn("G4-T05 userId={} date={} failed: {}", userId, date, msg, re);
            recorder.recordItemLog(UUID.randomUUID(), startRow.getId(), userId, date,
                    JobRunItemLogStatus.FAILED, "AGG_RUNTIME_EXCEPTION", msg,
                    (int) (System.currentTimeMillis() - itemStart), createdAt);
            recorder.incrementCounters(startRow.getId(), false);
        }
    }

    private JobRunSummary finalizeRun(JobRun startRow, BatchResult agg, OffsetDateTime startedAt) {
        JobRunStatus terminal;
        if (agg.attempted == 0 || (agg.failed == 0 && agg.succeeded > 0)) {
            terminal = JobRunStatus.SUCCEEDED;
        } else if (agg.succeeded > 0 && agg.failed > 0) {
            terminal = JobRunStatus.PARTIAL;
        } else {
            terminal = JobRunStatus.FAILED;
        }
        OffsetDateTime finishedAt = OffsetDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC);
        String failureMsg = terminal == JobRunStatus.SUCCEEDED ? null : "users_failed=" + agg.failed + "/" + agg.attempted;
        recorder.finish(startRow.getId(), terminal, finishedAt, null, failureMsg);
        return new JobRunSummary(startRow.getId(), terminal, null, agg.attempted, agg.succeeded, agg.failed, agg.durationMs);
    }

    private static final class BatchResult {
        int attempted;
        int succeeded;
        int failed;
        long durationMs;
    }
}
'''.strip(),
}

for (const [relPath, content] of Object.entries(FILES)) {
    const fullPath = path.join(ROOT, relPath);
    // Ensure directory exists
    const dir = path.dirname(fullPath);
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }
    fs.writeFileSync(fullPath, content, 'utf8');
    console.log('Written:', relPath, 'len:', content.length);
}
console.log('Done!');
