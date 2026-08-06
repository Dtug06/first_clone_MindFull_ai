package com.mindbridge.behavior.feature.job.cli;

import java.time.LocalDate;
import java.util.UUID;

public record DailyFeatureAggregationCliTarget(TargetKind kind, UUID userId, LocalDate dateFrom, LocalDate dateTo) {

    public enum TargetKind {
        ALL_USERS_FOR_DATE,
        SINGLE_USER_DATE_RANGE;
    }

    public static DailyFeatureAggregationCliTarget forAllUsers(LocalDate date) {
        return new DailyFeatureAggregationCliTarget(TargetKind.ALL_USERS_FOR_DATE, null, date, null);
    }

    public static DailyFeatureAggregationCliTarget forUser(UUID userId, LocalDate from, LocalDate to) {
        return new DailyFeatureAggregationCliTarget(TargetKind.SINGLE_USER_DATE_RANGE, userId, from, to);
    }

    public boolean isValid() {
        return switch (kind) {
            case ALL_USERS_FOR_DATE -> dateFrom != null;
            case SINGLE_USER_DATE_RANGE -> userId != null && dateFrom != null && dateTo != null && !dateTo.isBefore(dateFrom);
        };
    }
}
