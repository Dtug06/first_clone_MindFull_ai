package com.mindbridge.behavior.feature.profile.dto;

import java.util.List;

public record FeatureSeriesResponse(
        List<FeatureSeriesPoint> points,
        WindowType windowType,
        FeatureType feature
) {}
