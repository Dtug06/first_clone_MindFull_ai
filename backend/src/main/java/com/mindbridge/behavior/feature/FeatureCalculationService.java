package com.mindbridge.behavior.feature;

import com.mindbridge.behavior.feature.config.FeatureConfig;
import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;

public interface FeatureCalculationService {

    DailyFeatureResult calculateForDay(DailySourceAggregation source, FeatureConfig config);
}