package com.mindbridge.behavior.feature.profile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.engagement.EngagementAndTopicsService;
import com.mindbridge.behavior.feature.engagement.config.EngagementConfig;
import com.mindbridge.behavior.feature.engagement.dto.EngagementAndTopicsResult;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import com.mindbridge.behavior.feature.profile.config.DataQualityConfig;
import com.mindbridge.behavior.feature.profile.config.DataQualityConfigProperties;
import com.mindbridge.behavior.feature.profile.config.TrendConfigProperties;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.trend.TrendCalculator;
import com.mindbridge.behavior.feature.trend.dto.TrendSummary;
import com.mindbridge.behavior.feature.window.WindowAggregationService;
import com.mindbridge.behavior.feature.window.dto.WindowAggregationResult;
import com.mindbridge.safety.resolver.RiskStateHistory;
import com.mindbridge.safety.resolver.RiskStateHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserBehaviorProfileAggregationServiceImpl
        implements UserBehaviorProfileAggregationService {

    private static final Logger log =
            LoggerFactory.getLogger(UserBehaviorProfileAggregationServiceImpl.class);
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("UTC");

    private final WindowAggregationService windowAggregationService;
    private final TrendCalculator trendCalculator;
    private final EngagementAndTopicsService engagementAndTopicsService;
    private final RiskStateHistoryRepository riskStateHistoryRepository;
    private final ObjectMapper objectMapper;
    private final TrendConfigProperties trendConfigProperties;
    private final DataQualityConfigProperties dataQualityConfigProperties;
    private final UserRepository userRepository;

    public UserBehaviorProfileAggregationServiceImpl(
            WindowAggregationService windowAggregationService,
            TrendCalculator trendCalculator,
            EngagementAndTopicsService engagementAndTopicsService,
            RiskStateHistoryRepository riskStateHistoryRepository,
            ObjectMapper objectMapper,
            TrendConfigProperties trendConfigProperties,
            DataQualityConfigProperties dataQualityConfigProperties,
            UserRepository userRepository) {
        this.windowAggregationService = windowAggregationService;
        this.trendCalculator = trendCalculator;
        this.engagementAndTopicsService = engagementAndTopicsService;
        this.riskStateHistoryRepository = riskStateHistoryRepository;
        this.objectMapper = objectMapper;
        this.trendConfigProperties = trendConfigProperties;
        this.dataQualityConfigProperties = dataQualityConfigProperties;
        this.userRepository = userRepository;
    }

    @Override
    public ProfileSnapshot aggregateForUser(UUID userId, LocalDate targetDate) {
        return aggregateForUser(userId, targetDate, dataQualityConfigProperties.toConfig());
    }

    /**
     * Overload that accepts a {@link DataQualityConfig} so the scheduled job
     * (and CLI runner) can inject expert-approved thresholds. The no-arg
     * overload uses {@link DataQualityConfig#defaults()} which causes
     * {@code NullPointerException} on evaluation — fail-fast in unconfigured
     * environments.
     *
     * <p>Trend thresholds are sourced from the injected
     * {@link TrendConfigProperties} (G4-T12). If any property is missing,
     * {@link com.mindbridge.behavior.feature.trend.config.TrendConfig#of}
     * accepts it as {@code null} and the calculator will throw
     * {@code IllegalStateException} on first call — fail-fast.
     */
    public ProfileSnapshot aggregateForUser(UUID userId, LocalDate targetDate,
                                          DataQualityConfig dataQualityConfig) {
        ZoneId zoneId = resolveUserZone(userId);

        WindowAggregationResult window =
                windowAggregationService.aggregateForUser(userId, targetDate);

        TrendSummary trend = trendCalculator.calculateTrendForUser(
                userId, targetDate, zoneId, trendConfigProperties.toTrendConfig());

        EngagementAndTopicsResult engagement = engagementAndTopicsService.summarizeForUser(
                userId, targetDate, zoneId, EngagementConfig.defaults());

        Short riskLevel = null;
        UUID riskHistoryId = null;
        var latestRisk = riskStateHistoryRepository
                .findFirstByUserIdOrderByOccurredAtDescIdDesc(userId);
        if (latestRisk.isPresent()) {
            RiskStateHistory r = latestRisk.get();
            riskLevel = r.getRiskLevel();
            riskHistoryId = r.getId();
        }

        BigDecimal coverage7d = nzOrZero(window.explicitCoverage7d());
        BigDecimal coverage30d = nzOrZero(window.explicitCoverage30d());
        BigDecimal dataCoverage = maxOf(coverage7d, coverage30d);
        // Clamp to [0, 1] - coverage must not exceed 1 even if the denominator
        // is smaller than the number of seeded days (e.g. user just registered).
        if (dataCoverage.compareTo(BigDecimal.ONE) > 0) {
            dataCoverage = BigDecimal.ONE;
        }
        BigDecimal confidence7d = nzOrZero(window.inferredConfidence7d());
        BigDecimal confidence30d = nzOrZero(window.inferredConfidence30d());
        BigDecimal confidence = maxOf(confidence7d, confidence30d);
        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            confidence = BigDecimal.ONE;
        }

        DataQualityStatus dataQualityStatus =
                dataQualityConfig.evaluate(dataCoverage, confidence);

        OffsetDateTime calculatedAt = Instant.now()
                .atZone(zoneId)
                .toOffsetDateTime();

        String trendJson = serializeTrend(trend);

        return new ProfileSnapshot(
                userId,
                targetDate,
                window.stressScore7d(),
                window.stressScore30d(),
                window.moodScore7d(),
                window.moodScore30d(),
                window.energyScore7d(),
                window.energyScore30d(),
                window.sleepScore7d(),
                window.sleepScore30d(),
                window.anxietySignal7d(),
                window.anxietySignal30d(),
                engagement.engagementActivityScore7d(),
                engagement.engagementActivityScore30d(),
                trendJson,
                engagement.dominantTopics7d(),
                engagement.dominantTopics30d(),
                riskLevel,
                riskHistoryId,
                dataCoverage,
                confidence,
                dataQualityStatus,
                calculatedAt);
    }

    private String serializeTrend(TrendSummary trend) {
        try {
            return objectMapper.writeValueAsString(trend);
        } catch (JsonProcessingException e) {
            log.warn("G4-T09 aggregateForUser failed to serialize TrendSummary for userId={}",
                    trend.userId(), e);
            return null;
        }
    }

    private ZoneId resolveUserZone(UUID userId) {
        String value = userRepository.findById(userId)
                .map(user -> user.getTimezone())
                .orElse(null);
        if (value == null || value.isBlank()) {
            return FALLBACK_ZONE;
        }
        try {
            return ZoneId.of(value);
        } catch (DateTimeException ex) {
            log.warn("Invalid user timezone; falling back to UTC for userId={}", userId);
            return FALLBACK_ZONE;
        }
    }

    private static BigDecimal nzOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal maxOf(BigDecimal a, BigDecimal b) {
        BigDecimal max = a.compareTo(b) >= 0 ? a : b;
        return max.setScale(3, RoundingMode.HALF_UP);
    }
}
