package com.mindbridge.behavior.feature.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindbridge.behavior.feature.job.DailyFeatureAggregationService;
import com.mindbridge.behavior.feature.job.dto.UserAggregationResult;
import com.mindbridge.behavior.feature.profile.job.UserBehaviorProfileAggregationJobService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnDemandAggregationTriggerTest {

    @Mock DailyFeatureAggregationService dailyFeatureAggregationService;
    @Mock UserBehaviorProfileAggregationJobService profileAggregationService;

    private OnDemandAggregationTrigger trigger;
    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final LocalDate date = LocalDate.of(2026, 8, 8);

    @BeforeEach
    void setUp() {
        trigger = new OnDemandAggregationTrigger(
                dailyFeatureAggregationService, profileAggregationService);
    }

    @Test
    void t05Throws_returnsFalseAndSkipsT09() {
        when(dailyFeatureAggregationService.aggregateOneUser(userId, date))
                .thenThrow(new IllegalStateException("daily failure"));

        assertThat(trigger.triggerForUserAndDate(userId, date)).isFalse();
        verify(profileAggregationService, never()).aggregateOneUser(userId, date);
    }

    @Test
    void t05FailureResult_returnsFalseAndSkipsT09() {
        when(dailyFeatureAggregationService.aggregateOneUser(userId, date))
                .thenReturn(UserAggregationResult.failure(userId, date, "no source"));

        assertThat(trigger.triggerForUserAndDate(userId, date)).isFalse();
        verify(profileAggregationService, never()).aggregateOneUser(userId, date);
    }

    @Test
    void t09Throws_returnsFalse() {
        when(dailyFeatureAggregationService.aggregateOneUser(userId, date))
                .thenReturn(UserAggregationResult.success(userId, date, UUID.randomUUID()));
        when(profileAggregationService.aggregateOneUser(userId, date))
                .thenThrow(new IllegalStateException("profile failure"));

        assertThat(trigger.triggerForUserAndDate(userId, date)).isFalse();
    }

    @Test
    void t09NoUpdate_returnsFalse() {
        when(dailyFeatureAggregationService.aggregateOneUser(userId, date))
                .thenReturn(UserAggregationResult.success(userId, date, UUID.randomUUID()));
        when(profileAggregationService.aggregateOneUser(userId, date)).thenReturn(false);

        assertThat(trigger.triggerForUserAndDate(userId, date)).isFalse();
    }

    @Test
    void bothStagesSucceed_returnsTrue() {
        when(dailyFeatureAggregationService.aggregateOneUser(userId, date))
                .thenReturn(UserAggregationResult.success(userId, date, UUID.randomUUID()));
        when(profileAggregationService.aggregateOneUser(userId, date)).thenReturn(true);

        assertThat(trigger.triggerForUserAndDate(userId, date)).isTrue();
    }

    @Test
    void callbackFailuresNeverEscape() {
        when(dailyFeatureAggregationService.aggregateOneUser(userId, date))
                .thenThrow(new IllegalArgumentException("unexpected callback failure"));

        assertThatCode(() -> trigger.triggerForUserAndDate(userId, date))
                .doesNotThrowAnyException();
    }
}
