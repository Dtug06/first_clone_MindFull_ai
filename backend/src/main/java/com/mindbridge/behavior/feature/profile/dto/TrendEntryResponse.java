package com.mindbridge.behavior.feature.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mindbridge.behavior.feature.trend.dto.TrendDirection;
import com.mindbridge.behavior.feature.trend.dto.TrendEntry;
import com.mindbridge.behavior.feature.trend.dto.TrendReason;
import java.math.BigDecimal;

/**
 * G4-T12 API representation of a {@link TrendEntry}.
 *
 * <p>{@code direction} and {@code reason} are exposed as enums (serialized
 * as strings via Jackson), {@code deltaPct} as a ratio (NOT a percentage
 * string), {@code recentAvg} / {@code priorAvg} as BigDecimal scores.
 *
 * <p>{@code null} vs {@code 0} semantics are preserved:
 * <ul>
 *   <li>{@code recentAvg == null} 뿯↽ no data in the recent window (do NOT
 *       coerce to {@code 0}).</li>
 *   <li>{@code priorAvg == null} or {@code priorAvg == 0} 뿯↽
 *       {@code direction = UNKNOWN}, {@code reason = NO_PRIOR_DATA}
 *       (avoids divide-by-zero in delta math).</li>
 *   <li>{@code deltaPct == null} when undefined (prior == 0/null/UNKNOWN).</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record TrendEntryResponse(
        String featureCode,
        TrendDirection direction,
        BigDecimal deltaPct,
        TrendReason reason,
        BigDecimal recentAvg,
        BigDecimal priorAvg,
        BigDecimal recentCoverage,
        BigDecimal priorCoverage) {

    public static TrendEntryResponse from(TrendEntry source) {
        return new TrendEntryResponse(
                source.featureCode(),
                source.direction(),
                source.deltaPct(),
                source.reason(),
                source.recentAvg(),
                source.priorAvg(),
                source.recentCoverage(),
                source.priorCoverage());
    }
}