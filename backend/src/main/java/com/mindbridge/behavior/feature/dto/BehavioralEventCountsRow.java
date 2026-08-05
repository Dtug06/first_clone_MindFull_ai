package com.mindbridge.behavior.feature.dto;

/**
 * G4-T03: JPA projection row for the per-(user, day) behavioral event
 * aggregation query in {@code BehavioralEventRepository.aggregateByUserAndDay}.
 *
 * <p>This is NOT a public DTO - it exists only to receive the result of one
 * SQL aggregation and is immediately folded into
 * {@link DailySourceAggregation.BehavioralEventCounts} by
 * {@code DailySourceAggregationServiceImpl}.
 *
 * <p>Field order intentionally matches the SELECT projection in the
 * repository query so the Spring Data interface projection maps
 * positionally. Do not reorder without updating the query.
 */
public interface BehavioralEventCountsRow {
    long getChatMessageCount();
    long getActiveChatSessionCount();
    long getCheckinCompletedCount();
    long getCheckinSkippedCount();
}