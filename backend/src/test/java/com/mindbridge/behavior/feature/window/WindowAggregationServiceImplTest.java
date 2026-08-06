package com.mindbridge.behavior.feature.window;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import com.mindbridge.behavior.feature.window.dto.WindowAggregationResult;
import com.mindbridge.behavior.feature.window.repository.UserDailyFeatureWindowRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * G4-T06 unit tests for {@link WindowAggregationServiceImpl}.
 *
 * <p>Verifies formula correctness: averages, coverage denominators, max
 * risk, sums, and the empty-user path. Manual fixtures, no DB.
 */
@ExtendWith(MockitoExtension.class)
class WindowAggregationServiceImplTest {

    @Mock UserDailyFeatureWindowRepository featureRepository;
    @Mock UserRepository userRepository;

    WindowAggregationServiceImpl service;

    UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    LocalDate targetDate = LocalDate.of(2026, 8, 4);

    @BeforeEach
    void setUp() {
        service = new WindowAggregationServiceImpl(featureRepository, userRepository);
    }

    private UserDailyFeature row(LocalDate date,
                                  BigDecimal stress, BigDecimal mood, BigDecimal energy,
                                  BigDecimal sleepHours, BigDecimal sleepScore,
                                  BigDecimal anxiety, BigDecimal anxietyConf, String anxietySrc,
                                  BigDecimal engagement, Long messages, Integer checkins,
                                  Integer maxRisk, Integer riskEvents) {
        UserDailyFeature r = new UserDailyFeature();
        r.setUserId(userId);
        r.setFeatureDate(date);
        r.setTimezone("Asia/Ho_Chi_Minh");
        r.setStressScore(stress);
        r.setMoodScore(mood);
        r.setEnergyScore(energy);
        r.setSleepHours(sleepHours);
        r.setSleepScore(sleepScore);
        r.setAnxietySignal(anxiety);
        r.setAnxietySignalConfidence(anxietyConf);
        r.setAnxietySignalSource(anxietySrc);
        r.setEngagementScore(engagement);
        r.setMessageCount(messages);
        r.setCheckinCompletedCount(checkins);
        r.setMaxRiskLevel(maxRisk);
        r.setRiskEventCount(riskEvents);
        return r;
    }

    private User userRegisteredDaysBefore(int days) {
        User u = new User();
        Instant created = targetDate.minusDays(days - 1).atStartOfDay()
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        try {
            Field f = User.class.getDeclaredField("createdAt");
            f.setAccessible(true);
            f.set(u, created);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt", e);
        }
        return u;
    }
    @Test
    @DisplayName("User not found: result has zero coverage and null scores")
    void userNotFound() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        WindowAggregationResult r = service.aggregateForUser(userId, targetDate);

        assertThat(r.userId()).isEqualTo(userId);
        assertThat(r.targetDate()).isEqualTo(targetDate);
        assertThat(r.stressScore7d()).isNull();
        assertThat(r.stressCoverage7d()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.explicitCoverage7d()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.maxRiskLevel7d()).isNull();
    }

    @Test
    @DisplayName("Happy path: 7-day stress average is computed from rows with stress scores")
    void stress7dAverage() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(userRegisteredDaysBefore(30)));
        when(featureRepository.findByUserAndWindow(eq(userId), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(1);
                    if (start.equals(targetDate.minusDays(6))) {
                        return List.of(
                                row(targetDate.minusDays(6), new BigDecimal("2.0"), null, null, null, null, null, null, null, null, null, null, null, null),
                                row(targetDate.minusDays(5), new BigDecimal("4.0"), null, null, null, null, null, null, null, null, null, null, null, null),
                                row(targetDate.minusDays(4), new BigDecimal("6.0"), null, null, null, null, null, null, null, null, null, null, null, null)
                        );
                    }
                    return new ArrayList<>();
                });
        when(featureRepository.countDaysWithExplicitData(eq(userId), any(), any())).thenReturn(3L);

        WindowAggregationResult r = service.aggregateForUser(userId, targetDate);

        assertThat(r.stressScore7d()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(r.stressCoverage7d()).isEqualByComparingTo(new BigDecimal("0.4286"));
    }

    @Test
    @DisplayName("Empty 30-day window: scores null, coverage zero")
    void empty30dWindow() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(userRegisteredDaysBefore(60)));
        when(featureRepository.findByUserAndWindow(eq(userId), any(), any()))
                .thenReturn(new ArrayList<>());
        when(featureRepository.countDaysWithExplicitData(eq(userId), any(), any())).thenReturn(0L);

        WindowAggregationResult r = service.aggregateForUser(userId, targetDate);

        assertThat(r.stressScore30d()).isNull();
        assertThat(r.moodScore30d()).isNull();
        assertThat(r.energyScore30d()).isNull();
        assertThat(r.sleepHoursAvg30d()).isNull();
        assertThat(r.anxietySignal30d()).isNull();
        assertThat(r.engagementScore30d()).isNull();
        assertThat(r.stressCoverage30d()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.maxRiskLevel30d()).isNull();
    }

    @Test
    @DisplayName("Mixed scores: null values are filtered, only non-null contribute to average")
    void mixedScoresFilterNulls() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(userRegisteredDaysBefore(30)));
        when(featureRepository.findByUserAndWindow(eq(userId), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(1);
                    if (start.equals(targetDate.minusDays(6))) {
                        return List.of(
                                row(targetDate.minusDays(6), null, null, null, null, null, null, null, null, null, null, null, null, null),
                                row(targetDate.minusDays(5), new BigDecimal("8.0"), null, null, null, null, null, null, null, null, null, null, null, null),
                                row(targetDate.minusDays(4), new BigDecimal("2.0"), null, null, null, null, null, null, null, null, null, null, null, null)
                        );
                    }
                    return new ArrayList<>();
                });
        when(featureRepository.countDaysWithExplicitData(eq(userId), any(), any())).thenReturn(2L);

        WindowAggregationResult r = service.aggregateForUser(userId, targetDate);

        assertThat(r.stressScore7d()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(r.stressCoverage7d()).isEqualByComparingTo(new BigDecimal("0.2857"));
    }
    @Test
    @DisplayName("New user (1 day registered): denominator is 1, no rows = 0 coverage")
    void newUserDenominator() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(userRegisteredDaysBefore(1)));
        when(featureRepository.findByUserAndWindow(eq(userId), any(), any()))
                .thenReturn(new ArrayList<>());
        when(featureRepository.countDaysWithExplicitData(eq(userId), any(), any())).thenReturn(0L);

        WindowAggregationResult r = service.aggregateForUser(userId, targetDate);

        assertThat(r.stressCoverage7d()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.stressCoverage30d()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Anxiety: signal averaged, source preserved")
    void anxietySourceInferred() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(userRegisteredDaysBefore(30)));
        when(featureRepository.findByUserAndWindow(eq(userId), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(1);
                    if (start.equals(targetDate.minusDays(6))) {
                        return List.of(
                                row(targetDate.minusDays(2), null, null, null, null, null,
                                        new BigDecimal("0.6"), new BigDecimal("0.9"), "INFERRED",
                                        null, null, null, null, null)
                        );
                    }
                    return new ArrayList<>();
                });
        when(featureRepository.countDaysWithExplicitData(eq(userId), any(), any())).thenReturn(0L);

        WindowAggregationResult r = service.aggregateForUser(userId, targetDate);

        assertThat(r.anxietySignal7d()).isEqualByComparingTo(new BigDecimal("0.60"));
        assertThat(r.anxietySource7d()).isEqualTo("CHAT_ANALYSIS");
        assertThat(r.anxietyCoverage7d()).isEqualByComparingTo(new BigDecimal("0.1429"));
    }

    @Test
    @DisplayName("Max risk: highest value wins across all rows")
    void maxRiskHighestWins() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(userRegisteredDaysBefore(30)));
        when(featureRepository.findByUserAndWindow(eq(userId), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(1);
                    if (start.equals(targetDate.minusDays(29))) {
                        return List.of(
                                row(targetDate.minusDays(10), null, null, null, null, null, null, null, null, null, null, null, 1, 1),
                                row(targetDate.minusDays(5), null, null, null, null, null, null, null, null, null, null, null, 3, 1),
                                row(targetDate.minusDays(2), null, null, null, null, null, null, null, null, null, null, null, 2, 1)
                        );
                    }
                    return new ArrayList<>();
                });
        when(featureRepository.countDaysWithExplicitData(eq(userId), any(), any())).thenReturn(0L);

        WindowAggregationResult r = service.aggregateForUser(userId, targetDate);

        assertThat(r.maxRiskLevel30d()).isEqualTo(3);
        assertThat(r.riskEventCount30d()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Sleep: hours and score are averaged separately")
    void sleepBothMetrics() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(userRegisteredDaysBefore(30)));
        when(featureRepository.findByUserAndWindow(eq(userId), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(1);
                    if (start.equals(targetDate.minusDays(6))) {
                        return List.of(
                                row(targetDate.minusDays(3), null, null, null, new BigDecimal("7.0"), new BigDecimal("80"), null, null, null, null, null, null, null, null),
                                row(targetDate.minusDays(2), null, null, null, new BigDecimal("9.0"), new BigDecimal("60"), null, null, null, null, null, null, null, null)
                        );
                    }
                    return new ArrayList<>();
                });
        when(featureRepository.countDaysWithExplicitData(eq(userId), any(), any())).thenReturn(2L);

        WindowAggregationResult r = service.aggregateForUser(userId, targetDate);

        assertThat(r.sleepHoursAvg7d()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(r.sleepScore7d()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("Engagement: message and checkin sums aggregate across days")
    void engagementSums() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(userRegisteredDaysBefore(30)));
        when(featureRepository.findByUserAndWindow(eq(userId), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(1);
                    if (start.equals(targetDate.minusDays(6))) {
                        return List.of(
                                row(targetDate.minusDays(3), null, null, null, null, null, null, null, null, new BigDecimal("0.8"), 10L, 2, null, null),
                                row(targetDate.minusDays(2), null, null, null, null, null, null, null, null, new BigDecimal("0.6"), 20L, 1, null, null),
                                row(targetDate.minusDays(1), null, null, null, null, null, null, null, null, null, 5L, 0, null, null)
                        );
                    }
                    return new ArrayList<>();
                });
        when(featureRepository.countDaysWithExplicitData(eq(userId), any(), any())).thenReturn(0L);

        WindowAggregationResult r = service.aggregateForUser(userId, targetDate);

        assertThat(r.engagementScore7d()).isEqualByComparingTo(new BigDecimal("0.70"));
        assertThat(r.messageCountSum7d()).isEqualTo(35L);
        assertThat(r.checkinCompletedSum7d()).isEqualTo(3L);
    }
}