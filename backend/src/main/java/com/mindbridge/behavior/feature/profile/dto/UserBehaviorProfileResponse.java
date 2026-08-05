package com.mindbridge.behavior.feature.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mindbridge.behavior.feature.engagement.dto.TopicFrequency;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record UserBehaviorProfileResponse(
        String profileVersion,
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
        Integer riskLevel,
        List<TopicFrequency> dominantTopics7d,
        List<TopicFrequency> dominantTopics30d,
        TrendSummaryResponse trendSummary,
        BigDecimal dataCoverage,
        BigDecimal confidence,
        DataQualityStatus dataQualityStatus,
        OffsetDateTime calculatedAt) {

    public UserBehaviorProfileResponse {
        if (profileVersion == null || profileVersion.isBlank()) {
            throw new IllegalArgumentException("profileVersion must not be blank");
        }
        if (windowEnd == null) throw new IllegalArgumentException("windowEnd must not be null");
        if (dataCoverage == null) throw new IllegalArgumentException("dataCoverage must not be null");
        if (confidence == null) throw new IllegalArgumentException("confidence must not be null");
        if (dataQualityStatus == null) throw new IllegalArgumentException("dataQualityStatus must not be null");
        if (calculatedAt == null) throw new IllegalArgumentException("calculatedAt must not be null");
        if (trendSummary == null) throw new IllegalArgumentException("trendSummary must not be null");
        dominantTopics7d = (dominantTopics7d == null) ? List.of() : dominantTopics7d;
        dominantTopics30d = (dominantTopics30d == null) ? List.of() : dominantTopics30d;
        validateEngagement("engagementScore7d", engagementScore7d);
        validateEngagement("engagementScore30d", engagementScore30d);
        validateRisk(riskLevel);
    }

    private static void validateEngagement(String name, Integer v) {
        if (v != null && (v < 0 || v > 3)) {
            throw new IllegalArgumentException(name + " must be in [0, 3]; got " + v);
        }
    }

    private static void validateRisk(Integer v) {
        if (v != null && (v < 1 || v > 4)) {
            throw new IllegalArgumentException("riskLevel must be in [1, 4]; got " + v);
        }
    }
}