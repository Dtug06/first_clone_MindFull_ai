package com.mindbridge.behavior.feature.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.DailySourceAggregationService;
import com.mindbridge.behavior.feature.FeatureCalculationService;
import com.mindbridge.behavior.feature.config.FeatureConfig;
import com.mindbridge.behavior.feature.dto.CbtAvailability;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import com.mindbridge.behavior.feature.job.mapper.UserDailyFeatureMapper;
import com.mindbridge.behavior.feature.job.persistence.UserDailyFeatureUpsertService;
import com.mindbridge.behavior.feature.job.recorder.JobRunRecorder;
import com.mindbridge.behavior.feature.job.repository.JobRunRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyFeatureAggregationServiceImplTest {

    @Mock DailySourceAggregationService sourceService;
    @Mock UserRepository userRepository;
    @Mock FeatureCalculationService calculationService;
    @Mock UserDailyFeatureMapper mapper;
    @Mock UserDailyFeatureUpsertService upsertService;
    @Mock JobRunRepository jobRunRepository;
    @Mock JobRunRecorder recorder;

    private DailyFeatureAggregationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DailyFeatureAggregationServiceImpl(
                sourceService, userRepository,
                new DailyFeatureAggregationProperties(false, 100),
                calculationService, mapper, upsertService, jobRunRepository, recorder);
    }

    @Test
    void aggregateOneUserUsesRealSourceAggregatorAndUserTimezone() {
        UUID userId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 6);
        String timezone = "Asia/Ho_Chi_Minh";
        User user = User.register("test@example.com", "hash", "Test");
        user.setTimezone(timezone);
        DailySourceAggregation source = new DailySourceAggregation(
                userId, timezone, date,
                date.atStartOfDay(ZoneId.of(timezone)).toOffsetDateTime(),
                date.plusDays(1).atStartOfDay(ZoneId.of(timezone)).toOffsetDateTime(),
                List.of(), List.of(), DailySourceAggregation.BehavioralEventCounts.empty(),
                CbtAvailability.NOT_SHIPPED, DailySourceAggregation.CbtAggregation.empty());
        DailyFeatureResult calculated = mock(DailyFeatureResult.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sourceService.aggregateForDay(userId, timezone, date)).thenReturn(source);
        when(calculationService.calculateForDay(
                org.mockito.ArgumentMatchers.eq(source),
                org.mockito.ArgumentMatchers.any(FeatureConfig.class)))
                .thenReturn(calculated);
        when(upsertService.upsert(org.mockito.ArgumentMatchers.any(UserDailyFeature.class)))
                .thenReturn(rowId);

        var result = service.aggregateOneUser(userId, date);

        assertThat(result.success()).isTrue();
        assertThat(result.rowId()).isEqualTo(rowId);
        verify(sourceService).aggregateForDay(userId, timezone, date);
        verify(calculationService).calculateForDay(
                org.mockito.ArgumentMatchers.eq(source),
                org.mockito.ArgumentMatchers.any(FeatureConfig.class));
    }
}
