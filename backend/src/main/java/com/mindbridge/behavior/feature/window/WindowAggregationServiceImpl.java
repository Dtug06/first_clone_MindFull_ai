package com.mindbridge.behavior.feature.window;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import com.mindbridge.behavior.feature.window.dto.WindowAggregationResult;
import com.mindbridge.behavior.feature.window.repository.UserDailyFeatureWindowRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WindowAggregationServiceImpl implements WindowAggregationService {
    private static final Logger log = LoggerFactory.getLogger(WindowAggregationServiceImpl.class);
    private static final String DEFAULT_TZ = "Asia/Ho_Chi_Minh";
    private static final int WINDOW_7 = 7;
    private static final int WINDOW_30 = 30;

    private final UserDailyFeatureWindowRepository featureRepository;
    private final UserRepository userRepository;

    public WindowAggregationServiceImpl(
            UserDailyFeatureWindowRepository featureRepository,
            UserRepository userRepository) {
        this.featureRepository = featureRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public WindowAggregationResult aggregateForUser(UUID userId, java.time.LocalDate targetDate) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return resultWithUser(userId, targetDate);

        java.time.LocalDate windowStart7 = targetDate.minusDays(WINDOW_7 - 1);
        java.time.LocalDate windowStart30 = targetDate.minusDays(WINDOW_30 - 1);

        int daysSinceRegistration = computeDaysSinceRegistration(userOpt.get(), targetDate);
        int denominator7 = Math.min(WINDOW_7, daysSinceRegistration);
        int denominator30 = Math.min(WINDOW_30, daysSinceRegistration);

        List<UserDailyFeature> rows7 = featureRepository.findByUserAndWindow(userId, windowStart7, targetDate);
        List<UserDailyFeature> rows30 = featureRepository.findByUserAndWindow(userId, windowStart30, targetDate);

        return buildResult(userId, targetDate, rows7, rows30, denominator7, denominator30);
    }

    private int computeDaysSinceRegistration(User user, java.time.LocalDate targetDate) {
        if (user.getCreatedAt() == null) return 1;
        java.time.LocalDate reg = user.getCreatedAt().atZone(ZoneId.of(DEFAULT_TZ)).toLocalDate();
        if (reg.isAfter(targetDate)) return 1;
        return (int) (targetDate.toEpochDay() - reg.toEpochDay()) + 1;
    }

    private WindowAggregationResult buildResult(
            UUID userId, java.time.LocalDate targetDate,
            List<UserDailyFeature> rows7, List<UserDailyFeature> rows30,
            int denominator7, int denominator30) {

        BigDecimal stressScore7d = avgScore(rows7, UserDailyFeature::getStressScore);
        BigDecimal stressScore30d = avgScore(rows30, UserDailyFeature::getStressScore);
        long stressDays7 = countDistinctDays(rows7, UserDailyFeature::getStressScore);
        long stressDays30 = countDistinctDays(rows30, UserDailyFeature::getStressScore);
        BigDecimal stressCoverage7d = coverage(stressDays7, denominator7);
        BigDecimal stressCoverage30d = coverage(stressDays30, denominator30);
        BigDecimal stressRawAvg30d = avgScore(rows30, UserDailyFeature::getStressRawValue);

        BigDecimal moodScore7d = avgScore(rows7, UserDailyFeature::getMoodScore);
        BigDecimal moodScore30d = avgScore(rows30, UserDailyFeature::getMoodScore);
        long moodDays7 = countDistinctDays(rows7, UserDailyFeature::getMoodScore);
        long moodDays30 = countDistinctDays(rows30, UserDailyFeature::getMoodScore);
        BigDecimal moodCoverage7d = coverage(moodDays7, denominator7);
        BigDecimal moodCoverage30d = coverage(moodDays30, denominator30);

        BigDecimal energyScore7d = avgScore(rows7, UserDailyFeature::getEnergyScore);
        BigDecimal energyScore30d = avgScore(rows30, UserDailyFeature::getEnergyScore);
        long energyDays7 = countDistinctDays(rows7, UserDailyFeature::getEnergyScore);
        long energyDays30 = countDistinctDays(rows30, UserDailyFeature::getEnergyScore);
        BigDecimal energyCoverage7d = coverage(energyDays7, denominator7);
        BigDecimal energyCoverage30d = coverage(energyDays30, denominator30);

        BigDecimal sleepHoursAvg7d = avgScore(rows7, UserDailyFeature::getSleepHours);
        BigDecimal sleepHoursAvg30d = avgScore(rows30, UserDailyFeature::getSleepHours);
        BigDecimal sleepScore7d = avgScore(rows7, UserDailyFeature::getSleepScore);
        BigDecimal sleepScore30d = avgScore(rows30, UserDailyFeature::getSleepScore);
        long sleepDays7 = countDistinctDays(rows7, UserDailyFeature::getSleepHours);
        long sleepDays30 = countDistinctDays(rows30, UserDailyFeature::getSleepHours);
        BigDecimal sleepCoverage7d = coverage(sleepDays7, denominator7);
        BigDecimal sleepCoverage30d = coverage(sleepDays30, denominator30);

        BigDecimal anxietySignal7d = avgScore(rows7, UserDailyFeature::getAnxietySignal);
        BigDecimal anxietySignal30d = avgScore(rows30, UserDailyFeature::getAnxietySignal);
        BigDecimal anxietyConfidence7d = avgScore(rows7, UserDailyFeature::getAnxietySignalConfidence);
        BigDecimal anxietyConfidence30d = avgScore(rows30, UserDailyFeature::getAnxietySignalConfidence);
        String anxietySource7d = anxietySource(rows7);
        String anxietySource30d = anxietySource(rows30);
        long anxietyDays7 = countDistinctDays(rows7, UserDailyFeature::getAnxietySignal);
        long anxietyDays30 = countDistinctDays(rows30, UserDailyFeature::getAnxietySignal);
        BigDecimal anxietyCoverage7d = coverage(anxietyDays7, denominator7);
        BigDecimal anxietyCoverage30d = coverage(anxietyDays30, denominator30);

        BigDecimal engagementScore7d = avgScore(rows7, UserDailyFeature::getEngagementScore);
        BigDecimal engagementScore30d = avgScore(rows30, UserDailyFeature::getEngagementScore);
        long engagementDays7 = countDistinctDays(rows7, UserDailyFeature::getEngagementScore);
        long engagementDays30 = countDistinctDays(rows30, UserDailyFeature::getEngagementScore);
        BigDecimal engagementCoverage7d = coverage(engagementDays7, denominator7);
        BigDecimal engagementCoverage30d = coverage(engagementDays30, denominator30);
        Long messageCountSum7d = sumLong(rows7, UserDailyFeature::getMessageCount);
        Long messageCountSum30d = sumLong(rows30, UserDailyFeature::getMessageCount);
        Long checkinCompletedSum7d = sumLongInt(rows7, UserDailyFeature::getCheckinCompletedCount);
        Long checkinCompletedSum30d = sumLongInt(rows30, UserDailyFeature::getCheckinCompletedCount);

        String exerciseStatus = "NOT_APPLICABLE";

        Integer maxRiskLevel7d = maxInt(rows7, UserDailyFeature::getMaxRiskLevel);
        Integer maxRiskLevel30d = maxInt(rows30, UserDailyFeature::getMaxRiskLevel);
        Long riskEventCount7d = sumLongInt(rows7, UserDailyFeature::getRiskEventCount);
        Long riskEventCount30d = sumLongInt(rows30, UserDailyFeature::getRiskEventCount);
        long maxRiskDays7 = countDistinctDaysInt(rows7, UserDailyFeature::getMaxRiskLevel);
        long maxRiskDays30 = countDistinctDaysInt(rows30, UserDailyFeature::getMaxRiskLevel);
        BigDecimal maxRiskCoverage7d = coverage(maxRiskDays7, denominator7);
        BigDecimal maxRiskCoverage30d = coverage(maxRiskDays30, denominator30);

        long explicitDays7 = featureRepository.countDaysWithExplicitData(
                userId, targetDate.minusDays(WINDOW_7 - 1), targetDate);
        long explicitDays30 = featureRepository.countDaysWithExplicitData(
                userId, targetDate.minusDays(WINDOW_30 - 1), targetDate);
        BigDecimal explicitCoverage7d = coverage(explicitDays7, denominator7);
        BigDecimal explicitCoverage30d = coverage(explicitDays30, denominator30);

        BigDecimal inferredConfidence7d = avgScore(rows7, UserDailyFeature::getAnxietySignalConfidence);
        BigDecimal inferredConfidence30d = avgScore(rows30, UserDailyFeature::getAnxietySignalConfidence);

        return new WindowAggregationResult(
                userId, targetDate,
                stressScore7d, stressScore30d,
                stressCoverage7d, stressCoverage30d, stressRawAvg30d,
                moodScore7d, moodScore30d,
                moodCoverage7d, moodCoverage30d,
                energyScore7d, energyScore30d,
                energyCoverage7d, energyCoverage30d,
                sleepHoursAvg7d, sleepHoursAvg30d,
                sleepScore7d, sleepScore30d,
                sleepCoverage7d, sleepCoverage30d,
                anxietySignal7d, anxietySignal30d,
                anxietyConfidence7d, anxietyConfidence30d,
                anxietySource7d, anxietySource30d,
                anxietyCoverage7d, anxietyCoverage30d,
                engagementScore7d, engagementScore30d,
                engagementCoverage7d, engagementCoverage30d,
                messageCountSum7d, messageCountSum30d,
                checkinCompletedSum7d, checkinCompletedSum30d,
                null, null, exerciseStatus, exerciseStatus,
                maxRiskLevel7d, maxRiskLevel30d,
                riskEventCount7d, riskEventCount30d,
                maxRiskCoverage7d, maxRiskCoverage30d,
                explicitCoverage7d, explicitCoverage30d,
                inferredConfidence7d, inferredConfidence30d);
    }

    private WindowAggregationResult resultWithUser(UUID userId, java.time.LocalDate targetDate) {
        return new WindowAggregationResult(
                userId, targetDate,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO, null,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, null,
                null, null, "NOT_APPLICABLE", "NOT_APPLICABLE",
                null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }

    private BigDecimal avgScore(List<UserDailyFeature> rows,
            java.util.function.Function<UserDailyFeature, BigDecimal> getter) {
        java.util.DoubleSummaryStatistics stats = rows.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .summaryStatistics();
        if (stats.getCount() == 0) return null;
        return BigDecimal.valueOf(stats.getAverage()).setScale(4, RoundingMode.HALF_UP);
    }

    private long countDistinctDays(List<UserDailyFeature> rows,
            java.util.function.Function<UserDailyFeature, BigDecimal> getter) {
        return rows.stream()
                .filter(r -> getter.apply(r) != null)
                .map(UserDailyFeature::getFeatureDate)
                .distinct()
                .count();
    }

    private long countDistinctDaysInt(List<UserDailyFeature> rows,
            java.util.function.Function<UserDailyFeature, Integer> getter) {
        return rows.stream()
                .filter(r -> getter.apply(r) != null)
                .map(UserDailyFeature::getFeatureDate)
                .distinct()
                .count();
    }

    private BigDecimal coverage(long daysWithData, int denominator) {
        if (denominator <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(daysWithData)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private String anxietySource(List<UserDailyFeature> rows) {
        boolean hasSource = rows.stream()
                .map(UserDailyFeature::getAnxietySignal)
                .filter(Objects::nonNull)
                .findFirst()
                .isPresent();
        return hasSource ? "CHAT_ANALYSIS" : "NONE";
    }

    private Long sumLong(List<UserDailyFeature> rows,
            java.util.function.Function<UserDailyFeature, Long> getter) {
        long sum = rows.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        return sum == 0 ? null : sum;
    }

    private Long sumLongInt(List<UserDailyFeature> rows,
            java.util.function.Function<UserDailyFeature, Integer> getter) {
        long sum = rows.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        return sum == 0 ? null : sum;
    }

    private Integer maxInt(List<UserDailyFeature> rows,
            java.util.function.Function<UserDailyFeature, Integer> getter) {
        return rows.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
