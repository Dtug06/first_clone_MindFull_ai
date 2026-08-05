package com.mindbridge.behavior.feature.job.recorder;

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
            log.info("G4-T05 JobRun id={} finished: status={} attempted={} succeeded={} failed={}",
                    id, status, jr.getUsersAttempted(), jr.getUsersSucceeded(), jr.getUsersFailed());
        });
    }
}
