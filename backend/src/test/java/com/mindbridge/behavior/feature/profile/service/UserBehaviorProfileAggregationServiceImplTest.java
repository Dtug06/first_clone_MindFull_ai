package com.mindbridge.behavior.feature.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mindbridge.behavior.feature.engagement.EngagementAndTopicsService;
import com.mindbridge.behavior.feature.engagement.config.EngagementConfig;
import com.mindbridge.behavior.feature.engagement.dto.EngagementAndTopicsResult;
import com.mindbridge.behavior.feature.engagement.dto.TopicFrequency;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import com.mindbridge.behavior.feature.profile.config.DataQualityConfig;
import com.mindbridge.behavior.feature.profile.config.DataQualityConfigProperties;
import com.mindbridge.behavior.feature.profile.config.TrendConfigProperties;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.trend.TrendCalculator;
import com.mindbridge.behavior.feature.trend.config.TrendConfig;
import com.mindbridge.behavior.feature.trend.dto.StreakInfo;
import com.mindbridge.behavior.feature.trend.dto.TrendDirection;
import com.mindbridge.behavior.feature.trend.dto.TrendEntry;
import com.mindbridge.behavior.feature.trend.dto.TrendReason;
import com.mindbridge.behavior.feature.trend.dto.TrendSummary;
import com.mindbridge.behavior.feature.window.WindowAggregationService;
import com.mindbridge.behavior.feature.window.dto.WindowAggregationResult;
import com.mindbridge.safety.resolver.RiskStateHistory;
import com.mindbridge.safety.resolver.RiskStateHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserBehaviorProfileAggregationServiceImplTest {

    @Mock WindowAggregationService windowAggregationService;
    @Mock TrendCalculator trendCalculator;
    @Mock EngagementAndTopicsService engagementService;
    @Mock RiskStateHistoryRepository riskStateHistoryRepository;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    UserBehaviorProfileAggregationServiceImpl service;

    UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    LocalDate targetDate = LocalDate.of(2026, 8, 4);

    DataQualityConfig sufficientConfig = DataQualityConfig.of(
            new BigDecimal("0.20"), new BigDecimal("0.50"), new BigDecimal("0.30"));
    DataQualityConfig lowConfig = DataQualityConfig.of(
            new BigDecimal("0.10"), new BigDecimal("0.40"), new BigDecimal("0.20"));
    DataQualityConfig insufficientConfig = DataQualityConfig.of(
            new BigDecimal("0.05"), new BigDecimal("0.10"), new BigDecimal("0.50"));

    // G4-T12: TrendConfigProperties with placeholder values (same as
    // application.yml); TrendConfig.of validates [0,1] range.
    TrendConfigProperties trendConfigProperties = new TrendConfigProperties(
            new BigDecimal("0.5"), new BigDecimal("0.1"), new BigDecimal("0.75"));

    @BeforeEach
    void setUp() {
        service = new UserBehaviorProfileAggregationServiceImpl(
                windowAggregationService,
                trendCalculator,
                engagementService,
                riskStateHistoryRepository,
                objectMapper,
                trendConfigProperties,
                new DataQualityConfigProperties(
                        new BigDecimal("0.20"),
                        new BigDecimal("0.50"),
                        new BigDecimal("0.30")));
    }

    @Test
    @DisplayName("aggregateForUser maps all upstream scores into ProfileSnapshot")
    void aggregateForUser_mapsAllUpstreamScores() {
        WindowAggregationResult window = mockWindow(
                new BigDecimal("0.420"), new BigDecimal("0.500"),
                new BigDecimal("0.600"), new BigDecimal("0.550"));
        TrendSummary trend = mockTrend();
        EngagementAndTopicsResult engagement = mockEngagement(2, 3,
                List.of(new TopicFrequency("WORK_STRESS", 5L, 0.6250)));
        RiskStateHistory risk = mockRisk((short) 2);

        when(windowAggregationService.aggregateForUser(userId, targetDate))
                .thenReturn(window);
        when(trendCalculator.calculateTrendForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(TrendConfig.class)))
                .thenReturn(trend);
        when(engagementService.summarizeForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(EngagementConfig.class)))
                .thenReturn(engagement);
        when(riskStateHistoryRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(Optional.of(risk));

        ProfileSnapshot snapshot = service.aggregateForUser(userId, targetDate, sufficientConfig);

        assertThat(snapshot.userId()).isEqualTo(userId);
        assertThat(snapshot.windowEnd()).isEqualTo(targetDate);
        assertThat(snapshot.stressAvg7d()).isEqualByComparingTo("0.420");
        assertThat(snapshot.stressAvg30d()).isEqualByComparingTo("0.500");
        assertThat(snapshot.engagementScore7d()).isEqualTo(2);
        assertThat(snapshot.engagementScore30d()).isEqualTo(3);
        assertThat(snapshot.riskLevel()).isEqualTo((short) 2);
        assertThat(snapshot.riskHistoryId()).isEqualTo(risk.getId());
        assertThat(snapshot.dominantTopics7d()).hasSize(1);
        assertThat(snapshot.dominantTopics7d().get(0).topic()).isEqualTo("WORK_STRESS");
        assertThat(snapshot.dataQualityStatus()).isEqualTo(DataQualityStatus.SUFFICIENT);
        assertThat(snapshot.calculatedAt()).isNotNull();
    }

    @Test
    @DisplayName("dataCoverage is max(7d, 30d) rounded to 3 decimals")
    void dataCoverage_takesMaxOfWindows() {
        WindowAggregationResult window = mockWindowWithCoverage(
                new BigDecimal("0.4"), new BigDecimal("0.7"));
        when(windowAggregationService.aggregateForUser(userId, targetDate))
                .thenReturn(window);
        when(trendCalculator.calculateTrendForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(TrendConfig.class)))
                .thenReturn(mockTrend());
        when(engagementService.summarizeForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(EngagementConfig.class)))
                .thenReturn(mockEngagement(0, 0, List.of()));
        when(riskStateHistoryRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        ProfileSnapshot snapshot = service.aggregateForUser(userId, targetDate, sufficientConfig);

        assertThat(snapshot.dataCoverage()).isEqualByComparingTo("0.700");
        assertThat(snapshot.confidence()).isNotNull();
    }

    @Test
    @DisplayName("user with no data: dataCoverage and confidence default to 0")
    void firstTimeUser_coverageAndConfidenceAreZero() {
        WindowAggregationResult window = mockWindowEmpty();
        when(windowAggregationService.aggregateForUser(userId, targetDate))
                .thenReturn(window);
        when(trendCalculator.calculateTrendForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(TrendConfig.class)))
                .thenReturn(mockTrend());
        when(engagementService.summarizeForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(EngagementConfig.class)))
                .thenReturn(mockEngagement(0, 0, List.of()));
        when(riskStateHistoryRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        ProfileSnapshot snapshot = service.aggregateForUser(userId, targetDate, insufficientConfig);

        assertThat(snapshot.dataCoverage()).isEqualByComparingTo("0");
        assertThat(snapshot.confidence()).isEqualByComparingTo("0");
        assertThat(snapshot.riskLevel()).isNull();
        assertThat(snapshot.riskHistoryId()).isNull();
        assertThat(snapshot.dominantTopics7d()).isEmpty();
        assertThat(snapshot.dominantTopics30d()).isEmpty();
        assertThat(snapshot.dataQualityStatus()).isEqualTo(DataQualityStatus.INSUFFICIENT);
    }

    @Test
    @DisplayName("null explicitCoverage / inferredConfidence treated as zero")
    void nullCoverage_treatedAsZero() {
        WindowAggregationResult window = mockWindowWithCoverage(null, null);
        when(windowAggregationService.aggregateForUser(userId, targetDate))
                .thenReturn(window);
        when(trendCalculator.calculateTrendForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(TrendConfig.class)))
                .thenReturn(mockTrend());
        when(engagementService.summarizeForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(EngagementConfig.class)))
                .thenReturn(mockEngagement(0, 0, List.of()));
        when(riskStateHistoryRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        ProfileSnapshot snapshot = service.aggregateForUser(userId, targetDate, insufficientConfig);

        assertThat(snapshot.dataCoverage()).isEqualByComparingTo("0");
        assertThat(snapshot.confidence()).isEqualByComparingTo("0");
        assertThat(snapshot.dataQualityStatus()).isEqualTo(DataQualityStatus.INSUFFICIENT);
    }

    @Test
    @DisplayName("SUFFICIENT: coverage >= minCoverageForSufficient AND confidence >= minConfidence")
    void dataQualityStatus_SUFFICIENT() {
        WindowAggregationResult window = mockWindowWithCoverage(
                new BigDecimal("0.6"), new BigDecimal("0.6"));
        when(windowAggregationService.aggregateForUser(userId, targetDate))
                .thenReturn(window);
        when(trendCalculator.calculateTrendForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(TrendConfig.class)))
                .thenReturn(mockTrend());
        when(engagementService.summarizeForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(EngagementConfig.class)))
                .thenReturn(mockEngagement(0, 0, List.of()));
        when(riskStateHistoryRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        ProfileSnapshot snapshot = service.aggregateForUser(userId, targetDate, sufficientConfig);

        assertThat(snapshot.dataQualityStatus()).isEqualTo(DataQualityStatus.SUFFICIENT);
    }

    @Test
    @DisplayName("LOW: coverage >= minCoverageForLow but < minCoverageForSufficient")
    void dataQualityStatus_LOW() {
        WindowAggregationResult window = mockWindowWithCoverage(
                new BigDecimal("0.30"), new BigDecimal("0.30"));
        when(windowAggregationService.aggregateForUser(userId, targetDate))
                .thenReturn(window);
        when(trendCalculator.calculateTrendForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(TrendConfig.class)))
                .thenReturn(mockTrend());
        when(engagementService.summarizeForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(EngagementConfig.class)))
                .thenReturn(mockEngagement(0, 0, List.of()));
        when(riskStateHistoryRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        ProfileSnapshot snapshot = service.aggregateForUser(userId, targetDate, lowConfig);

        assertThat(snapshot.dataQualityStatus()).isEqualTo(DataQualityStatus.LOW);
    }

    @Test
    @DisplayName("INSUFFICIENT: coverage < minCoverageForLow")
    void dataQualityStatus_INSUFFICIENT_coverageLow() {
        WindowAggregationResult window = mockWindowWithCoverage(
                new BigDecimal("0.02"), new BigDecimal("0.02"));
        when(windowAggregationService.aggregateForUser(userId, targetDate))
                .thenReturn(window);
        when(trendCalculator.calculateTrendForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(TrendConfig.class)))
                .thenReturn(mockTrend());
        when(engagementService.summarizeForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(EngagementConfig.class)))
                .thenReturn(mockEngagement(0, 0, List.of()));
        when(riskStateHistoryRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        ProfileSnapshot snapshot = service.aggregateForUser(userId, targetDate, insufficientConfig);

        assertThat(snapshot.dataQualityStatus()).isEqualTo(DataQualityStatus.INSUFFICIENT);
    }

    @Test
    @DisplayName("INSUFFICIENT: confidence < minConfidence (even if coverage is high)")
    void dataQualityStatus_INSUFFICIENT_confidenceLow() {
        WindowAggregationResult window = mockWindowWithCoverage(
                new BigDecimal("0.9"), new BigDecimal("0.05"));
        when(windowAggregationService.aggregateForUser(userId, targetDate))
                .thenReturn(window);
        when(trendCalculator.calculateTrendForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(TrendConfig.class)))
                .thenReturn(mockTrend());
        when(engagementService.summarizeForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(EngagementConfig.class)))
                .thenReturn(mockEngagement(0, 0, List.of()));
        when(riskStateHistoryRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        ProfileSnapshot snapshot = service.aggregateForUser(userId, targetDate, sufficientConfig);

        assertThat(snapshot.dataQualityStatus()).isEqualTo(DataQualityStatus.INSUFFICIENT);
    }

    @Test
    @DisplayName("no-arg overload uses externally configured data-quality thresholds")
    void noArgOverload_usesDefaults() {
        WindowAggregationResult window = mockWindowWithCoverage(
                new BigDecimal("0.6"), new BigDecimal("0.6"));
        when(windowAggregationService.aggregateForUser(userId, targetDate))
                .thenReturn(window);
        when(trendCalculator.calculateTrendForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(TrendConfig.class)))
                .thenReturn(mockTrend());
        when(engagementService.summarizeForUser(
                eq(userId), eq(targetDate), any(ZoneId.class), any(EngagementConfig.class)))
                .thenReturn(mockEngagement(0, 0, List.of()));
        when(riskStateHistoryRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        ProfileSnapshot snapshot = service.aggregateForUser(userId, targetDate);

        assertThat(snapshot.dataQualityStatus()).isEqualTo(DataQualityStatus.SUFFICIENT);
    }

    private WindowAggregationResult mockWindow(BigDecimal score7d, BigDecimal score30d,
                                              BigDecimal coverage7d, BigDecimal coverage30d) {
        return new WindowAggregationResult(
                userId, targetDate,
                score7d, score30d, coverage7d, coverage30d, null,
                score7d, score30d, coverage7d, coverage30d,
                score7d, score30d, coverage7d, coverage30d,
                score7d, score30d, score7d, score30d, coverage7d, coverage30d,
                score7d, score30d, score7d, score30d, "NONE", "NONE", coverage7d, coverage30d,
                score7d, score30d, coverage7d, coverage30d, 0L, 0L, 0L, 0L,
                null, null, "NOT_APPLICABLE", "NOT_APPLICABLE",
                2, 2, 0L, 0L, coverage7d, coverage30d,
                coverage7d, coverage30d, score7d, score30d);
    }

    private WindowAggregationResult mockWindowWithCoverage(
            BigDecimal explicitCov, BigDecimal inferredConf) {
        return new WindowAggregationResult(
                userId, targetDate,
                null, null, explicitCov, inferredConf, null,
                null, null, explicitCov, inferredConf,
                null, null, explicitCov, inferredConf,
                null, null, null, null, explicitCov, inferredConf,
                null, null, null, null, "NONE", "NONE", explicitCov, inferredConf,
                null, null, explicitCov, inferredConf, 0L, 0L, 0L, 0L,
                null, null, "NOT_APPLICABLE", "NOT_APPLICABLE",
                null, null, 0L, 0L, explicitCov, inferredConf,
                explicitCov, inferredConf, inferredConf, inferredConf);
    }

    private WindowAggregationResult mockWindowEmpty() {
        return new WindowAggregationResult(
                userId, targetDate,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, "NONE", "NONE", null, null,
                null, null, null, null, 0L, 0L, 0L, 0L,
                null, null, "NOT_APPLICABLE", "NOT_APPLICABLE",
                null, null, 0L, 0L, null, null,
                null, null, null, null);
    }

    private TrendSummary mockTrend() {
        TrendEntry entry = new TrendEntry("stress_score",
                TrendDirection.UNKNOWN, null, TrendReason.NO_PRIOR_DATA,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO);
        StreakInfo streak = new StreakInfo(0, 0, null, null, 30);
        return new TrendSummary(userId, targetDate, ZoneId.of("Asia/Ho_Chi_Minh"),
                List.of(entry), streak,
                TrendSummary.DATA_QUALITY_PLACEHOLDER, TrendSummary.CALCULATION_VERSION);
    }

    private EngagementAndTopicsResult mockEngagement(int score7d, int score30d,
                                                    List<TopicFrequency> topics) {
        return new EngagementAndTopicsResult(userId, score7d, score30d,
                topics, topics,
                EngagementConfig.CALCULATION_VERSION);
    }

    private RiskStateHistory mockRisk(short level) {
        return RiskStateHistory.record(
                UUID.randomUUID(),
                userId,
                level,
                null,
                null,
                null,
                com.mindbridge.safety.resolver.RiskStateSourceType.MANUAL_REVIEW,
                null,
                "NONE",
                null,
                null,
                new BigDecimal("0.500"),
                new String[]{"MANUAL_REVIEW_REQUIRED"},
                OffsetDateTime.now());
    }
}
