package com.mindbridge.behavior.feature.profile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.profile.dto.TrendSummaryResponse;
import com.mindbridge.behavior.feature.profile.dto.UserBehaviorProfileResponse;
import com.mindbridge.behavior.feature.trend.dto.TrendSummary;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserBehaviorProfileResponseMapper {

    private static final Logger log =
            LoggerFactory.getLogger(UserBehaviorProfileResponseMapper.class);

    private final ObjectMapper objectMapper;

    public UserBehaviorProfileResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UserBehaviorProfileResponse toResponse(ProfileSnapshot snapshot) {
        UUID userId = snapshot.userId();
        TrendSummaryResponse trend = parseTrendSummary(snapshot.trendSummaryJson(), userId,
                snapshot.windowEnd());
        return new UserBehaviorProfileResponse(
                com.mindbridge.behavior.feature.profile.entity.UserBehaviorProfile.PROFILE_VERSION,
                snapshot.windowEnd(),
                snapshot.stressAvg7d(),
                snapshot.stressAvg30d(),
                snapshot.moodAvg7d(),
                snapshot.moodAvg30d(),
                snapshot.energyAvg7d(),
                snapshot.energyAvg30d(),
                snapshot.sleepAvg7d(),
                snapshot.sleepAvg30d(),
                snapshot.anxietyAvg7d(),
                snapshot.anxietyAvg30d(),
                snapshot.engagementScore7d(),
                snapshot.engagementScore30d(),
                snapshot.riskLevel() == null ? null : snapshot.riskLevel().intValue(),
                snapshot.dominantTopics7d(),
                snapshot.dominantTopics30d(),
                trend,
                snapshot.dataCoverage(),
                snapshot.confidence(),
                snapshot.dataQualityStatus(),
                snapshot.calculatedAt());
    }

    /**
     * Deserialize the raw trend JSON into a typed {@link TrendSummaryResponse}.
     * <p>If parsing fails, return an empty-but-valid fallback so the dashboard
     * never crashes on a malformed row (G4-T12 Phase 1 decision #5).
     */
    private TrendSummaryResponse parseTrendSummary(String json, UUID userId,
                                                   java.time.LocalDate targetDate) {
        if (json == null || json.isBlank()) {
            return TrendSummaryResponse.empty(userId, targetDate, null);
        }
        try {
            TrendSummary summary = objectMapper.readValue(json, TrendSummary.class);
            return TrendSummaryResponse.from(summary);
        } catch (JsonProcessingException e) {
            log.warn("G4-T12 mapper failed to deserialize trendSummary JSON for userId={}; "
                    + "returning empty TrendSummaryResponse", userId, e);
            return TrendSummaryResponse.empty(userId, targetDate, null);
        }
    }
}