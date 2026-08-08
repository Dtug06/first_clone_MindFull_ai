package com.mindbridge.behavior.feature.job.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.FeatureSource;
import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserDailyFeatureMapperTest {

    private final UserDailyFeatureMapper mapper = UserDailyFeatureMapper.INSTANCE;

    @Test
    void preservesSchemaVersionStringsAndTypedValues() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 6);
        DailyFeatureResult result = result(userId, date, 7L);
        UserDailyFeature target = new UserDailyFeature();

        mapper.toEntity(
                result,
                UUID.randomUUID(),
                date,
                "Asia/Ho_Chi_Minh",
                OffsetDateTime.parse("2026-08-06T12:00:00Z"),
                target);

        assertThat(target.getFeatureVersion()).isEqualTo("feature_dictionary_v1");
        assertThat(target.getCalculationVersion()).isEqualTo("composite_v1");
        assertThat(target.getStressScoreCalculationVersion()).isEqualTo("normalization_v1");
        assertThat(target.getMoodRawValue()).isEqualTo("4");
        assertThat(target.getSleepQualityRaw()).isEqualTo(3);
        assertThat(target.getMessageCount()).isEqualTo(7);
        assertThat(target.getMaxRiskLevel()).isEqualTo((short) 2);
    }

    @Test
    void rejectsDailyCountsOutsideDatabaseIntegerRange() {
        DailyFeatureResult result = result(
                UUID.randomUUID(),
                LocalDate.of(2026, 8, 6),
                (long) Integer.MAX_VALUE + 1L);

        assertThatThrownBy(() -> mapper.toEntity(
                result,
                UUID.randomUUID(),
                result.featureDate(),
                "UTC",
                OffsetDateTime.parse("2026-08-06T12:00:00Z"),
                new UserDailyFeature()))
                .isInstanceOf(ArithmeticException.class);
    }

    private DailyFeatureResult result(UUID userId, LocalDate date, Long messageCount) {
        return new DailyFeatureResult(
                userId,
                date,
                "Asia/Ho_Chi_Minh",
                new DailyFeatureResult.StressResult(
                        new BigDecimal("0.500"), new BigDecimal("3"),
                        FeatureSource.DAILY_ANSWER, "normalization_v1"),
                new DailyFeatureResult.MoodResult(
                        new BigDecimal("0.750"), "4",
                        FeatureSource.DAILY_ANSWER, "normalization_v1"),
                new DailyFeatureResult.EnergyResult(
                        new BigDecimal("0.500"), new BigDecimal("3"),
                        FeatureSource.DAILY_ANSWER, "normalization_v1"),
                new DailyFeatureResult.SleepResult(
                        null, new BigDecimal("7.5"), 3,
                        FeatureSource.DAILY_ANSWER, "sleep_quality_v1"),
                new DailyFeatureResult.AnxietySignalResult(
                        null, null, FeatureSource.NONE, "anxiety_v1", null),
                new DailyFeatureResult.EngagementResult(
                        null, messageCount, 1L, 4L, 3L,
                        new BigDecimal("0.750"), FeatureSource.BEHAVIORAL,
                        "engagement_v1"),
                new DailyFeatureResult.ExerciseCompletionResult(
                        null,
                        DailyFeatureResult.ExerciseCompletionResult.ExerciseCompletionStatus.NOT_APPLICABLE,
                        FeatureSource.NONE,
                        "exercise_v1"),
                new DailyFeatureResult.MaxRiskResult(
                        (short) 2, 1, FeatureSource.SAFETY_DERIVED, "risk_v1"),
                BigDecimal.ONE,
                null,
                Set.of(),
                "feature_dictionary_v1",
                "composite_v1");
    }
}
