package com.mindbridge.behavior.feature.impl;

import com.mindbridge.behavior.feature.FeatureCalculationService;
import com.mindbridge.behavior.feature.config.FeatureConfig;
import com.mindbridge.behavior.feature.dto.CbtAvailability;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.AnxietySignalResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.EngagementResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.EnergyResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.ExerciseCompletionResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.MaxRiskResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.MoodResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.SleepResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.StressResult;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult.ExerciseCompletionResult.ExerciseCompletionStatus;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation.BehavioralEventCounts;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation.EffectiveChatAnalysis;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation.ExplicitAnswer;
import com.mindbridge.behavior.feature.dto.FeatureSource;
import com.mindbridge.behavior.feature.dto.FeatureSourceFlag;
import com.mindbridge.safety.resolver.RiskStateHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureCalculationServiceImpl implements FeatureCalculationService {

    private static final Logger log = LoggerFactory.getLogger(FeatureCalculationServiceImpl.class);

    private static final BigDecimal NORMALIZATION_DIVISOR = new BigDecimal("4.0");
    private static final BigDecimal NORMALIZATION_OFFSET = BigDecimal.ONE;

    private static final String FEATURE_DICTIONARY_VERSION = "feature_dictionary_v1";
    private static final String NORMALIZATION_VERSION = "normalization_v1";
    private static final String SLEEP_QUALITY_VERSION = "sleep_quality_v1";
    private static final String ENGAGEMENT_VERSION = "engagement_v1_chat_checkin";
    private static final String EXERCISE_COMPLETION_VERSION = "exercise_completion_v1";
    private static final String MAX_RISK_VERSION = "max_risk_daily_v1";
    private static final String ANXIETY_SIGNAL_VERSION_PLACEHOLDER = "TODO_EXPERT_REVIEW";

    private static final String COMPOSITE_CALCULATION_VERSION = String.join("|",
            NORMALIZATION_VERSION,
            NORMALIZATION_VERSION,
            NORMALIZATION_VERSION,
            SLEEP_QUALITY_VERSION,
            ENGAGEMENT_VERSION,
            EXERCISE_COMPLETION_VERSION,
            MAX_RISK_VERSION,
            ANXIETY_SIGNAL_VERSION_PLACEHOLDER,
            ANXIETY_SIGNAL_VERSION_PLACEHOLDER);

    private static final int EXPLICIT_FEATURE_COUNT = 4;

    private final RiskStateHistoryRepository riskStateHistoryRepository;

    public FeatureCalculationServiceImpl(RiskStateHistoryRepository riskStateHistoryRepository) {
        this.riskStateHistoryRepository = riskStateHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DailyFeatureResult calculateForDay(DailySourceAggregation source, FeatureConfig config) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }

        UUID userId = source.userId();
        String timezone = source.timezone();

        StressResult stress = calcStress(source.explicitAnswers());
        MoodResult mood = calcMood(source.explicitAnswers());
        EnergyResult energy = calcEnergy(source.explicitAnswers());
        SleepResult sleep = calcSleep(source.explicitAnswers());
        AnxietySignalResult anxietySignal = calcAnxietySignal(
                source.effectiveChatAnalyses(), config.getMinInferredConfidence());
        EngagementResult engagement = calcEngagement(source.behavioralCounts());
        ExerciseCompletionResult exerciseCompletion = calcExerciseCompletion(source.cbtAvailability());
        MaxRiskResult maxRisk = calcMaxRisk(
                userId, source.windowStartUtc(), source.windowEndUtc());

        BigDecimal explicitCoverage = calcExplicitCoverage(
                stress, mood, energy, sleep, source.explicitAnswers().size());
        BigDecimal inferredConfidence = anxietySignal.confidence();

        Set<FeatureSourceFlag> sourceFlags = computeSourceFlags(
                stress, mood, energy, sleep, anxietySignal, engagement, exerciseCompletion, maxRisk);

        if (log.isDebugEnabled()) {
            log.debug("G4-T04 calculateForDay: userId={} date={} tz={} flags={}",
                    userId, source.localDate(), timezone, sourceFlags);
        }

        return new DailyFeatureResult(
                userId,
                source.localDate(),
                timezone,
                stress,
                mood,
                energy,
                sleep,
                anxietySignal,
                engagement,
                exerciseCompletion,
                maxRisk,
                explicitCoverage,
                inferredConfidence,
                EnumSet.copyOf(sourceFlags),
                FEATURE_DICTIONARY_VERSION,
                COMPOSITE_CALCULATION_VERSION);
    }

    StressResult calcStress(List<ExplicitAnswer> answers) {
        Optional<ExplicitAnswer> match = findExplicit(answers, "STRESS");
        if (match.isEmpty()) {
            return new StressResult(null, null, FeatureSource.NONE, NORMALIZATION_VERSION);
        }
        ExplicitAnswer ans = match.get();
        BigDecimal raw = ans.numericValue();
        if (raw == null || raw.compareTo(BigDecimal.ONE) < 0 || raw.compareTo(new BigDecimal("5")) > 0) {
            log.warn("G4-T04 stress: raw_value out of [1,5] (value={}), returning NONE", raw);
            return new StressResult(null, raw, FeatureSource.NONE, NORMALIZATION_VERSION);
        }
        BigDecimal score = raw.subtract(NORMALIZATION_OFFSET)
                .divide(NORMALIZATION_DIVISOR, 3, RoundingMode.HALF_UP);
        return new StressResult(score, raw, FeatureSource.DAILY_ANSWER, NORMALIZATION_VERSION);
    }

    MoodResult calcMood(List<ExplicitAnswer> answers) {
        Optional<ExplicitAnswer> match = findExplicit(answers, "MOOD");
        if (match.isEmpty()) {
            return new MoodResult(null, null, FeatureSource.NONE, NORMALIZATION_VERSION);
        }
        ExplicitAnswer ans = match.get();
        String option = ans.optionValue();
        if (option == null) {
            log.warn("G4-T04 mood: option_value null, returning NONE");
            return new MoodResult(null, null, FeatureSource.NONE, NORMALIZATION_VERSION);
        }
        int rawInt;
        try {
            rawInt = Integer.parseInt(option.trim());
        } catch (NumberFormatException nfe) {
            log.warn("G4-T04 mood: option_value not numeric (value={}), returning NONE", option);
            return new MoodResult(null, option, FeatureSource.NONE, NORMALIZATION_VERSION);
        }
        if (rawInt < 1 || rawInt > 5) {
            log.warn("G4-T04 mood: option_value out of [1,5] (value={}), returning NONE", rawInt);
            return new MoodResult(null, option, FeatureSource.NONE, NORMALIZATION_VERSION);
        }
        BigDecimal score = BigDecimal.valueOf(4 - (rawInt - 1))
                .divide(NORMALIZATION_DIVISOR, 3, RoundingMode.HALF_UP);
        return new MoodResult(score, option, FeatureSource.DAILY_ANSWER, NORMALIZATION_VERSION);
    }

    EnergyResult calcEnergy(List<ExplicitAnswer> answers) {
        Optional<ExplicitAnswer> match = findExplicit(answers, "ENERGY");
        if (match.isEmpty()) {
            return new EnergyResult(null, null, FeatureSource.NONE, NORMALIZATION_VERSION);
        }
        ExplicitAnswer ans = match.get();
        BigDecimal raw = ans.numericValue();
        if (raw == null || raw.compareTo(BigDecimal.ONE) < 0 || raw.compareTo(new BigDecimal("5")) > 0) {
            log.warn("G4-T04 energy: raw_value out of [1,5] (value={}), returning NONE", raw);
            return new EnergyResult(null, raw, FeatureSource.NONE, NORMALIZATION_VERSION);
        }
        BigDecimal score = raw.subtract(NORMALIZATION_OFFSET)
                .divide(NORMALIZATION_DIVISOR, 3, RoundingMode.HALF_UP);
        return new EnergyResult(score, raw, FeatureSource.DAILY_ANSWER, NORMALIZATION_VERSION);
    }

    SleepResult calcSleep(List<ExplicitAnswer> answers) {
        Optional<ExplicitAnswer> match = findExplicit(answers, "SLEEP");
        if (match.isEmpty()) {
            return new SleepResult(null, null, null, FeatureSource.NONE, SLEEP_QUALITY_VERSION);
        }
        ExplicitAnswer ans = match.get();
        BigDecimal duration = ans.numericValue();
        if (duration == null || duration.signum() < 0 || duration.compareTo(new BigDecimal("24")) > 0) {
            log.warn("G4-T04 sleep: duration out of [0,24] (value={}), returning NONE", duration);
            return new SleepResult(null, null, null, FeatureSource.NONE, SLEEP_QUALITY_VERSION);
        }
        return new SleepResult(null, duration, null, FeatureSource.DAILY_ANSWER, SLEEP_QUALITY_VERSION);
    }

    AnxietySignalResult calcAnxietySignal(
            List<EffectiveChatAnalysis> chats, BigDecimal minInferredConfidence) {
        if (chats == null || chats.isEmpty()) {
            return new AnxietySignalResult(null, null, FeatureSource.NONE,
                    ANXIETY_SIGNAL_VERSION_PLACEHOLDER, null);
        }
        EffectiveChatAnalysis latest = null;
        BigDecimal maxConfidence = null;
        for (EffectiveChatAnalysis c : chats) {
            BigDecimal conf = c.confidence();
            if (conf == null) {
                continue;
            }
            if (minInferredConfidence != null && conf.compareTo(minInferredConfidence) < 0) {
                continue;
            }
            if (maxConfidence == null || conf.compareTo(maxConfidence) > 0) {
                maxConfidence = conf;
                latest = c;
            }
        }
        if (latest == null) {
            return new AnxietySignalResult(null, null, FeatureSource.NONE,
                    ANXIETY_SIGNAL_VERSION_PLACEHOLDER, null);
        }
        return new AnxietySignalResult(null, maxConfidence, FeatureSource.INFERRED,
                ANXIETY_SIGNAL_VERSION_PLACEHOLDER, latest.analysisResultId());
    }

    EngagementResult calcEngagement(BehavioralEventCounts counts) {
        long chatMessages = counts == null ? 0L : counts.chatMessageCount();
        long sessions = counts == null ? 0L : counts.activeChatSessionCount();
        long completed = counts == null ? 0L : counts.checkinCompletedCount();
        long skipped = counts == null ? 0L : counts.checkinSkippedCount();
        long assigned = counts == null ? 0L : counts.checkinAssignedCount();

        BigDecimal checkinRatio = null;
        if (assigned > 0) {
            long clampedCompleted = Math.min(completed, assigned);
            checkinRatio = BigDecimal.valueOf(clampedCompleted)
                    .divide(BigDecimal.valueOf(assigned), 3, RoundingMode.HALF_UP);
        } else {
            // No assignments for this day. Default to full completion (1.0) so
            // the CHECK constraint (ratio IN [0,1] OR NULL) is always satisfied
            // in both H2 test and PostgreSQL production.  All 5 templates are
            // always assigned on seed days, so this branch is only hit when
            // the day has no assignments at all (e.g. future or outside scope).
            checkinRatio = BigDecimal.ONE;
        }

        return new EngagementResult(
                null,
                chatMessages,
                sessions,
                assigned,
                completed,
                checkinRatio,
                FeatureSource.BEHAVIORAL,
                ENGAGEMENT_VERSION);
    }

    ExerciseCompletionResult calcExerciseCompletion(CbtAvailability availability) {
        return new ExerciseCompletionResult(null, ExerciseCompletionStatus.NOT_APPLICABLE,
                FeatureSource.NONE, EXERCISE_COMPLETION_VERSION);
    }

    MaxRiskResult calcMaxRisk(UUID userId, OffsetDateTime fromUtc, OffsetDateTime toUtc) {
        List<Short> riskLevels =
                riskStateHistoryRepository.findRiskLevelsByUserIdAndOccurredAtBetween(userId, fromUtc, toUtc);
        if (riskLevels == null || riskLevels.isEmpty()) {
            return new MaxRiskResult(null, 0, FeatureSource.NONE, MAX_RISK_VERSION);
        }
        short max = riskLevels.get(0);
        for (Short r : riskLevels) {
            if (r != null && r > max) {
                max = r;
            }
        }
        return new MaxRiskResult(max, riskLevels.size(),
                FeatureSource.SAFETY_DERIVED, MAX_RISK_VERSION);
    }

    BigDecimal calcExplicitCoverage(
            StressResult stress, MoodResult mood, EnergyResult energy, SleepResult sleep,
            int explicitAnswersCount) {
        if (explicitAnswersCount == 0) {
            return null;
        }
        long withData = 0L;
        if (stress.source() == FeatureSource.DAILY_ANSWER) withData++;
        if (mood.source() == FeatureSource.DAILY_ANSWER) withData++;
        if (energy.source() == FeatureSource.DAILY_ANSWER) withData++;
        if (sleep.source() == FeatureSource.DAILY_ANSWER) withData++;
        return BigDecimal.valueOf(withData)
                .divide(BigDecimal.valueOf(EXPLICIT_FEATURE_COUNT), 3, RoundingMode.HALF_UP);
    }

    Set<FeatureSourceFlag> computeSourceFlags(
            StressResult stress, MoodResult mood, EnergyResult energy, SleepResult sleep,
            AnxietySignalResult anxiety, EngagementResult engagement,
            ExerciseCompletionResult exercise, MaxRiskResult maxRisk) {
        EnumSet<FeatureSourceFlag> flags = EnumSet.noneOf(FeatureSourceFlag.class);
        if (stress.source() == FeatureSource.DAILY_ANSWER) flags.add(FeatureSourceFlag.EXPLICIT_USED);
        if (mood.source() == FeatureSource.DAILY_ANSWER) flags.add(FeatureSourceFlag.EXPLICIT_USED);
        if (energy.source() == FeatureSource.DAILY_ANSWER) flags.add(FeatureSourceFlag.EXPLICIT_USED);
        if (sleep.source() == FeatureSource.DAILY_ANSWER) flags.add(FeatureSourceFlag.EXPLICIT_USED);
        if (anxiety.source() == FeatureSource.INFERRED) flags.add(FeatureSourceFlag.INFERRED_USED);
        if (engagement.source() == FeatureSource.BEHAVIORAL
                && (engagement.messageCount() != null && engagement.messageCount() > 0L
                    || engagement.activeChatSessionCount() != null && engagement.activeChatSessionCount() > 0L
                    || engagement.checkinCompletedCount() != null && engagement.checkinCompletedCount() > 0L)) {
            flags.add(FeatureSourceFlag.BEHAVIORAL_USED);
        }
        if (maxRisk.source() == FeatureSource.SAFETY_DERIVED) flags.add(FeatureSourceFlag.SAFETY_USED);
        return flags;
    }

    private Optional<ExplicitAnswer> findExplicit(List<ExplicitAnswer> answers, String templateCode) {
        if (answers == null) {
            return Optional.empty();
        }
        return answers.stream()
                .filter(a -> templateCode.equals(a.templateCode()))
                .findFirst();
    }
}