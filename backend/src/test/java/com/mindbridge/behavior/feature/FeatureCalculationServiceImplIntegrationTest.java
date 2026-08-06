package com.mindbridge.behavior.feature;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.behavior.feature.config.FeatureConfig;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
import com.mindbridge.behavior.feature.dto.FeatureSource;
import com.mindbridge.behavior.feature.dto.FeatureSourceFlag;
import com.mindbridge.behavior.feature.impl.FeatureCalculationServiceImpl;
import com.mindbridge.safety.resolver.RiskStateHistory;
import com.mindbridge.safety.resolver.RiskStateHistoryRepository;
import com.mindbridge.safety.resolver.RiskStateSourceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-risk-state-history.sql"
})
class FeatureCalculationServiceImplIntegrationTest {

    @Autowired
    FeatureCalculationServiceImpl service;

    @Autowired
    RiskStateHistoryRepository riskStateHistoryRepository;

    private static final String TZ = "UTC";

    @Nested
    @DisplayName("Max-risk integration against H2 risk_state_history mirror")
    class MaxRiskIntegration {

        @Test
        void maxRisk_isMaxOfRowsInWindow() {
            UUID userId = UUID.randomUUID();
            OffsetDateTime today = OffsetDateTime.parse("2026-08-04T08:00:00Z");
            riskStateHistoryRepository.save(riskRow(userId, today, (short) 1));
            riskStateHistoryRepository.save(riskRow(userId, today.plusHours(6), (short) 3));
            riskStateHistoryRepository.save(riskRow(userId, today.plusHours(14), (short) 2));

            DailySourceAggregation source = sourceFor(userId, LocalDate.parse("2026-08-04"));
            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.maxRisk().riskLevel()).isEqualTo((short) 3);
            assertThat(out.maxRisk().riskEventCount()).isEqualTo(3);
            assertThat(out.maxRisk().source()).isEqualTo(FeatureSource.SAFETY_DERIVED);
            assertThat(out.sourceFlags()).contains(FeatureSourceFlag.SAFETY_USED);
        }

        @Test
        void maxRisk_isNullWhenNoRowsInWindow() {
            UUID userId = UUID.randomUUID();
            riskStateHistoryRepository.save(riskRow(userId,
                    OffsetDateTime.parse("2026-08-03T22:00:00Z"), (short) 2));

            DailySourceAggregation source = sourceFor(userId, LocalDate.parse("2026-08-04"));
            DailyFeatureResult out = service.calculateForDay(source, FeatureConfig.defaults());

            assertThat(out.maxRisk().riskLevel()).isNull();
            assertThat(out.maxRisk().riskEventCount()).isEqualTo(0);
            assertThat(out.maxRisk().source()).isEqualTo(FeatureSource.NONE);
            assertThat(out.sourceFlags()).doesNotContain(FeatureSourceFlag.SAFETY_USED);
        }
    }

    private DailySourceAggregation sourceFor(UUID userId, LocalDate date) {
        OffsetDateTime start = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return new DailySourceAggregation(
                userId, TZ, date, start, end,
                Collections.emptyList(),
                Collections.emptyList(),
                DailySourceAggregation.BehavioralEventCounts.empty(),
                com.mindbridge.behavior.feature.dto.CbtAvailability.NOT_SHIPPED,
                DailySourceAggregation.CbtAggregation.empty());
    }

    private RiskStateHistory riskRow(UUID userId, OffsetDateTime occurredAt, short riskLevel) {
        return RiskStateHistory.record(
                UUID.randomUUID(),
                userId,
                riskLevel,
                (short) 1,
                (short) 1,
                riskLevel,
                RiskStateSourceType.LLM_CLASSIFIER,
                null,
                "rule_v1",
                "model_v1",
                "prompt_v1",
                new BigDecimal("0.95"),
                new String[] {"OK"},
                occurredAt);
    }
}