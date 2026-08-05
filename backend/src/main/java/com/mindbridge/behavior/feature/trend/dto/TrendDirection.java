package com.mindbridge.behavior.feature.trend.dto;

/**
 * G4-T07 trend direction for a single feature.
 *
 * <p>Designed to be self-explanatory in JSON for the dashboard. The literal
 * values are API-stable; renaming requires a {@code trend_calculation_version}
 * bump.
 */
public enum TrendDirection {
    UP,
    DOWN,
    STABLE,
    UNKNOWN
}