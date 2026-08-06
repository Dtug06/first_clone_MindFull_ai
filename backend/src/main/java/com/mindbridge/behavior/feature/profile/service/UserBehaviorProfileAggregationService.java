package com.mindbridge.behavior.feature.profile.service;

import com.mindbridge.behavior.feature.profile.config.DataQualityConfig;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import java.time.LocalDate;
import java.util.UUID;

public interface UserBehaviorProfileAggregationService {

    /**
     * Aggregates the profile for the given user at {@code targetDate}
     * using default (unconfigured) data quality thresholds.
     * Throws NullPointerException if thresholds are not configured —
     * prefer {@link #aggregateForUser(UUID, LocalDate, DataQualityConfig)}
     * when a configured instance is available.
     */
    ProfileSnapshot aggregateForUser(UUID userId, LocalDate targetDate);

    /**
     * Aggregates the profile for the given user at {@code targetDate}
     * using the supplied {@link DataQualityConfig}.
     *
     * @param userId    the user to aggregate
     * @param targetDate the window-end date (the "today" anchor)
     * @param dataQualityConfig the data quality configuration (must not be null)
     * @return the snapshot to be upserted into {@code user_behavior_profiles}
     */
    ProfileSnapshot aggregateForUser(UUID userId, LocalDate targetDate,
                                     DataQualityConfig dataQualityConfig);
}