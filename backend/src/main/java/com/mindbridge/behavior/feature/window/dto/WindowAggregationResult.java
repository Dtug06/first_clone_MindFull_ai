package com.mindbridge.behavior.feature.window.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record WindowAggregationResult(
    UUID userId,
    LocalDate targetDate,

    // stress
    BigDecimal stressScore7d,
    BigDecimal stressScore30d,
    BigDecimal stressCoverage7d,
    BigDecimal stressCoverage30d,
    BigDecimal stressRawAvg30d,

    // mood
    BigDecimal moodScore7d,
    BigDecimal moodScore30d,
    BigDecimal moodCoverage7d,
    BigDecimal moodCoverage30d,

    // energy
    BigDecimal energyScore7d,
    BigDecimal energyScore30d,
    BigDecimal energyCoverage7d,
    BigDecimal energyCoverage30d,

    // sleep
    BigDecimal sleepHoursAvg7d,
    BigDecimal sleepHoursAvg30d,
    BigDecimal sleepScore7d,
    BigDecimal sleepScore30d,
    BigDecimal sleepCoverage7d,
    BigDecimal sleepCoverage30d,

    // anxiety_signal
    BigDecimal anxietySignal7d,
    BigDecimal anxietySignal30d,
    BigDecimal anxietyConfidence7d,
    BigDecimal anxietyConfidence30d,
    String anxietySource7d,
    String anxietySource30d,
    BigDecimal anxietyCoverage7d,
    BigDecimal anxietyCoverage30d,

    // engagement
    BigDecimal engagementScore7d,
    BigDecimal engagementScore30d,
    BigDecimal engagementCoverage7d,
    BigDecimal engagementCoverage30d,
    Long messageCountSum7d,
    Long messageCountSum30d,
    Long checkinCompletedSum7d,
    Long checkinCompletedSum30d,

    // exercise_completion
    BigDecimal exerciseCompletionRatio7d,
    BigDecimal exerciseCompletionRatio30d,
    String exerciseCompletionStatus7d,
    String exerciseCompletionStatus30d,

    // max_risk
    Integer maxRiskLevel7d,
    Integer maxRiskLevel30d,
    Long riskEventCount7d,
    Long riskEventCount30d,
    BigDecimal maxRiskCoverage7d,
    BigDecimal maxRiskCoverage30d,

    // Overall quality signals
    BigDecimal explicitCoverage7d,
    BigDecimal explicitCoverage30d,
    BigDecimal inferredConfidence7d,
    BigDecimal inferredConfidence30d
) {}
