package com.mindbridge.behavior.feature.profile.controller;

import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.profile.dto.FeatureSeriesPoint;
import com.mindbridge.behavior.feature.profile.dto.FeatureSeriesResponse;
import com.mindbridge.behavior.feature.profile.dto.FeatureType;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.profile.dto.UserBehaviorProfileResponse;
import com.mindbridge.behavior.feature.profile.dto.WindowType;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileResponseMapper;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileService;
import com.mindbridge.behavior.feature.profile.service.OnDemandAggregationTrigger;
import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.common.service.CurrentUserService;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/behavior/profile")
public class UserBehaviorProfileController {

    private final UserBehaviorProfileService profileService;
    private final UserBehaviorProfileResponseMapper responseMapper;
    private final CurrentUserService currentUserService;
    private final OnDemandAggregationTrigger onDemandAggregationTrigger;
    private final UserRepository userRepository;
    private final Clock clock;

    public UserBehaviorProfileController(
            UserBehaviorProfileService profileService,
            UserBehaviorProfileResponseMapper responseMapper,
            CurrentUserService currentUserService,
            OnDemandAggregationTrigger onDemandAggregationTrigger,
            UserRepository userRepository,
            Clock clock) {
        this.profileService = profileService;
        this.responseMapper = responseMapper;
        this.currentUserService = currentUserService;
        this.onDemandAggregationTrigger = onDemandAggregationTrigger;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @GetMapping
    public ResponseEntity<UserBehaviorProfileResponse> getCurrentProfile() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        Optional<ProfileSnapshot> existing = profileService.findLatestForUser(currentUserId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(responseMapper.toResponse(existing.get()));
        }

        // Never manufacture a profile for a user who has supplied no Daily
        // Check-in data. Lazy aggregation is only a reconciliation path.
        if (!profileService.hasSourceData(currentUserId)) {
            throw profileNotFound();
        }

        LocalDate today = LocalDate.now(clock.withZone(resolveUserZone(currentUserId)));
        onDemandAggregationTrigger.triggerForUserAndDate(currentUserId, today);

        ProfileSnapshot snapshot = profileService.findLatestForUser(currentUserId)
                .orElseThrow(this::profileNotFound);
        return ResponseEntity.ok(responseMapper.toResponse(snapshot));
    }

    private ZoneId resolveUserZone(UUID userId) {
        String timezone = userRepository.findById(userId)
                .map(user -> user.getTimezone())
                .filter(value -> !value.isBlank())
                .orElse("UTC");
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ignored) {
            return ZoneId.of("UTC");
        }
    }

    private ResourceNotFoundException profileNotFound() {
        return new ResourceNotFoundException(
                ErrorCode.BEHAVIOR_PROFILE_NOT_FOUND,
                "Behavior profile not yet available for the current user");
    }

    @GetMapping("/series")
    public ResponseEntity<FeatureSeriesResponse> getSeries(
            @RequestParam WindowType windowType,
            @RequestParam FeatureType feature) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        List<FeatureSeriesPoint> points = profileService.getSeries(currentUserId, windowType, feature);
        return ResponseEntity.ok(new FeatureSeriesResponse(points, windowType, feature));
    }
}
