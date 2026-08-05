package com.mindbridge.behavior.feature.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.mindbridge.behavior.feature.trend.config.TrendConfig;
import com.mindbridge.behavior.feature.trend.dto.TrendDirection;
import com.mindbridge.behavior.feature.trend.dto.TrendEntry;
import com.mindbridge.behavior.feature.trend.dto.TrendReason;
import com.mindbridge.behavior.feature.trend.dto.TrendSummary;
import com.mindbridge.behavior.feature.trend.impl.TrendCalculatorImpl;
import com.mindbridge.behavior.feature.trend.repository.TrendQueryRepository;
import com.mindbridge.behavior.feature.window.WindowAggregationService;
import com.mindbridge.behavior.feature.window.dto.WindowAggregationResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * G4-T07 unit tests for {@link TrendCalculatorImpl}.
 *
 * <p>Covers the 14 cases from the task spec Phase 1 \u00a78 test plan.
 * Uses Mockito for both {@link WindowAggregationService} and
 * {@link TrendQueryRepository} - no Spring context needed (run under
 * Surefire without {@code @SpringBootTest}).
 */
class TrendCalculatorImplTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final ZoneId TZ = ZoneId.of("UTC");
    private static final LocalDate TARGET = LocalDate.of(2026, 8, 4);

    private static final BigDecimal MIN_COV = new BigDecimal("0.50");
    private static final BigDecimal DELTA_TH = new BigDecimal("0.10");
    private static final BigDecimal STRESS_TH = new BigDecimal("0.75");

    private final WindowAggregationService windowSvc = Mockito.mock(WindowAggregationService.class);
    private final TrendQueryRepository trendRepo = Mockito.mock(TrendQueryRepository.class);
    private final TrendCalculatorImpl service = new TrendCalculatorImpl(windowSvc, trendRepo);

    private static final TrendConfig CONFIG =
            TrendConfig.of(MIN_COV, DELTA_TH, STRESS_TH);

    /** Returns a WindowAggregationResult with all-null feature scores/coverage
     *  except for the specific stress fields the test needs. */
    private static WindowAggregationResult wnd(
            BigDecimal stress7, BigDecimal stressCov7,
            BigDecimal stressPrior7, BigDecimal stressPriorCov7) {
        // Empty default = all null. Stress is the only field we exercise in unit tests
        // because the polarity + coverage logic is identical for every feature.
        return new WindowAggregationResult(
                USER, TARGET,
                stress7, null, stressCov7, null, null,                        // stress
                null, null, null, null,                                      // mood
                null, null, null, null,                                      // energy
                null, null, null, null, null, null,                          // sleep
                null, null, null, null, null, null, null, null,              // anxiety
                null, null, null, null, null, null, null, null,              // engagement
                null, null, "NOT_APPLICABLE", "NOT_APPLICABLE",              // exercise
                null, null, null, null, null, null,                          // max_risk
                null, null, null, null);
    }

    private static WindowAggregationResult wndMood(
            BigDecimal mood7, BigDecimal moodCov7,
            BigDecimal moodPrior7, BigDecimal moodPriorCov7) {
        return new WindowAggregationResult(
                USER, TARGET,
                null, null, null, null, null,                                // stress
                mood7, null, moodCov7, null,                                // mood
                null, null, null, null,                                      // energy
                null, null, null, null, null, null,                          // sleep
                null, null, null, null, null, null, null, null,              // anxiety
                null, null, null, null, null, null, null, null,              // engagement
                null, null, "NOT_APPLICABLE", "NOT_APPLICABLE",              // exercise
                null, null, null, null, null, null,                          // max_risk
                null, null, null, null);
    }

    private static WindowAggregationResult wndRisk(
            Integer risk7, BigDecimal riskCov7,
            Integer riskPrior7, BigDecimal riskPriorCov7) {
        return new WindowAggregationResult(
                USER, TARGET,
                null, null, null, null, null,                                // stress
                null, null, null, null,                                      // mood
                null, null, null, null,                                      // energy
                null, null, null, null, null, null,                          // sleep
                null, null, null, null, null, null, null, null,              // anxiety
                null, null, null, null, null, null, null, null,              // engagement
                null, null, "NOT_APPLICABLE", "NOT_APPLICABLE",              // exercise
                risk7, riskPrior7, null, null, riskCov7, riskPriorCov7,      // max_risk
                null, null, null, null);
    }

    private void stubRecent(WindowAggregationResult w) {
        Mockito.when(windowSvc.aggregateForUser(eq(USER), eq(TARGET))).thenReturn(w);
    }

    private void stubPrior(WindowAggregationResult w) {
        Mockito.when(windowSvc.aggregateForUser(eq(USER), eq(TARGET.minusDays(7)))).thenReturn(w);
    }

    private void stubStreak(List<LocalDate> checkIn, List<LocalDate> highStress) {
        Mockito.when(trendRepo.findCheckInDatesByUserInRange(eq(USER), any(), eq(TARGET)))
                .thenReturn(checkIn);
        Mockito.when(trendRepo.findHighStressDatesByUserInRange(
                eq(USER), any(), eq(TARGET), eq(STRESS_TH))).thenReturn(highStress);
    }

    @Test
    @DisplayName("calculationVersion is trend_v1 in every result")
    void calculationVersion_isTrendV1() {
        stubRecent(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
        stubPrior(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
        stubStreak(List.of(), List.of());

        TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);

        assertThat(out.calculationVersion()).isEqualTo("trend_v1");
    }

    @Test
    @DisplayName("dataQuality is TODO_T11_ALIGNED placeholder")
    void dataQuality_isPlaceholder() {
        stubRecent(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
        stubPrior(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
        stubStreak(List.of(), List.of());

        TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);

        assertThat(out.dataQuality()).isEqualTo("TODO_T11_ALIGNED");
    }

    @Test
    @DisplayName("null config threshold throws IllegalStateException")
    void nullConfig_throws() {
        TrendConfig bad = TrendConfig.defaults();
        assertThatThrownBy(() -> service.calculateTrendForUser(USER, TARGET, TZ, bad))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MIN_TREND_COVERAGE");
    }

    @Test
    @DisplayName("null userId/targetDate/zoneId/config throw NPE")
    void nullArgs_throw() {
        assertThatThrownBy(() -> service.calculateTrendForUser(null, TARGET, TZ, CONFIG))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.calculateTrendForUser(USER, null, TZ, CONFIG))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.calculateTrendForUser(USER, TARGET, null, CONFIG))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.calculateTrendForUser(USER, TARGET, TZ, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Nested
    @DisplayName("U-1..U-3 — UP / DOWN / STABLE on stress (HIGHER_IS_WORSE)")
    class StressTrend {

        @Test
        void up_stressIncreases_isDown_inPolarityHIGHER_IS_WORSE() {
            // recent=0.75 prior=0.25: stress got worse. Polarity is WORSE -> direction = DOWN.
            BigDecimal recent = new BigDecimal("0.75");
            BigDecimal prior = new BigDecimal("0.25");
            stubRecent(wnd(recent, BigDecimal.ONE, prior, BigDecimal.ONE));
            stubPrior(wnd(prior, BigDecimal.ONE, recent, BigDecimal.ONE));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            TrendEntry stress = findEntry(out, "stress");

            // delta = (0.75 - 0.25) / 0.25 = 2.0  -> > 0.10
            // polarity HIGHER_IS_WORSE -> positive delta = DOWN
            assertThat(stress.direction()).isEqualTo(TrendDirection.DOWN);
            assertThat(stress.reason()).isEqualTo(TrendReason.SUFFICIENT_DATA);
            assertThat(stress.deltaPct()).isEqualByComparingTo("2.0000");
        }

        @Test
        void down_stressDecreases_isUp_inPolarityHIGHER_IS_WORSE() {
            // recent=0.25 prior=0.75: stress got better. Polarity WORSE -> UP.
            BigDecimal recent = new BigDecimal("0.25");
            BigDecimal prior = new BigDecimal("0.75");
            stubRecent(wnd(recent, BigDecimal.ONE, prior, BigDecimal.ONE));
            stubPrior(wnd(prior, BigDecimal.ONE, recent, BigDecimal.ONE));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            TrendEntry stress = findEntry(out, "stress");

            // delta = (0.25 - 0.75) / 0.75 = -0.6667
            // polarity HIGHER_IS_WORSE -> negative delta = UP
            assertThat(stress.direction()).isEqualTo(TrendDirection.UP);
            assertThat(stress.reason()).isEqualTo(TrendReason.SUFFICIENT_DATA);
            assertThat(stress.deltaPct()).isEqualByComparingTo("-0.6667");
        }

        @Test
        void stable_stressWithinThreshold() {
            // recent=0.51 prior=0.50: delta = 0.02 / 0.50 = 0.04 < 0.10 -> STABLE
            BigDecimal recent = new BigDecimal("0.51");
            BigDecimal prior = new BigDecimal("0.50");
            stubRecent(wnd(recent, BigDecimal.ONE, prior, BigDecimal.ONE));
            stubPrior(wnd(prior, BigDecimal.ONE, recent, BigDecimal.ONE));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            TrendEntry stress = findEntry(out, "stress");

            assertThat(stress.direction()).isEqualTo(TrendDirection.STABLE);
            assertThat(stress.reason()).isEqualTo(TrendReason.SUFFICIENT_DATA);
        }
    }

    @Nested
    @DisplayName("U-4..U-8 — coverage / null / zero-prior branches")
    class CoverageAndNull {

        @Test
        void recentCoverage_belowThreshold_isUnknown() {
            // recent coverage 0.30 < 0.50 threshold
            stubRecent(wnd(new BigDecimal("0.50"), new BigDecimal("0.30"),
                           new BigDecimal("0.50"), BigDecimal.ONE));
            stubPrior(wnd(new BigDecimal("0.50"), BigDecimal.ONE,
                           new BigDecimal("0.50"), new BigDecimal("0.30")));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            assertThat(findEntry(out, "stress").direction()).isEqualTo(TrendDirection.UNKNOWN);
            assertThat(findEntry(out, "stress").reason())
                    .isEqualTo(TrendReason.INSUFFICIENT_RECENT_COVERAGE);
        }

        @Test
        void priorCoverage_belowThreshold_isUnknown() {
            stubRecent(wnd(new BigDecimal("0.50"), BigDecimal.ONE,
                           new BigDecimal("0.50"), new BigDecimal("0.40")));
            stubPrior(wnd(new BigDecimal("0.50"), new BigDecimal("0.40"),
                           new BigDecimal("0.50"), BigDecimal.ONE));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            assertThat(findEntry(out, "stress").direction()).isEqualTo(TrendDirection.UNKNOWN);
            assertThat(findEntry(out, "stress").reason())
                    .isEqualTo(TrendReason.INSUFFICIENT_PRIOR_COVERAGE);
        }

        @Test
        void recentAvgNull_isUnknown_noRecentData() {
            stubRecent(wnd(null, BigDecimal.ONE, new BigDecimal("0.50"), BigDecimal.ONE));
            stubPrior(wnd(new BigDecimal("0.50"), BigDecimal.ONE,
                           new BigDecimal("0.50"), BigDecimal.ONE));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            assertThat(findEntry(out, "stress").direction()).isEqualTo(TrendDirection.UNKNOWN);
            assertThat(findEntry(out, "stress").reason()).isEqualTo(TrendReason.NO_RECENT_DATA);
        }

        @Test
        void priorAvgNull_isUnknown_noPriorData() {
            stubRecent(wnd(new BigDecimal("0.50"), BigDecimal.ONE, null, BigDecimal.ONE));
            stubPrior(wnd(null, BigDecimal.ONE, new BigDecimal("0.50"), BigDecimal.ONE));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            assertThat(findEntry(out, "stress").direction()).isEqualTo(TrendDirection.UNKNOWN);
            assertThat(findEntry(out, "stress").reason()).isEqualTo(TrendReason.NO_PRIOR_DATA);
        }

        @Test
        void priorAvgZero_isUnknown_noPriorData_noDivision() {
            // recent=0.50 prior=0.00 -> delta undefined (divide by zero) -> UNKNOWN NO_PRIOR_DATA
            stubRecent(wnd(new BigDecimal("0.50"), BigDecimal.ONE,
                           BigDecimal.ZERO, BigDecimal.ONE));
            stubPrior(wnd(BigDecimal.ZERO, BigDecimal.ONE,
                           new BigDecimal("0.50"), BigDecimal.ONE));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            TrendEntry stress = findEntry(out, "stress");
            assertThat(stress.direction()).isEqualTo(TrendDirection.UNKNOWN);
            assertThat(stress.reason()).isEqualTo(TrendReason.NO_PRIOR_DATA);
            assertThat(stress.deltaPct()).isNull();
        }
    }

    @Nested
    @DisplayName("U-9 — exercise_completion is always NOT_APPLICABLE")
    class ExerciseCompletion {

        @Test
        void exerciseCompletion_isAlwaysNotApplicable_unknown() {
            stubRecent(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
            stubPrior(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            TrendEntry exercise = findEntry(out, "exercise_completion");

            assertThat(exercise.direction()).isEqualTo(TrendDirection.UNKNOWN);
            assertThat(exercise.reason()).isEqualTo(TrendReason.NOT_APPLICABLE);
        }
    }

    @Nested
    @DisplayName("U-10 / U-11 — polarity inversions for mood (BETTER) and max_risk (WORSE)")
    class Polarity {

        @Test
        void mood_increase_isUp_BETTER() {
            // mood recent=0.75 prior=0.25, delta>0 -> UP (positive = better)
            BigDecimal recent = new BigDecimal("0.75");
            BigDecimal prior = new BigDecimal("0.25");
            stubRecent(wndMood(recent, BigDecimal.ONE, prior, BigDecimal.ONE));
            stubPrior(wndMood(prior, BigDecimal.ONE, recent, BigDecimal.ONE));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            assertThat(findEntry(out, "mood").direction()).isEqualTo(TrendDirection.UP);
        }

        @Test
        void maxRisk_decrease_isUp_WORSE() {
            // risk recent=1 prior=3, delta = (1-3)/3 = -0.6667 -> polarity WORSE -> UP
            // (because for WORSE-polarity, a negative delta means things got better)
            stubRecent(wndRisk(1, BigDecimal.ONE, 3, BigDecimal.ONE));
            stubPrior(wndRisk(3, BigDecimal.ONE, 1, BigDecimal.ONE));
            stubStreak(List.of(), List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            TrendEntry risk = findEntry(out, "max_risk");
            assertThat(risk.direction()).isEqualTo(TrendDirection.UP);
            assertThat(risk.deltaPct()).isEqualByComparingTo("-0.6667");
        }
    }

    @Nested
    @DisplayName("U-12..U-14 — streak computation")
    class StreakTests {

        @Test
        void checkInStreak_fiveConsecutiveDays() {
            stubRecent(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
            stubPrior(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
            List<LocalDate> dates = List.of(
                    TARGET,
                    TARGET.minusDays(1),
                    TARGET.minusDays(2),
                    TARGET.minusDays(3),
                    TARGET.minusDays(4));
            stubStreak(dates, List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            assertThat(out.streakInfo().checkInStreak()).isEqualTo(5);
            assertThat(out.streakInfo().lastCheckInDate()).isEqualTo(TARGET);
        }

        @Test
        void checkInStreak_brokenByGap_returnsTwo() {
            stubRecent(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
            stubPrior(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
            // gap: target + D-1 present, D-2 missing
            List<LocalDate> dates = List.of(TARGET, TARGET.minusDays(1), TARGET.minusDays(3));
            stubStreak(dates, List.of());

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            assertThat(out.streakInfo().checkInStreak()).isEqualTo(2);
        }

        @Test
        void highStressStreak_fiveConsecutiveDays() {
            stubRecent(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
            stubPrior(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
            stubStreak(List.of(), List.of(
                    TARGET,
                    TARGET.minusDays(1),
                    TARGET.minusDays(2),
                    TARGET.minusDays(3),
                    TARGET.minusDays(4)));

            TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);
            assertThat(out.streakInfo().highStressStreak()).isEqualTo(5);
            assertThat(out.streakInfo().lastHighStressDate()).isEqualTo(TARGET);
        }
    }

    @Test
    @DisplayName("entry list has all 8 MVP features")
    void allEightFeaturesPresent() {
        stubRecent(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
        stubPrior(wnd(null, BigDecimal.ZERO, null, BigDecimal.ZERO));
        stubStreak(List.of(), List.of());

        TrendSummary out = service.calculateTrendForUser(USER, TARGET, TZ, CONFIG);

        assertThat(out.entries()).extracting(TrendEntry::featureCode)
                .containsExactlyInAnyOrder(
                        "stress", "mood", "energy", "sleep",
                        "anxiety_signal", "engagement", "exercise_completion", "max_risk");
    }

    private static TrendEntry findEntry(TrendSummary summary, String code) {
        return summary.entries().stream()
                .filter(e -> e.featureCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing entry for " + code));
    }
}