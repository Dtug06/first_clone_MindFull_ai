package com.mindbridge.behavior.feature.profile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeatureSeriesPoint(
        LocalDate date,
        BigDecimal value,
        FeatureSource source,
        BigDecimal confidence
) {

    public static FeatureSeriesPoint explicit(LocalDate date, BigDecimal value) {
        return new FeatureSeriesPoint(date, value, FeatureSource.EXPLICIT, null);
    }

    public static FeatureSeriesPoint noData(LocalDate date) {
        return new FeatureSeriesPoint(date, null, FeatureSource.NONE, null);
    }
}