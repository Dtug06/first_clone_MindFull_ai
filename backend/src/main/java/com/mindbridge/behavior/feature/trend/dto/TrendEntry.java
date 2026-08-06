package com.mindbridge.behavior.feature.trend.dto;

import java.math.BigDecimal;

/**
 * G4-T07 per-feature trend entry inside {@link TrendSummary}.
 *
 * @param featureCode one of: stress, mood, energy, sleep, anxiety_signal,
 *                    engagement, exercise_completion, max_risk
 * @param direction UP / DOWN / STABLE / UNKNOWN
 * @param deltaPct signed ratio (recent - prior) / prior; {@code null} when
 *                 undefined (prior = 0, prior = null, or any UNKNOWN case)
 * @param reason one of {@link TrendReason}; never null
 * @param recentAvg the recent 7d average from WindowAggregationService;
 *                  {@code null} when no data
 * @param priorAvg the prior 7d average; {@code null} when no data
 * @param recentCoverage the recent 7d coverage (0..1); may be BigDecimal.ZERO
 * @param priorCoverage the prior 7d coverage (0..1); may be BigDecimal.ZERO
 */
public record TrendEntry(
        String featureCode,
        TrendDirection direction,
        BigDecimal deltaPct,
        TrendReason reason,
        BigDecimal recentAvg,
        BigDecimal priorAvg,
        BigDecimal recentCoverage,
        BigDecimal priorCoverage
) {}