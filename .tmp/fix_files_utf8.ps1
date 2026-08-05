$base = "c:\Users\ADMIN\OneDrive\Desktop\first_clone_MindFull_ai\backend\src\main\java\com\mindbridge\behavior\feature\job"
$utf8NoBom = New-Object System.Text.UTF8Encoding($False)
function Write-Java($rel, $content) {
    $path = Join-Path $base $rel
    $dir = Split-Path $path -Parent
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
    Write-Host "Fixed: $rel"
}
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

    @Scheduled(cron = ""`${mindbridge.feature-aggregation.schedule-cron:0 0 3 * * *}"")
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
Write-Host "Fixed 2 files."
