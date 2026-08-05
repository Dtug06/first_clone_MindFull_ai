package com.mindbridge.behavior.feature.job.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UserAggregationResult(
        UUID userId,
        LocalDate localDate,
        UUID rowId,
        boolean success,
        String errorMessage) {

    public static UserAggregationResult success(UUID userId, LocalDate localDate, UUID rowId) {
        return new UserAggregationResult(userId, localDate, rowId, true, null);
    }

    public static UserAggregationResult failure(UUID userId, LocalDate localDate, String msg) {
        return new UserAggregationResult(userId, localDate, null, false, msg);
    }
}
