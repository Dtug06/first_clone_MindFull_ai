package com.mindbridge.behavior.feature.job.dto;

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
