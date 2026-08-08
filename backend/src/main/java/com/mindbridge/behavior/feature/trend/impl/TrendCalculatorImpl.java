package com.mindbridge.behavior.feature.trend.impl;

import com.mindbridge.behavior.feature.trend.TrendCalculator;
import com.mindbridge.behavior.feature.trend.config.TrendConfig;
import com.mindbridge.behavior.feature.trend.dto.StreakInfo;
import com.mindbridge.behavior.feature.trend.dto.TrendDirection;
import com.mindbridge.behavior.feature.trend.dto.TrendEntry;
import com.mindbridge.behavior.feature.trend.dto.TrendReason;
import com.mindbridge.behavior.feature.trend.dto.TrendSummary;
import com.mindbridge.behavior.feature.trend.repository.TrendQueryRepository;
import com.mindbridge.behavior.feature.window.WindowAggregationService;
import com.mindbridge.behavior.feature.window.dto.WindowAggregationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * G4-T07 default implementation.
 *
 * <p>Algorithm overview (per docs/tasks/G4/G4-T07-trend-streak-calculator.md
 * Phase 1 \u00a71-7):
 * <ol>
 *   <li>Validate config (fail-fast on null thresholds).</li>
 *   <li>Call {@link WindowAggregationService} twice: once for
 *       {@code targetDate} (recent window) and once for
 *       {@code targetDate.minusDays(7)} (prior window).</li>
 *   <li>For each of the 8 features, compute a {@link TrendEntry} using the
 *       polarity table (HIGHER_IS_BETTER / HIGHER_IS_WORSE / HIGHER_IS_MORE)
 *       and the coverage gate.</li>
 *   <li>Query {@link TrendQueryRepository} for check-in dates and
 *       high-stress dates; compute {@link StreakInfo}.</li>
 *   <li>Build {@link TrendSummary} with {@code calculationVersion=trend_v1}
 *       and {@code dataQuality=TODO_T11_ALIGNED}.</li>
 * </ol>
 *
 * <p>NOTE on naming: this class uses {@code statisticalBaselineWindow} /
 * {@code priorWindow} for the 7-day window immediately preceding the recent
 * 7-day window. This is a STATISTICAL concept and must NOT be confused with
 * the CBT Program State {@code BASELINE} (docs/04_SAFETY_AND_CBT_RULES.md
 * \u00a716, used by G5 program runtime).
 */
@Service
public class TrendCalculatorImpl implements TrendCalculator {

    private static final Logger log = LoggerFactory.getLogger(TrendCalculatorImpl.class);

    /** Recent window size (per Phase 1 \u00a71 and FEATURE_DICTIONARY \u00a79.3). */
    static final int WINDOW_DAYS = 7;
    /** Streak cap (Phase 1 Q4 default; configurable later). */
    static final int STREAK_CAP_DAYS = 30;
    /** Rounding for delta_pct and coverage (4 decimals, HALF_UP, mirror T06). */
    private static final int ROUND_SCALE = 4;

    private final WindowAggregationService windowAggregationService;
    private final TrendQueryRepository trendQueryRepository;

    public TrendCalculatorImpl(WindowAggregationService windowAggregationService,
                               TrendQueryRepository trendQueryRepository) {
        this.windowAggregationService = windowAggregationService;
        this.trendQueryRepository = trendQueryRepository;
        log.info("TrendCalculator initialized, calculationVersion={}",
                TrendSummary.CALCULATION_VERSION);
    }

    @Override
    @Transactional(readOnly = true)
    public TrendSummary calculateTrendForUser(UUID userId, LocalDate targetDate, ZoneId zoneId,
                                              TrendConfig config) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(targetDate, "targetDate");
        Objects.requireNonNull(zoneId, "zoneId");
        Objects.requireNonNull(config, "config");
        validateConfig(config);

        // Window math: recent = [targetDate-6..targetDate] (inclusive),
        // prior (statistical baseline) = [targetDate-13..targetDate-7] (inclusive).
        WindowAggregationResult recent = windowAggregationService.aggregateForUser(userId, targetDate);
        WindowAggregationResult priorWindow =
                windowAggregationService.aggregateForUser(userId, targetDate.minusDays(WINDOW_DAYS));

        List<TrendEntry> entries = new ArrayList<>(8);
        entries.add(buildEntry("stress", TrendPolarity.HIGHER_IS_WORSE,
                recent.stressScore7d(), recent.stressCoverage7d(),
                priorWindow.stressScore7d(), priorWindow.stressCoverage7d(), config));
        entries.add(buildEntry("mood", TrendPolarity.HIGHER_IS_BETTER,
                recent.moodScore7d(), recent.moodCoverage7d(),
                priorWindow.moodScore7d(), priorWindow.moodCoverage7d(), config));
        entries.add(buildEntry("energy", TrendPolarity.HIGHER_IS_BETTER,
                recent.energyScore7d(), recent.energyCoverage7d(),
                priorWindow.energyScore7d(), priorWindow.energyCoverage7d(), config));
        // G4-FIXUP-2026-08-08: sleep_quality template is not yet shipped (T04
        // sleep_quality_v1 formula is TODO_EXPERT_REVIEW), so `sleep_score`
        // is always NULL even when `sleep_hours` has data. To avoid a
        // permanent UNKNOWN trend for sleep, fall back to `sleepHoursAvg*`
        // (raw user duration average — non-clinical proxy documented as
        // calculation_version "sleep_hours_v1_proxy"). Switch back to
        // sleepScore7d once sleep_quality_v1 ships.
        entries.add(buildEntry("sleep", TrendPolarity.HIGHER_IS_BETTER,
                recent.sleepHoursAvg7d(), recent.sleepCoverage7d(),
                priorWindow.sleepHoursAvg7d(), priorWindow.sleepCoverage7d(), config));
        entries.add(buildEntry("anxiety_signal", TrendPolarity.HIGHER_IS_WORSE,
                recent.anxietySignal7d(), recent.anxietyCoverage7d(),
                priorWindow.anxietySignal7d(), priorWindow.anxietyCoverage7d(), config));
        entries.add(buildEntry("engagement", TrendPolarity.HIGHER_IS_MORE,
                recent.engagementScore7d(), recent.engagementCoverage7d(),
                priorWindow.engagementScore7d(), priorWindow.engagementCoverage7d(), config));
        entries.add(buildEntry("exercise_completion", TrendPolarity.HIGHER_IS_BETTER,
                null, BigDecimal.ZERO, null, BigDecimal.ZERO, config));
        entries.add(buildEntry("max_risk", TrendPolarity.HIGHER_IS_WORSE,
                recent.maxRiskLevel7d() == null ? null : new BigDecimal(recent.maxRiskLevel7d()),
                recent.maxRiskCoverage7d(),
                priorWindow.maxRiskLevel7d() == null ? null : new BigDecimal(priorWindow.maxRiskLevel7d()),
                priorWindow.maxRiskCoverage7d(), config));

        StreakInfo streakInfo = computeStreak(userId, targetDate, config);

        return new TrendSummary(
                userId, targetDate, zoneId,
                List.copyOf(entries),
                streakInfo,
                TrendSummary.DATA_QUALITY_PLACEHOLDER,
                TrendSummary.CALCULATION_VERSION);
    }

    private void validateConfig(TrendConfig config) {
        if (config.getMinTrendCoverage() == null) {
            throw new IllegalStateException(
                    "MIN_TREND_COVERAGE is not configured - inject via TrendConfig before "
                            + "calling calculateTrendForUser (see FEATURE_DICTIONARY \u00a710.1).");
        }
        if (config.getTrendDeltaThreshold() == null) {
            throw new IllegalStateException(
                    "TREND_DELTA_THRESHOLD is not configured - inject via TrendConfig before "
                            + "calling calculateTrendForUser (see FEATURE_DICTIONARY \u00a710.1).");
        }
        if (config.getHighStressThreshold() == null) {
            throw new IllegalStateException(
                    "HIGH_STRESS_THRESHOLD is not configured - inject via TrendConfig before "
                            + "calling calculateTrendForUser (see FEATURE_DICTIONARY \u00a710.1).");
        }
    }

    private TrendEntry buildEntry(String featureCode, TrendPolarity polarity,
                                  BigDecimal recentAvg, BigDecimal recentCoverage,
                                  BigDecimal priorAvg, BigDecimal priorCoverage,
                                  TrendConfig config) {
        // G5-not-shipped: exercise_completion is always NOT_APPLICABLE per
        // FEATURE_DICTIONARY \u00a76.7.4 (do not fabricate a 0). This check MUST
        // run BEFORE the coverage gate so an exercise_completion entry never
        // surfaces INSUFFICIENT_*_COVERAGE on a fresh user.
        if ("exercise_completion".equals(featureCode)) {
            return new TrendEntry(featureCode, TrendDirection.UNKNOWN, null,
                    TrendReason.NOT_APPLICABLE, recentAvg, priorAvg, recentCoverage, priorCoverage);
        }
        if (polarity == null) {
            // Defensive: any future feature added without a polarity must NOT
            // invent a direction.
            return new TrendEntry(featureCode, TrendDirection.UNKNOWN, null,
                    TrendReason.NOT_APPLICABLE, recentAvg, priorAvg, recentCoverage, priorCoverage);
        }

        BigDecimal minCov = config.getMinTrendCoverage();

        // Coverage gate: both windows must meet threshold for a meaningful trend.
        if (recentCoverage == null || recentCoverage.compareTo(minCov) < 0) {
            return new TrendEntry(featureCode, TrendDirection.UNKNOWN, null,
                    TrendReason.INSUFFICIENT_RECENT_COVERAGE,
                    recentAvg, priorAvg, recentCoverage, priorCoverage);
        }
        if (priorCoverage == null || priorCoverage.compareTo(minCov) < 0) {
            return new TrendEntry(featureCode, TrendDirection.UNKNOWN, null,
                    TrendReason.INSUFFICIENT_PRIOR_COVERAGE,
                    recentAvg, priorAvg, recentCoverage, priorCoverage);
        }
        if (recentAvg == null) {
            return new TrendEntry(featureCode, TrendDirection.UNKNOWN, null,
                    TrendReason.NO_RECENT_DATA,
                    recentAvg, priorAvg, recentCoverage, priorCoverage);
        }
        if (priorAvg == null || priorAvg.signum() == 0) {
            // prior == 0 is a real value but prevents meaningful ratio comparison;
            // treat the same as "no baseline" rather than dividing by 0.
            return new TrendEntry(featureCode, TrendDirection.UNKNOWN, null,
                    TrendReason.NO_PRIOR_DATA,
                    recentAvg, priorAvg, recentCoverage, priorCoverage);
        }

        BigDecimal delta = recentAvg.subtract(priorAvg)
                .divide(priorAvg, ROUND_SCALE, RoundingMode.HALF_UP);
        BigDecimal threshold = config.getTrendDeltaThreshold();
        BigDecimal absDelta = delta.abs();

        TrendDirection direction;
        if (absDelta.compareTo(threshold) <= 0) {
            direction = TrendDirection.STABLE;
        } else if (delta.signum() > 0) {
            direction = polarity.upMeansBetter() ? TrendDirection.UP : TrendDirection.DOWN;
        } else {
            direction = polarity.upMeansBetter() ? TrendDirection.DOWN : TrendDirection.UP;
        }

        return new TrendEntry(featureCode, direction, delta, TrendReason.SUFFICIENT_DATA,
                recentAvg, priorAvg, recentCoverage, priorCoverage);
    }

    private StreakInfo computeStreak(UUID userId, LocalDate targetDate, TrendConfig config) {
        LocalDate streakFrom = targetDate.minusDays(STREAK_CAP_DAYS - 1L);

        List<LocalDate> checkInDates =
                trendQueryRepository.findCheckInDatesByUserInRange(userId, streakFrom, targetDate);
        int checkInStreak = consecutiveDaysBackFrom(checkInDates, targetDate);
        LocalDate lastCheckIn = checkInDates.isEmpty() ? null : checkInDates.get(0);

        List<LocalDate> highStressDates = config.getHighStressThreshold() == null
                ? List.of()
                : trendQueryRepository.findHighStressDatesByUserInRange(
                        userId, streakFrom, targetDate, config.getHighStressThreshold());
        int highStressStreak = consecutiveDaysBackFrom(highStressDates, targetDate);
        LocalDate lastHighStress = highStressDates.isEmpty() ? null : highStressDates.get(0);

        return new StreakInfo(checkInStreak, highStressStreak,
                lastCheckIn, lastHighStress, STREAK_CAP_DAYS);
    }

    /**
     * Counts consecutive days, starting from {@code targetDate} and walking
     * backward, that are present in {@code datesDesc} (sorted DESC by caller).
     */
    static int consecutiveDaysBackFrom(List<LocalDate> datesDesc, LocalDate targetDate) {
        if (datesDesc == null || datesDesc.isEmpty()) return 0;
        int streak = 0;
        LocalDate expected = targetDate;
        for (LocalDate d : datesDesc) {
            if (!d.equals(expected)) {
                // gap: streak broken
                return streak;
            }
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }

    /** Internal polarity enum for trend interpretation. */
    enum TrendPolarity {
        HIGHER_IS_BETTER(true),
        HIGHER_IS_WORSE(false),
        HIGHER_IS_MORE(true);

        private final boolean upMeansBetter;

        TrendPolarity(boolean upMeansBetter) {
            this.upMeansBetter = upMeansBetter;
        }

        boolean upMeansBetter() {
            return upMeansBetter;
        }
    }

    // Suppress unused warnings - reserved for future enrichment (e.g. featureCode -> rawValue lookup)
    @SuppressWarnings("unused")
    private static <T> T noop(Function<T, T> f) { return f.apply(null); }
}