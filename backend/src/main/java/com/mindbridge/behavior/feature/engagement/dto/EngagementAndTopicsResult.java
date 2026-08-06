package com.mindbridge.behavior.feature.engagement.dto;

import java.util.List;
import java.util.UUID;

/**
 * G4-T08 result DTO for {@code EngagementAndTopicsService.summarizeForUser}.
 *
 * <p>Carries:
 * <ul>
 *   <li>{@code userId}: echoed back from the call (for traceability / caching).</li>
 *   <li>{@code engagementActivityScore7d}: integer count of distinct
 *       active sources ({@code chat-message}, {@code chat-session},
 *       {@code checkin-completed}) in the recent 7-day window. Domain
 *       = {@code [0, 3]} per MVP formula {@code v1-unweighted} (no
 *       contribution weights yet - awaiting expert approval per
 *       {@code FEATURE_DICTIONARY_v1.md §10.1}).</li>
 *   <li>{@code engagementActivityScore30d}: same formula over the recent
 *       30-day window.</li>
 *   <li>{@code dominantTopics7d}: top-N topics by frequency in the recent
 *       7-day window. Empty list when no qualifying rows.</li>
 *   <li>{@code dominantTopics30d}: same for the recent 30-day window.</li>
 *   <li>{@code calculationVersion}: stamped from
 *       {@link com.mindbridge.behavior.feature.engagement.config.EngagementConfig#CALCULATION_VERSION}
 *       for audit / UI display.</li>
 * </ul>
 *
 * <p><b>Schema coupling.</b> Per Phase 1 Q1 (xung đột #1, decision B),
 * this DTO is NOT persisted into {@code user_daily_features} (V21) —
 * that column's domain is {@code [0, 1]} and changing it to {@code [0, 3]}
 * would require a separate migration task. The score is computed on demand
 * by the profile endpoint (T12) or by future controllers; consumers must
 * treat this DTO as derived output, not a stored feature.
 *
 * <p><b>Timezone.</b> The caller injects a {@code ZoneId} (T06 / T07
 * pattern) so the 7d/30d window boundaries are computed in the user's
 * local timezone, not the JVM default. The DTO intentionally does NOT
 * carry the zone back - it's the caller's responsibility to track that.
 */
public record EngagementAndTopicsResult(
        UUID userId,
        int engagementActivityScore7d,
        int engagementActivityScore30d,
        List<TopicFrequency> dominantTopics7d,
        List<TopicFrequency> dominantTopics30d,
        String calculationVersion) {

    public EngagementAndTopicsResult {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        validateScore("engagementActivityScore7d", engagementActivityScore7d);
        validateScore("engagementActivityScore30d", engagementActivityScore30d);
        dominantTopics7d = (dominantTopics7d == null) ? List.of() : List.copyOf(dominantTopics7d);
        dominantTopics30d = (dominantTopics30d == null) ? List.of() : List.copyOf(dominantTopics30d);
        if (calculationVersion == null || calculationVersion.isBlank()) {
            throw new IllegalArgumentException("calculationVersion must not be null or blank");
        }
    }

    private static void validateScore(String name, int value) {
        if (value < 0 || value > 3) {
            throw new IllegalArgumentException(
                    name + " must be in [0, 3] (v1-unweighted formula); got " + value);
        }
    }
}
