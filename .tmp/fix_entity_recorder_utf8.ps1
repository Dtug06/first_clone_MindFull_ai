$base = "c:\Users\ADMIN\OneDrive\Desktop\first_clone_MindFull_ai\backend\src\main\java\com\mindbridge\behavior\feature\job"
$utf8NoBom = New-Object System.Text.UTF8Encoding($False)
function Write-Java($rel, $content) {
    $path = Join-Path $base $rel
    $dir = Split-Path $path -Parent
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
    Write-Host "OK: $rel"
}
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
import java.time.OffsetDateTime;
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
    @Column(name = ""triggered_by"", nullable = false)
    private JobRunTrigger triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = ""status"", nullable = false)
    private JobRunStatus status;

    @Column(name = ""target_user_id"")
    private UUID targetUserId;

    @Column(name = ""target_local_date"")
    private LocalDate targetLocalDate;

    @Column(name = ""target_date_from"")
    private LocalDate dateFrom;

    @Column(name = ""target_date_to"")
    private LocalDate dateTo;

    @Column(name = ""started_at"", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = ""finished_at"")
    private OffsetDateTime finishedAt;

    @Column(name = ""users_attempted"", nullable = false)
    private int usersAttempted = 0;

    @Column(name = ""users_succeeded"", nullable = false)
    private int usersSucceeded = 0;

    @Column(name = ""users_failed"", nullable = false)
    private int usersFailed = 0;

    @Column(name = ""failure_summary_json"")
    private String failureSummaryJson;

    @Column(name = ""failure_message"")
    private String failureMessage;

    @Column(name = ""created_at"", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public JobRunTrigger getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(JobRunTrigger triggeredBy) { this.triggeredBy = triggeredBy; }
    public JobRunStatus getStatus() { return status; }
    public void setStatus(JobRunStatus status) { this.status = status; }
    public UUID getTargetUserId() { return targetUserId; }
    public void setTargetUserId(UUID targetUserId) { this.targetUserId = targetUserId; }
    public LocalDate getTargetLocalDate() { return targetLocalDate; }
    public void setTargetLocalDate(LocalDate targetLocalDate) { this.targetLocalDate = targetLocalDate; }
    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
    public int getUsersAttempted() { return usersAttempted; }
    public void setUsersAttempted(int usersAttempted) { this.usersAttempted = usersAttempted; }
    public int getUsersSucceeded() { return usersSucceeded; }
    public void setUsersSucceeded(int usersSucceeded) { this.usersSucceeded = usersSucceeded; }
    public int getUsersFailed() { return usersFailed; }
    public void setUsersFailed(int usersFailed) { this.usersFailed = usersFailed; }
    public String getFailureSummaryJson() { return failureSummaryJson; }
    public void setFailureSummaryJson(String failureSummaryJson) { this.failureSummaryJson = failureSummaryJson; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
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
import java.time.OffsetDateTime;
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

    @Column(name = ""target_local_date"", nullable = false)
    private LocalDate targetLocalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = ""status"", nullable = false)
    private JobRunItemLogStatus status;

    @Column(name = ""error_code"")
    private String errorCode;

    @Column(name = ""error_message"")
    private String errorMessage;

    @Column(name = ""duration_ms"")
    private Integer durationMs;

    @Column(name = ""created_at"", nullable = false)
    private OffsetDateTime createdAt;

    public static JobRunItemLog of(UUID id, UUID jobRunId, UUID userId, LocalDate targetLocalDate,
            JobRunItemLogStatus status, String errorCode, String errorMessage,
            Integer durationMs, OffsetDateTime createdAt) {
        JobRunItemLog log = new JobRunItemLog();
        log.id = id;
        log.jobRunId = jobRunId;
        log.userId = userId;
        log.targetLocalDate = targetLocalDate;
        log.status = status;
        log.errorCode = errorCode;
        log.errorMessage = errorMessage;
        log.durationMs = durationMs;
        log.createdAt = createdAt;
        return log;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJobRunId() { return jobRunId; }
    public void setJobRunId(UUID jobRunId) { this.jobRunId = jobRunId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public LocalDate getTargetLocalDate() { return targetLocalDate; }
    public void setTargetLocalDate(LocalDate targetLocalDate) { this.targetLocalDate = targetLocalDate; }
    public JobRunItemLogStatus getStatus() { return status; }
    public void setStatus(JobRunItemLogStatus status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
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
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobRun start(UUID id, String jobName, JobRunTrigger triggeredBy,
            OffsetDateTime startedAt, UUID targetUserId, LocalDate targetLocalDate,
            LocalDate dateFrom, LocalDate dateTo) {
        JobRun jr = new JobRun();
        jr.setId(id);
        jr.setJobName(jobName);
        jr.setTriggeredBy(triggeredBy);
        jr.setStatus(JobRunStatus.RUNNING);
        jr.setStartedAt(startedAt);
        jr.setTargetUserId(targetUserId);
        jr.setTargetLocalDate(targetLocalDate);
        jr.setDateFrom(dateFrom);
        jr.setDateTo(dateTo);
        jr.setUsersAttempted(0);
        jr.setUsersSucceeded(0);
        jr.setUsersFailed(0);
        jr.setCreatedAt(OffsetDateTime.now());
        return jobRunRepository.save(jr);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordItemLog(UUID id, UUID jobRunId, UUID userId, LocalDate targetLocalDate,
            JobRunItemLogStatus status, String errorCode, String errorMessage,
            Integer durationMs, OffsetDateTime createdAt) {
        JobRunItemLog itemLog = JobRunItemLog.of(id, jobRunId, userId, targetLocalDate,
                status, errorCode, errorMessage, durationMs, createdAt);
        itemLogRepository.save(itemLog);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementCounters(UUID jobRunId, boolean success) {
        jobRunRepository.findById(jobRunId).ifPresent(jr -> {
            jr.setUsersAttempted(jr.getUsersAttempted() + 1);
            if (success) jr.setUsersSucceeded(jr.getUsersSucceeded() + 1);
            else jr.setUsersFailed(jr.getUsersFailed() + 1);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(UUID id, JobRunStatus status, OffsetDateTime finishedAt,
            String failureMessage, String failureSummary) {
        jobRunRepository.findById(id).ifPresent(jr -> {
            jr.setStatus(status);
            jr.setFinishedAt(finishedAt);
            if (failureMessage != null) jr.setFailureMessage(failureMessage);
            if (failureSummary != null) jr.setFailureSummaryJson(failureSummary);
            log.info(""G4-T05 JobRun id={} finished: status={} attempted={} succeeded={} failed={}"",
                    id, status, jr.getUsersAttempted(), jr.getUsersSucceeded(), jr.getUsersFailed());
        });
    }
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
        UUID jobRunId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        JobRun run = recorder.start(jobRunId, JOB_NAME, JobRunTrigger.SCHEDULED,
                startedAt.atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime(),
                null, localDate, null, null);
        long startMs = System.currentTimeMillis();
        List<UUID> userIds = behavioralEventRepository.findDistinctUserIdsByLocalDate(localDate);
        log.info(""G4-T05 aggregateAllForDate: date={} userCount={}"", localDate, userIds.size());
        for (UUID userId : userIds) {
            Instant itemStart = Instant.now();
            UserAggregationResult result = aggregateOneUser(userId, localDate);
            OffsetDateTime itemCreated = itemStart.atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime();
            if (result.success()) {
                recorder.recordItemLog(UUID.randomUUID(), run.getId(), userId, localDate,
                        JobRunItemLogStatus.SUCCESS, null, null, null, itemCreated);
                recorder.incrementCounters(run.getId(), true);
            } else {
                recorder.recordItemLog(UUID.randomUUID(), run.getId(), userId, localDate,
                        JobRunItemLogStatus.FAILED, null, result.errorMessage(),
                        (int) (System.currentTimeMillis() - startMs), itemCreated);
                recorder.incrementCounters(run.getId(), false);
            }
        }
        long durationMs = System.currentTimeMillis() - startMs;
        OffsetDateTime finishedAt = Instant.now().atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime();
        jobRunRepository.findById(run.getId()).ifPresent(jr -> {
            int totalAttempted = jr.getUsersAttempted();
            int totalSucceeded = jr.getUsersSucceeded();
            int totalFailed = jr.getUsersFailed();
            JobRunStatus finalStatus = (totalFailed == 0) ? JobRunStatus.SUCCEEDED
                    : (totalSucceeded == 0) ? JobRunStatus.FAILED
                    : JobRunStatus.PARTIAL;
            recorder.finish(run.getId(), finalStatus, finishedAt, null, null);
        });
        JobRun fresh = jobRunRepository.findById(run.getId()).orElse(run);
        return new JobRunSummary(run.getId(), fresh.getStatus(), localDate,
                fresh.getUsersAttempted(), fresh.getUsersSucceeded(), fresh.getUsersFailed(), durationMs);
    }

    @Override
    public JobRunSummary aggregateSingleUserForDateRange(UUID userId, LocalDate dateFrom, LocalDate dateTo) {
        UUID jobRunId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        JobRun run = recorder.start(jobRunId, JOB_NAME, JobRunTrigger.CLI,
                startedAt.atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime(),
                userId, null, dateFrom, dateTo);
        long startMs = System.currentTimeMillis();
        LocalDate cursor = dateFrom;
        while (!cursor.isAfter(dateTo)) {
            Instant itemStart = Instant.now();
            UserAggregationResult result = aggregateOneUser(userId, cursor);
            OffsetDateTime itemCreated = itemStart.atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime();
            if (result.success()) {
                recorder.recordItemLog(UUID.randomUUID(), run.getId(), userId, cursor,
                        JobRunItemLogStatus.SUCCESS, null, null, null, itemCreated);
                recorder.incrementCounters(run.getId(), true);
            } else {
                recorder.recordItemLog(UUID.randomUUID(), run.getId(), userId, cursor,
                        JobRunItemLogStatus.FAILED, null, result.errorMessage(),
                        (int) (System.currentTimeMillis() - startMs), itemCreated);
                recorder.incrementCounters(run.getId(), false);
            }
            cursor = cursor.plusDays(1);
        }
        long durationMs = System.currentTimeMillis() - startMs;
        OffsetDateTime finishedAt = Instant.now().atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime();
        jobRunRepository.findById(run.getId()).ifPresent(jr -> {
            int totalAttempted = jr.getUsersAttempted();
            int totalSucceeded = jr.getUsersSucceeded();
            int totalFailed = jr.getUsersFailed();
            JobRunStatus finalStatus = (totalFailed == 0) ? JobRunStatus.SUCCEEDED
                    : (totalSucceeded == 0) ? JobRunStatus.FAILED
                    : JobRunStatus.PARTIAL;
            recorder.finish(run.getId(), finalStatus, finishedAt, null, null);
        });
        JobRun fresh = jobRunRepository.findById(run.getId()).orElse(run);
        return new JobRunSummary(run.getId(), fresh.getStatus(), dateFrom,
                fresh.getUsersAttempted(), fresh.getUsersSucceeded(), fresh.getUsersFailed(), durationMs);
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
Write-Host "Fixed entity and service files."
