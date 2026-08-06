package com.mindbridge.behavior.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.behavior.feature.config.FeatureConfig;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.ExerciseCompletionResult;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
import com.mindbridge.behavior.feature.dto.FeatureSource;
import com.mindbridge.behavior.feature.dto.FeatureSourceFlag;
import com.mindbridge.behavior.feature.impl.FeatureCalculationServiceImpl;
import com.mindbridge.dailyquestion.domain.AnswerType;
import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import com.mindbridge.safety.resolver.RiskStateHistoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FeatureCalculationServiceImplTest {

    private final FeatureCalculationServiceImpl service =
            new FeatureCalculationServiceImpl(Mockito.mock(RiskStateHistoryRepository.class));

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String TZ = "UTC";
    private static final LocalDate DATE = LocalDate.of(2026, 8, 4);
    private static final OffsetDateTime WINDOW_START = DATE.atStartOfDay().atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime WINDOW_END = DATE.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

    @Nested
    @DisplayName("DoD scenario 1 \u2014 explicit only")
    class ExplicitOnly {

        @Test
        void explicitAnswers_produceScores_sourcesAreDailyAnswer() {
            List<DailySourceAggregation.ExplicitAnswer> answers = List.of(
                    explicitNumeric("STRESS", new BigDecimal("4")),
                    explicitOption("MOOD", "4"),
                    explicitNumeric("ENERGY", new BigDecimal("3")),
                    explicitNumeric("SLEEP", new BigDecimal("7.5")));
            DailySourceAggregation source = aggregation(
                    answers,
                    Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.stress().score()).isEqualByComparingTo("0.750");
            assertThat(out.stress().source()).isEqualTo(FeatureSource.DAILY_ANSWER);
            assertThat(out.stress().rawValue()).isEqualByComparingTo("4");
            assertThat(out.mood().score()).isEqualByComparingTo("0.750");
            assertThat(out.mood().source()).isEqualTo(FeatureSource.DAILY_ANSWER);
            assertThat(out.energy().score()).isEqualByComparingTo("0.500");
            assertThat(out.energy().source()).isEqualTo(FeatureSource.DAILY_ANSWER);
            assertThat(out.sleep().durationHours()).isEqualByComparingTo("7.50");
            assertThat(out.sleep().score()).isNull();
            assertThat(out.sleep().source()).isEqualTo(FeatureSource.DAILY_ANSWER);
        }

        @Test
        void explicitCoverage_isOne_whenAllFourFeaturesPresent() {
            List<DailySourceAggregation.ExplicitAnswer> answers = List.of(
                    explicitNumeric("STRESS", new BigDecimal("4")),
                    explicitOption("MOOD", "4"),
                    explicitNumeric("ENERGY", new BigDecimal("3")),
                    explicitNumeric("SLEEP", new BigDecimal("7.5")));
            DailySourceAggregation source = aggregation(
                    answers, Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.explicitCoverage()).isEqualByComparingTo("1.000");
        }

        @Test
        void explicitCoverage_isHalf_whenTwoOfFourPresent() {
            List<DailySourceAggregation.ExplicitAnswer> answers = List.of(
                    explicitNumeric("STRESS", new BigDecimal("4")),
                    explicitOption("MOOD", "4"));
            DailySourceAggregation source = aggregation(
                    answers, Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.explicitCoverage()).isEqualByComparingTo("0.500");
        }
    }

    @Nested
    @DisplayName("DoD scenario 2 \u2014 inferred only")
    class InferredOnly {

        @Test
        void inferredChats_belowFloor_doNotContribute() {
            DailySourceAggregation.EffectiveChatAnalysis chat = new DailySourceAggregation.EffectiveChatAnalysis(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                    "ANXIETY", "ANXIOUS", "VENT", (short) 3, new BigDecimal("0.50"));
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(),
                    List.of(chat),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            FeatureConfig cfg = FeatureConfig.of(new BigDecimal("0.80"));
            DailyFeatureResult out = service.calculateForDay(source, cfg);

            assertThat(out.anxietySignal().score()).isNull();
            assertThat(out.anxietySignal().confidence()).isNull();
            assertThat(out.anxietySignal().source()).isEqualTo(FeatureSource.NONE);
            assertThat(out.anxietySignal().analysisResultId()).isNull();
        }

        @Test
        void inferredChats_meetFloor_companionMetadataPopulated() {
            DailySourceAggregation.EffectiveChatAnalysis chat = new DailySourceAggregation.EffectiveChatAnalysis(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                    "ANXIETY", "ANXIOUS", "VENT", (short) 3, new BigDecimal("0.85"));
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(),
                    List.of(chat),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            FeatureConfig cfg = FeatureConfig.of(new BigDecimal("0.80"));
            DailyFeatureResult out = service.calculateForDay(source, cfg);

            assertThat(out.anxietySignal().score()).isNull();
            assertThat(out.anxietySignal().confidence()).isEqualByComparingTo("0.850");
            assertThat(out.anxietySignal().source()).isEqualTo(FeatureSource.INFERRED);
            assertThat(out.anxietySignal().analysisResultId()).isEqualTo(chat.analysisResultId());
        }

        @Test
        void inferredConfidence_isMaxOfContributingRows() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();
            DailySourceAggregation.EffectiveChatAnalysis c1 = new DailySourceAggregation.EffectiveChatAnalysis(
                    id1, UUID.randomUUID(), Instant.now(),
                    "ANXIETY", "ANXIOUS", "VENT", (short) 2, new BigDecimal("0.90"));
            DailySourceAggregation.EffectiveChatAnalysis c2 = new DailySourceAggregation.EffectiveChatAnalysis(
                    id2, UUID.randomUUID(), Instant.now(),
                    "ANXIETY", "SAD", "SHARE", (short) 3, new BigDecimal("0.85"));
            DailySourceAggregation.EffectiveChatAnalysis c3 = new DailySourceAggregation.EffectiveChatAnalysis(
                    id3, UUID.randomUUID(), Instant.now(),
                    "ANXIETY", "NEUTRAL", "SHARE", (short) 1, new BigDecimal("0.95"));
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(),
                    List.of(c1, c2, c3),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.anxietySignal().confidence()).isEqualByComparingTo("0.950");
            assertThat(out.anxietySignal().analysisResultId()).isEqualTo(id3);
        }
    }

    @Nested
    @DisplayName("DoD scenario 3 \u2014 both explicit and inferred")
    class ExplicitAndInferred {

        @Test
        void explicitWins_inferredIsRecordedButNotMixedIntoScore() {
            List<DailySourceAggregation.ExplicitAnswer> answers = List.of(
                    explicitNumeric("STRESS", new BigDecimal("4")));
            DailySourceAggregation.EffectiveChatAnalysis chat = new DailySourceAggregation.EffectiveChatAnalysis(
                    UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                    "ANXIETY", "DISTRESS", "VENT", (short) 4, new BigDecimal("0.95"));
            DailySourceAggregation source = aggregation(
                    answers, List.of(chat),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.stress().score()).isEqualByComparingTo("0.750");
            assertThat(out.stress().source()).isEqualTo(FeatureSource.DAILY_ANSWER);
            assertThat(out.sourceFlags())
                    .contains(FeatureSourceFlag.EXPLICIT_USED)
                    .contains(FeatureSourceFlag.INFERRED_USED);
        }
    }

    @Nested
    @DisplayName("DoD scenario 4 \u2014 missing data (must be null, NOT 0)")
    class MissingData {

        @Test
        void noAnswers_noChats_noEvents_returnsNullForEverything() {
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.stress().score()).isNull();
            assertThat(out.mood().score()).isNull();
            assertThat(out.energy().score()).isNull();
            assertThat(out.sleep().score()).isNull();
            assertThat(out.sleep().durationHours()).isNull();
            assertThat(out.anxietySignal().score()).isNull();
            assertThat(out.anxietySignal().confidence()).isNull();
            assertThat(out.engagement().score()).isNull();
            assertThat(out.exerciseCompletion().ratio()).isNull();
            assertThat(out.maxRisk().riskLevel()).isNull();

            assertThat(out.stress().source()).isEqualTo(FeatureSource.NONE);
            assertThat(out.mood().source()).isEqualTo(FeatureSource.NONE);
            assertThat(out.energy().source()).isEqualTo(FeatureSource.NONE);
            assertThat(out.sleep().source()).isEqualTo(FeatureSource.NONE);
            assertThat(out.anxietySignal().source()).isEqualTo(FeatureSource.NONE);
            assertThat(out.engagement().source()).isEqualTo(FeatureSource.BEHAVIORAL);
            assertThat(out.exerciseCompletion().source()).isEqualTo(FeatureSource.NONE);
            assertThat(out.maxRisk().source()).isEqualTo(FeatureSource.NONE);

            assertThat(out.explicitCoverage()).isNull();
            assertThat(out.inferredConfidence()).isNull();
            assertThat(out.sourceFlags()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Out-of-range guards")
    class OutOfRange {

        @Test
        void stressRawAboveFive_returnsNullScoreButKeepsRaw() {
            List<DailySourceAggregation.ExplicitAnswer> answers = List.of(
                    explicitNumeric("STRESS", new BigDecimal("7")));
            DailySourceAggregation source = aggregation(
                    answers, Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.stress().score()).isNull();
            assertThat(out.stress().rawValue()).isEqualByComparingTo("7");
            assertThat(out.stress().source()).isEqualTo(FeatureSource.NONE);
        }

        @Test
        void moodOptionOutOfRange_returnsNullScore() {
            List<DailySourceAggregation.ExplicitAnswer> answers = List.of(
                    explicitOption("MOOD", "7"));
            DailySourceAggregation source = aggregation(
                    answers, Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.mood().score()).isNull();
            assertThat(out.mood().source()).isEqualTo(FeatureSource.NONE);
        }

        @Test
        void sleepDurationAbove24_returnsNull() {
            List<DailySourceAggregation.ExplicitAnswer> answers = List.of(
                    explicitNumeric("SLEEP", new BigDecimal("30")));
            DailySourceAggregation source = aggregation(
                    answers, Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.sleep().durationHours()).isNull();
            assertThat(out.sleep().score()).isNull();
            assertThat(out.sleep().source()).isEqualTo(FeatureSource.NONE);
        }
    }

    @Nested
    @DisplayName("Version constants & no hard-coded threshold")
    class Versioning {

        @Test
        void featureVersionIsFixedDictionaryV1() {
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(), Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());
            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());
            assertThat(out.featureVersion()).isEqualTo("feature_dictionary_v1");
        }

        @Test
        void calculationVersionIsCompositeAndIncludesAllPerFeatureVersions() {
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(), Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());
            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());
            assertThat(out.calculationVersion())
                    .contains("normalization_v1")
                    .contains("sleep_quality_v1")
                    .contains("engagement_v1_chat_checkin")
                    .contains("exercise_completion_v1")
                    .contains("max_risk_daily_v1")
                    .contains("TODO_EXPERT_REVIEW");
        }

        @Test
        void perFeatureCalculationVersionsAreStable() {
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(), Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());
            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());
            assertThat(out.stress().calculationVersion()).isEqualTo("normalization_v1");
            assertThat(out.mood().calculationVersion()).isEqualTo("normalization_v1");
            assertThat(out.energy().calculationVersion()).isEqualTo("normalization_v1");
            assertThat(out.sleep().calculationVersion()).isEqualTo("sleep_quality_v1");
            assertThat(out.engagement().calculationVersion()).isEqualTo("engagement_v1_chat_checkin");
            assertThat(out.exerciseCompletion().calculationVersion()).isEqualTo("exercise_completion_v1");
            assertThat(out.maxRisk().calculationVersion()).isEqualTo("max_risk_daily_v1");
            assertThat(out.anxietySignal().calculationVersion()).isEqualTo("TODO_EXPERT_REVIEW");
        }
    }

    @Nested
    @DisplayName("Null guards")
    class NullGuards {

        @Test
        void nullSource_throws() {
            assertThatThrownBy(() -> service.calculateForDay(null, FeatureConfig.defaults()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void nullConfig_throws() {
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(), Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());
            assertThatThrownBy(() -> service.calculateForDay(source, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void featureConfigOfOutOfRange_throws() {
            assertThatThrownBy(() -> FeatureConfig.of(new BigDecimal("1.5")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> FeatureConfig.of(new BigDecimal("-0.1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Engagement & behavioural composition")
    class EngagementAndBehaviour {

        @Test
        void engagementCheckinRatio_nullWhenAssignedIsZero() {
            DailySourceAggregation.BehavioralEventCounts counts =
                    new DailySourceAggregation.BehavioralEventCounts(5L, 1L, 0L, 0L, 0L);
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(), Collections.emptyList(), counts,
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.engagement().checkinCompletionRatio()).isNull();
            assertThat(out.engagement().checkinAssignedCount()).isEqualTo(0L);
            assertThat(out.engagement().source()).isEqualTo(FeatureSource.BEHAVIORAL);
        }

        @Test
        void engagementCheckinRatio_computedWhenAssignedIsPositive() {
            DailySourceAggregation.BehavioralEventCounts counts =
                    new DailySourceAggregation.BehavioralEventCounts(5L, 1L, 3L, 1L, 4L);
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(), Collections.emptyList(), counts,
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.engagement().checkinCompletionRatio()).isEqualByComparingTo("0.750");
        }

        @Test
        void exerciseCompletion_isAlwaysNotApplicableInMvp() {
            DailySourceAggregation source = aggregation(
                    Collections.emptyList(), Collections.emptyList(),
                    DailySourceAggregation.BehavioralEventCounts.empty(),
                    com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                    DailySourceAggregation.CbtAggregation.empty());

            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.exerciseCompletion().status())
                    .isEqualTo(ExerciseCompletionResult.ExerciseCompletionStatus.NOT_APPLICABLE);
            assertThat(out.exerciseCompletion().ratio()).isNull();
        }
    }

    private DailySourceAggregation aggregation(
            List<DailySourceAggregation.ExplicitAnswer> answers,
            List<DailySourceAggregation.EffectiveChatAnalysis> chats,
            DailySourceAggregation.BehavioralEventCounts counts,
            com.mindbridge.behavior.feature.dto.CbtAvailability cbt,
            DailySourceAggregation.CbtAggregation cbtAgg) {
        return new DailySourceAggregation(
                USER, TZ, DATE, WINDOW_START, WINDOW_END,
                answers, chats, counts, cbt, cbtAgg);
    }

    private DailySourceAggregation.ExplicitAnswer explicitNumeric(String code, BigDecimal value) {
        DailyQuestionAssignment assignment = Mockito.mock(DailyQuestionAssignment.class);
        DailyQuestionAnswer answer = Mockito.mock(DailyQuestionAnswer.class);
        Mockito.when(answer.getNumericValue()).thenReturn(value);
        Mockito.when(answer.getAnswerType()).thenReturn(AnswerType.NUMERIC);
        Mockito.when(answer.getAssignment()).thenReturn(assignment);
        Mockito.when(assignment.getId()).thenReturn(UUID.randomUUID());
        Mockito.when(assignment.getTemplateCode()).thenReturn(code);
        Mockito.when(assignment.getAssignedForDate()).thenReturn(DATE);
        Mockito.when(assignment.getTimezone()).thenReturn(TZ);
        return new DailySourceAggregation.ExplicitAnswer(
                assignment.getId(), code, null, AnswerType.NUMERIC,
                value, null, null, TZ, DATE, Instant.now());
    }

    private DailySourceAggregation.ExplicitAnswer explicitOption(String code, String option) {
        DailyQuestionAssignment assignment = Mockito.mock(DailyQuestionAssignment.class);
        DailyQuestionAnswer answer = Mockito.mock(DailyQuestionAnswer.class);
        Mockito.when(answer.getOptionValue()).thenReturn(option);
        Mockito.when(answer.getAnswerType()).thenReturn(AnswerType.OPTION);
        Mockito.when(answer.getAssignment()).thenReturn(assignment);
        Mockito.when(assignment.getId()).thenReturn(UUID.randomUUID());
        Mockito.when(assignment.getTemplateCode()).thenReturn(code);
        Mockito.when(assignment.getAssignedForDate()).thenReturn(DATE);
        Mockito.when(assignment.getTimezone()).thenReturn(TZ);
        return new DailySourceAggregation.ExplicitAnswer(
                assignment.getId(), code, null, AnswerType.OPTION,
                null, null, option, TZ, DATE, Instant.now());
    }
}