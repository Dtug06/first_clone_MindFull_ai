package com.mindbridge.behavior.feature.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record DailyFeatureResult(
        UUID userId,
        LocalDate featureDate,
        String timezone,
        StressResult stress,
        MoodResult mood,
        EnergyResult energy,
        SleepResult sleep,
        AnxietySignalResult anxietySignal,
        EngagementResult engagement,
        ExerciseCompletionResult exerciseCompletion,
        MaxRiskResult maxRisk,
        BigDecimal explicitCoverage,
        BigDecimal inferredConfidence,
        Set<FeatureSourceFlag> sourceFlags,
        String featureVersion,
        String calculationVersion) {

    public record StressResult(
            BigDecimal score,
            BigDecimal rawValue,
            FeatureSource source,
            String calculationVersion) {
    }

    public record MoodResult(
            BigDecimal score,
            String rawLabel,
            FeatureSource source,
            String calculationVersion) {
    }

    public record EnergyResult(
            BigDecimal score,
            BigDecimal rawValue,
            FeatureSource source,
            String calculationVersion) {
    }

    public record SleepResult(
            BigDecimal score,
            BigDecimal durationHours,
            Integer qualityRaw,
            FeatureSource source,
            String calculationVersion) {
    }

    public record AnxietySignalResult(
            BigDecimal score,
            BigDecimal confidence,
            FeatureSource source,
            String calculationVersion,
            UUID analysisResultId) {
    }

    public record EngagementResult(
            BigDecimal score,
            Long messageCount,
            Long activeChatSessionCount,
            Long checkinAssignedCount,
            Long checkinCompletedCount,
            BigDecimal checkinCompletionRatio,
            FeatureSource source,
            String calculationVersion) {
    }

    public record ExerciseCompletionResult(
            BigDecimal ratio,
            ExerciseCompletionStatus status,
            FeatureSource source,
            String calculationVersion) {

        public enum ExerciseCompletionStatus {
            NOT_APPLICABLE,
            COMPUTED
        }
    }

    public record MaxRiskResult(
            Short riskLevel,
            Integer riskEventCount,
            FeatureSource source,
            String calculationVersion) {
    }
}