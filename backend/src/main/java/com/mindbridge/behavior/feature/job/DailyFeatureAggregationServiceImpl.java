package com.mindbridge.behavior.feature.job;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.FeatureCalculationService;
import com.mindbridge.behavior.feature.DailySourceAggregationService;
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
import com.mindbridge.behavior.feature.job.repository.JobRunRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DailyFeatureAggregationServiceImpl implements DailyFeatureAggregationService {
    private static final Logger log = LoggerFactory.getLogger(DailyFeatureAggregationServiceImpl.class);
    private static final String JOB_NAME = "DailyFeatureAggregation";
    private static final String DEFAULT_TZ = "Asia/Ho_Chi_Minh";
    private static final int BATCH_SIZE = 100;

    private final DailySourceAggregationService sourceAggregationService;
    private final UserRepository userRepository;
    private final DailyFeatureAggregationProperties properties;
    private final FeatureCalculationService featureCalculationService;
    private final UserDailyFeatureMapper featureMapper;
    private final UserDailyFeatureUpsertService upsertService;
    private final JobRunRepository jobRunRepository;
    private final JobRunRecorder recorder;

    public DailyFeatureAggregationServiceImpl(
            DailySourceAggregationService sourceAggregationService,
            UserRepository userRepository,
            DailyFeatureAggregationProperties properties,
            FeatureCalculationService featureCalculationService,
            UserDailyFeatureMapper featureMapper,
            UserDailyFeatureUpsertService upsertService,
            JobRunRepository jobRunRepository,
            JobRunRecorder recorder) {
        this.sourceAggregationService = sourceAggregationService;
        this.userRepository = userRepository;
        this.properties = properties;
        this.featureCalculationService = featureCalculationService;
        this.featureMapper = featureMapper;
        this.upsertService = upsertService;
        this.jobRunRepository = jobRunRepository;
        this.recorder = recorder;
    }

    @Override
    public UserAggregationResult aggregateOneUser(UUID userId, LocalDate localDate) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            String timezone = resolveTimezone(user.getTimezone());
            DailySourceAggregation source = sourceAggregationService.aggregateForDay(
                    userId, timezone, localDate);
            if (source == null) {
                return UserAggregationResult.failure(userId, localDate, "No source data for user on date");
            }
            FeatureConfig config = FeatureConfig.defaults();
            DailyFeatureResult result = featureCalculationService.calculateForDay(source, config);
            UUID entityId = UUID.randomUUID();
            OffsetDateTime createdAt = Instant.now().atZone(ZoneId.of(timezone)).toOffsetDateTime();
            UserDailyFeature entity = new UserDailyFeature();
            featureMapper.toEntity(result, entityId, localDate, timezone, createdAt, entity);
            UUID rowId = upsertService.upsert(entity);
            return UserAggregationResult.success(userId, localDate, rowId);
        } catch (Exception e) {
            log.error("G4-T05 aggregateOneUser failed: userId={} date={}", userId, localDate, e);
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
        long totalUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        int batchSize = properties.batchSize() > 0 ? properties.batchSize() : BATCH_SIZE;
        int page = 0;
        log.info("G4-T05 aggregateAllForDate: date={} totalUsers={}", localDate, totalUsers);
        while (true) {
            var userPage = userRepository.findByStatusOrderByIdAsc(User.UserStatus.ACTIVE, PageRequest.of(page, batchSize));
            if (userPage.isEmpty()) break;
            for (User user : userPage) {
                long itemStart = System.currentTimeMillis();
                UserAggregationResult result = aggregateOneUser(user.getId(), localDate);
                int itemDuration = (int) (System.currentTimeMillis() - itemStart);
                OffsetDateTime itemCreated = Instant.now().atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime();
                if (result.success()) {
                    recorder.recordItemLog(UUID.randomUUID(), run.getId(), user.getId(), localDate,
                            JobRunItemLogStatus.SUCCESS, null, null, itemDuration, itemCreated);
                    recorder.incrementCounters(run.getId(), true);
                } else {
                    recorder.recordItemLog(UUID.randomUUID(), run.getId(), user.getId(), localDate,
                            JobRunItemLogStatus.FAILED, null, result.errorMessage(), itemDuration, itemCreated);
                    recorder.incrementCounters(run.getId(), false);
                }
            }
            page++;
            if (userPage.size() < batchSize) break;
        }
        long durationMs = System.currentTimeMillis() - startMs;
        OffsetDateTime finishedAt = Instant.now().atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime();
        jobRunRepository.findById(run.getId()).ifPresent(jr -> {
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
            long itemStart = System.currentTimeMillis();
            UserAggregationResult result = aggregateOneUser(userId, cursor);
            int itemDuration = (int) (System.currentTimeMillis() - itemStart);
            OffsetDateTime itemCreated = Instant.now().atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime();
            if (result.success()) {
                recorder.recordItemLog(UUID.randomUUID(), run.getId(), userId, cursor,
                        JobRunItemLogStatus.SUCCESS, null, null, itemDuration, itemCreated);
                recorder.incrementCounters(run.getId(), true);
            } else {
                recorder.recordItemLog(UUID.randomUUID(), run.getId(), userId, cursor,
                        JobRunItemLogStatus.FAILED, null, result.errorMessage(), itemDuration, itemCreated);
                recorder.incrementCounters(run.getId(), false);
            }
            cursor = cursor.plusDays(1);
        }
        long durationMs = System.currentTimeMillis() - startMs;
        OffsetDateTime finishedAt = Instant.now().atZone(ZoneId.of(DEFAULT_TZ)).toOffsetDateTime();
        jobRunRepository.findById(run.getId()).ifPresent(jr -> {
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

    private String resolveTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_TZ;
        }
        ZoneId.of(timezone);
        return timezone;
    }
}
