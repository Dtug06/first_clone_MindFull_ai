package com.mindbridge.behavior.feature.profile.dto;

import com.mindbridge.behavior.feature.engagement.dto.TopicFrequency;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProfileSnapshot(
        UUID userId,
        LocalDate windowEnd,
        BigDecimal stressAvg7d,
        BigDecimal stressAvg30d,
        BigDecimal moodAvg7d,
        BigDecimal moodAvg30d,
        BigDecimal energyAvg7d,
        BigDecimal energyAvg30d,
        BigDecimal sleepAvg7d,
        BigDecimal sleepAvg30d,
        BigDecimal anxietyAvg7d,
        BigDecimal anxietyAvg30d,
        Integer engagementScore7d,
        Integer engagementScore30d,
        String trendSummaryJson,
        List<TopicFrequency> dominantTopics7d,
        List<TopicFrequency> dominantTopics30d,
        Short riskLevel,
        UUID riskHistoryId,
        BigDecimal dataCoverage,
        BigDecimal confidence,
        DataQualityStatus dataQualityStatus,
        OffsetDateTime calculatedAt) {

    public ProfileSnapshot {
        if (userId == null) throw new IllegalArgumentException("userId must not be null");
        if (windowEnd == null) throw new IllegalArgumentException("windowEnd must not be null");
        if (dataCoverage == null
                || BigDecimal.ZERO.compareTo(dataCoverage) > 0
                || BigDecimal.ONE.compareTo(dataCoverage) < 0) {
            throw new IllegalArgumentException("dataCoverage must be in [0, 1]; got " + dataCoverage);
        }
        if (confidence == null
                || BigDecimal.ZERO.compareTo(confidence) > 0
                || BigDecimal.ONE.compareTo(confidence) < 0) {
            throw new IllegalArgumentException("confidence must be in [0, 1]; got " + confidence);
        }
        if (dataQualityStatus == null) throw new IllegalArgumentException("dataQualityStatus must not be null");
        if (calculatedAt == null) throw new IllegalArgumentException("calculatedAt must not be null");
        validateEngagement("engagementScore7d", engagementScore7d);
        validateEngagement("engagementScore30d", engagementScore30d);
        validateRisk(riskLevel);
        dominantTopics7d = (dominantTopics7d == null) ? List.of() : List.copyOf(dominantTopics7d);
        dominantTopics30d = (dominantTopics30d == null) ? List.of() : List.copyOf(dominantTopics30d);
    }

    private static void validateEngagement(String name, Integer value) {
        if (value != null && (value < 0 || value > 3)) {
            throw new IllegalArgumentException(name + " must be in [0, 3]; got " + value);
        }
    }

    private static void validateRisk(Short value) {
        if (value != null && (value < 1 || value > 4)) {
            throw new IllegalArgumentException("riskLevel must be in [1, 4]; got " + value);
        }
    }
}