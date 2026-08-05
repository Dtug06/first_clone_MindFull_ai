package com.mindbridge.behavior.feature.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mindbridge.behavior.feature.trend.dto.StreakInfo;
import com.mindbridge.behavior.feature.trend.dto.TrendEntry;
import com.mindbridge.behavior.feature.trend.dto.TrendSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * G4-T12 API representation of a {@link TrendSummary} for the
 * {@code trendSummary} field of {@link UserBehaviorProfileResponse}.
 *
 * <p>Replaces the previous {@code Map<String,String>} flat representation
 * (G4-T09) with a typed nested object so the dashboard can render
 * {@code direction} / {@code deltaPct} / {@code streakInfo} without
 * parsing strings.
 *
 * <p>The {@code calculationVersion} on every {@link TrendSummary} is
 * {@code "trend_v1"} — bump to {@code "trend_v2"} if the formula changes.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record TrendSummaryResponse(
        UUID userId,
        LocalDate targetDate,
        String zoneId,
        List<TrendEntryResponse> entries,
        StreakInfoResponse streak,
        String dataQuality,
        String calculationVersion) {

    /**
     * Builds an empty-but-valid fallback used when
     * {@code ProfileSnapshot.trendSummaryJson} cannot be deserialized.
     * Keeps the API contract ({@code trendSummary} present) so the
     * dashboard never crashes on a malformed row.
     */
    public static TrendSummaryResponse empty(UUID userId, LocalDate targetDate, String zoneId) {
        return new TrendSummaryResponse(
                userId, targetDate, zoneId, List.of(), null,
                TrendSummary.DATA_QUALITY_PLACEHOLDER, TrendSummary.CALCULATION_VERSION);
    }

    public static TrendSummaryResponse from(TrendSummary source) {
        List<TrendEntryResponse> entries = source.entries().stream()
                .map(TrendEntryResponse::from)
                .toList();
        StreakInfoResponse streak = source.streakInfo() == null
                ? null
                : StreakInfoResponse.from(source.streakInfo());
        return new TrendSummaryResponse(
                source.userId(),
                source.targetDate(),
                source.zoneId() == null ? null : source.zoneId().getId(),
                entries,
                streak,
                source.dataQuality(),
                source.calculationVersion());
    }
}