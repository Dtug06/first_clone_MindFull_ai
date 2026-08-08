package com.mindbridge.chat.personalization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Bounded, non-clinical context that the backend may provide to conversational AI.
 * Raw chat, free-text Daily answers, email, and Safety evidence are deliberately absent.
 */
public record PersonalizationContext(
        String displayName,
        LocalDate contextDate,
        List<DailyObservation> dailyObservations,
        BehaviorProfileObservation behaviorProfile) {

    public PersonalizationContext {
        dailyObservations = dailyObservations == null ? List.of() : List.copyOf(dailyObservations);
    }

    public static PersonalizationContext empty() {
        return new PersonalizationContext(null, null, List.of(), null);
    }

    public boolean available() {
        return displayName != null || !dailyObservations.isEmpty() || behaviorProfile != null;
    }

    public record DailyObservation(
            String code,
            BigDecimal numericValue,
            String optionValue) {
    }

    public record BehaviorProfileObservation(
            LocalDate windowEnd,
            String dataQualityStatus,
            BigDecimal dataCoverage,
            BigDecimal confidence,
            BigDecimal stressAvg7d,
            BigDecimal stressAvg30d,
            BigDecimal moodAvg7d,
            BigDecimal moodAvg30d,
            BigDecimal energyAvg7d,
            BigDecimal energyAvg30d,
            BigDecimal sleepAvg7d,
            BigDecimal sleepAvg30d,
            List<String> dominantTopics7d,
            List<TrendObservation> trends) {

        public BehaviorProfileObservation {
            dominantTopics7d = dominantTopics7d == null ? List.of() : List.copyOf(dominantTopics7d);
            trends = trends == null ? List.of() : List.copyOf(trends);
        }
    }

    public record TrendObservation(
            String featureCode,
            String direction,
            BigDecimal recentAverage,
            BigDecimal priorAverage,
            BigDecimal recentCoverage) {
    }
}
