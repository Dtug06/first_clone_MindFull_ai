package com.mindbridge.behavior.feature.job;

import com.mindbridge.behavior.feature.job.dto.JobRunSummary;
import com.mindbridge.behavior.feature.job.dto.UserAggregationResult;
import java.time.LocalDate;
import java.util.UUID;

public interface DailyFeatureAggregationService {
    UserAggregationResult aggregateOneUser(UUID userId, LocalDate localDate);
    JobRunSummary aggregateAllForDate(LocalDate localDate);
    JobRunSummary aggregateSingleUserForDateRange(UUID userId, LocalDate dateFrom, LocalDate dateTo);
}
