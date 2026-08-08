package com.mindbridge.behavior.feature.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.profile.dto.UserBehaviorProfileResponse;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileResponseMapper;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileService;
import com.mindbridge.behavior.feature.profile.service.OnDemandAggregationTrigger;
import com.mindbridge.behavior.feature.trend.dto.StreakInfo;
import com.mindbridge.behavior.feature.trend.dto.TrendDirection;
import com.mindbridge.behavior.feature.trend.dto.TrendEntry;
import com.mindbridge.behavior.feature.trend.dto.TrendReason;
import com.mindbridge.behavior.feature.trend.dto.TrendSummary;
import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.common.service.CurrentUserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
class UserBehaviorProfileControllerTest {

    @Mock UserBehaviorProfileService profileService;
    @Mock CurrentUserService currentUserService;
    @Mock OnDemandAggregationTrigger onDemandAggregationTrigger;
    @Mock UserRepository userRepository;
    Clock clock = Clock.fixed(Instant.parse("2026-08-08T12:00:00Z"), ZoneOffset.UTC);

    // G4-T12: register JavaTimeModule so the mapper can deserialize
    // TrendSummary JSON (ZoneId + LocalDate) into typed records.
    UserBehaviorProfileResponseMapper mapper = new UserBehaviorProfileResponseMapper(
            new ObjectMapper().registerModule(new JavaTimeModule()));

    UserBehaviorProfileController controller;

    UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        controller = new UserBehaviorProfileController(profileService, mapper,
                currentUserService, onDemandAggregationTrigger, userRepository, clock);
    }

    @Test
    @DisplayName("Returns 200 response with full payload when profile exists")
    void returnsFullPayload() {
        LocalDate windowEnd = LocalDate.of(2026, 8, 4);
        OffsetDateTime calculatedAt = OffsetDateTime.parse("2026-08-04T03:15:00Z");
        // G4-T12: trendSummaryJson now holds a full TrendSummary JSON, not a
        // flat Map<String,String>. Mapper will deserialize it and produce a
        // typed TrendSummaryResponse.
        TrendEntry stressEntry = new TrendEntry("stress", TrendDirection.STABLE,
                null, TrendReason.SUFFICIENT_DATA,
                new BigDecimal("0.4"), new BigDecimal("0.5"),
                new BigDecimal("0.6"), new BigDecimal("0.5"));
        TrendSummary trend = new TrendSummary(userId, windowEnd, ZoneId.of("UTC"),
                List.of(stressEntry), new StreakInfo(1, 0, windowEnd, null, 30),
                TrendSummary.DATA_QUALITY_PLACEHOLDER, TrendSummary.CALCULATION_VERSION);
        String trendJson;
        try {
            trendJson = new ObjectMapper().registerModule(new JavaTimeModule())
                    .writeValueAsString(trend);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        ProfileSnapshot snapshot = new ProfileSnapshot(
                userId, windowEnd,
                new BigDecimal("0.4"), new BigDecimal("0.5"),
                new BigDecimal("0.6"), new BigDecimal("0.5"),
                new BigDecimal("0.7"), new BigDecimal("0.5"),
                new BigDecimal("0.55"), new BigDecimal("0.55"),
                new BigDecimal("0.3"), new BigDecimal("0.3"),
                2, 3,
                trendJson,
                List.of(), List.of(),
                (short) 2, null,
                new BigDecimal("0.6"),
                new BigDecimal("0.5"),
                DataQualityStatus.SUFFICIENT,
                calculatedAt);

        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(profileService.findLatestForUser(userId)).thenReturn(Optional.of(snapshot));

        UserBehaviorProfileResponse resp = controller.getCurrentProfile().getBody();

        assertThat(resp).isNotNull();
        assertThat(resp.profileVersion()).isEqualTo("profile_v1");
        assertThat(resp.windowEnd()).isEqualTo(windowEnd);
        assertThat(resp.riskLevel()).isEqualTo(2);
        assertThat(resp.dataCoverage()).isEqualByComparingTo("0.6");
        assertThat(resp.confidence()).isEqualByComparingTo("0.5");
        assertThat(resp.dataQualityStatus()).isEqualTo(DataQualityStatus.SUFFICIENT);
        assertThat(resp.engagementScore7d()).isEqualTo(2);
        assertThat(resp.engagementScore30d()).isEqualTo(3);
        assertThat(resp.calculatedAt()).isEqualTo(calculatedAt);
        assertThat(resp.dominantTopics7d()).isEmpty();
        assertThat(resp.dominantTopics30d()).isEmpty();
        assertThat(resp.trendSummary()).isNotNull();
        assertThat(resp.trendSummary().calculationVersion()).isEqualTo("trend_v1");
        assertThat(resp.trendSummary().entries()).hasSize(1);
        assertThat(resp.trendSummary().entries().get(0).featureCode()).isEqualTo("stress");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException with BEHAVIOR_PROFILE_NOT_FOUND")
    void throwsWhenProfileMissing() {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(profileService.findLatestForUser(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException thrown = null;
        try {
            controller.getCurrentProfile();
        } catch (ResourceNotFoundException e) {
            thrown = e;
        }

        assertThat(thrown).isNotNull();
        assertThat(thrown.getCode()).isEqualTo(ErrorCode.BEHAVIOR_PROFILE_NOT_FOUND);
        assertThat(thrown.getMessage()).contains("Behavior profile not yet available");
    }

    @Test
    @DisplayName("Never reads userId from request - always from CurrentUserService")
    void readsUserIdOnlyFromJwt() {
        UUID differentUser = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(currentUserService.getCurrentUserId()).thenReturn(differentUser);
        when(profileService.findLatestForUser(differentUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getCurrentProfile())
                .isInstanceOf(ResourceNotFoundException.class);

        org.mockito.Mockito.verify(profileService).findLatestForUser(differentUser);
        org.mockito.Mockito.verify(profileService, org.mockito.Mockito.never())
                .findLatestForUser(userId);
    }

    @Test
    @DisplayName("Missing profile with no Daily Answer remains 404 without aggregation")
    void noSourceData_doesNotAggregate() {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(profileService.findLatestForUser(userId)).thenReturn(Optional.empty());
        when(profileService.hasSourceData(userId)).thenReturn(false);

        assertThatThrownBy(() -> controller.getCurrentProfile())
                .isInstanceOf(ResourceNotFoundException.class);

        verify(onDemandAggregationTrigger, never())
                .triggerForUserAndDate(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Missing profile with Daily Answer aggregates lazily and reloads")
    void sourceData_lazyAggregatesAndReloads() {
        ProfileSnapshot generated = mock(ProfileSnapshot.class);
        when(generated.userId()).thenReturn(userId);
        when(generated.windowEnd()).thenReturn(LocalDate.of(2026, 8, 8));
        when(generated.dataCoverage()).thenReturn(BigDecimal.ZERO);
        when(generated.confidence()).thenReturn(BigDecimal.ZERO);
        when(generated.dataQualityStatus()).thenReturn(DataQualityStatus.INSUFFICIENT);
        when(generated.riskLevel()).thenReturn((short) 1);
        when(generated.calculatedAt()).thenReturn(OffsetDateTime.parse("2026-08-08T12:00:00Z"));
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(profileService.findLatestForUser(userId))
                .thenReturn(Optional.empty(), Optional.of(generated));
        when(profileService.hasSourceData(userId)).thenReturn(true);
        when(onDemandAggregationTrigger.triggerForUserAndDate(
                userId, LocalDate.of(2026, 8, 8))).thenReturn(true);

        assertThat(controller.getCurrentProfile().getStatusCode().is2xxSuccessful()).isTrue();
        verify(onDemandAggregationTrigger)
                .triggerForUserAndDate(userId, LocalDate.of(2026, 8, 8));
        verify(profileService, org.mockito.Mockito.times(2)).findLatestForUser(userId);
    }
}
