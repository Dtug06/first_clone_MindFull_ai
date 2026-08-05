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
@Table(name = "job_runs")
public class JobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false)
    private JobRunTrigger triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobRunStatus status;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "target_local_date")
    private LocalDate targetLocalDate;

    @Column(name = "target_date_from")
    private LocalDate dateFrom;

    @Column(name = "target_date_to")
    private LocalDate dateTo;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "users_attempted", nullable = false)
    private int usersAttempted = 0;

    @Column(name = "users_succeeded", nullable = false)
    private int usersSucceeded = 0;

    @Column(name = "users_failed", nullable = false)
    private int usersFailed = 0;

    @Column(name = "failure_summary_json")
    private String failureSummaryJson;

    @Column(name = "failure_message")
    private String failureMessage;

    @Column(name = "created_at", nullable = false)
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
