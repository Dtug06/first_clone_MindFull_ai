package com.mindbridge.behavior.feature.window;

import com.mindbridge.behavior.feature.window.dto.WindowAggregationResult;
import java.time.LocalDate;
import java.util.UUID;

public interface WindowAggregationService {
    WindowAggregationResult aggregateForUser(UUID userId, LocalDate targetDate);
}
