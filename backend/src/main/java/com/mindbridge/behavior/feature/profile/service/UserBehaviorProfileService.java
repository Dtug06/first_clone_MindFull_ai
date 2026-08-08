package com.mindbridge.behavior.feature.profile.service;

import com.mindbridge.behavior.feature.profile.dto.FeatureSeriesPoint;
import com.mindbridge.behavior.feature.profile.dto.FeatureType;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.profile.dto.WindowType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBehaviorProfileService {

    boolean upsert(ProfileSnapshot snapshot);

    Optional<ProfileSnapshot> findLatestForUser(UUID userId);

    boolean hasSourceData(UUID userId);

    List<FeatureSeriesPoint> getSeries(UUID userId, WindowType windowType, FeatureType feature);
}
