package com.mindbridge.behavior.feature.job.entity;

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
@Table(name = "job_run_item_logs")
public class JobRunItemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_run_id", nullable = false)
    private UUID jobRunId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "target_local_date", nullable = false)
    private LocalDate targetLocalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobRunItemLogStatus status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", nullable = false)
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
