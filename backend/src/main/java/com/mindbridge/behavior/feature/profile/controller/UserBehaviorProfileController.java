package com.mindbridge.behavior.feature.profile.controller;

import com.mindbridge.behavior.feature.profile.dto.FeatureSeriesPoint;
import com.mindbridge.behavior.feature.profile.dto.FeatureSeriesResponse;
import com.mindbridge.behavior.feature.profile.dto.FeatureType;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.profile.dto.UserBehaviorProfileResponse;
import com.mindbridge.behavior.feature.profile.dto.WindowType;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileResponseMapper;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileService;
import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.common.service.CurrentUserService;
import java.util.List;
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

    public UserBehaviorProfileController(
            UserBehaviorProfileService profileService,
            UserBehaviorProfileResponseMapper responseMapper,
            CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.responseMapper = responseMapper;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<UserBehaviorProfileResponse> getCurrentProfile() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        ProfileSnapshot snapshot = profileService.findLatestForUser(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BEHAVIOR_PROFILE_NOT_FOUND,
                        "Behavior profile not yet available for the current user"));
        return ResponseEntity.ok(responseMapper.toResponse(snapshot));
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