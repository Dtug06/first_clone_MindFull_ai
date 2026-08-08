package com.mindbridge.behavior.feature.job.mapper;

import com.mindbridge.behavior.feature.dto.DailyFeatureResult;
import com.mindbridge.behavior.feature.dto.FeatureSource;
import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.AfterMapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserDailyFeatureMapper {

    UserDailyFeatureMapper INSTANCE = Mappers.getMapper(UserDailyFeatureMapper.class);

    @Mapping(target = "id", source = "entityId")
    @Mapping(target = "userId", source = "result.userId")
    @Mapping(target = "featureDate", source = "featureDate")
    @Mapping(target = "timezone", source = "timezone")
    @Mapping(target = "stressScore", expression = "java(result.stress().score())")
    @Mapping(target = "stressRawValue", expression = "java(result.stress().rawValue())")
    @Mapping(target = "stressScoreCalculationVersion", ignore = true)
    @Mapping(target = "moodScore", expression = "java(result.mood().score())")
    @Mapping(target = "moodRawValue", ignore = true)
    @Mapping(target = "moodScoreCalculationVersion", ignore = true)
    @Mapping(target = "energyScore", expression = "java(result.energy().score())")
    @Mapping(target = "energyRawValue", expression = "java(result.energy().rawValue())")
    @Mapping(target = "energyScoreCalculationVersion", ignore = true)
    @Mapping(target = "sleepHours", expression = "java(result.sleep().durationHours())")
    @Mapping(target = "sleepQualityRaw", ignore = true)
    @Mapping(target = "sleepScore", expression = "java(result.sleep().score())")
    @Mapping(target = "sleepScoreCalculationVersion", ignore = true)
    @Mapping(target = "anxietySignal", expression = "java(result.anxietySignal().score())")
    @Mapping(target = "anxietySignalConfidence", expression = "java(result.anxietySignal().confidence())")
    @Mapping(target = "anxietySignalSource", expression = "java(mapAnxietySource(result.anxietySignal().source()))")
    @Mapping(target = "anxietySignalCalculationVersion", ignore = true)
    @Mapping(target = "anxietyAnalysisResultId", expression = "java(result.anxietySignal().analysisResultId())")
    @Mapping(target = "engagementScore", expression = "java(result.engagement().score())")
    @Mapping(target = "messageCount", ignore = true)
    @Mapping(target = "activeChatSessionCount", ignore = true)
    @Mapping(target = "checkinAssignedCount", expression = "java(toIntegerExact(result.engagement().checkinAssignedCount()))")
    @Mapping(target = "checkinCompletedCount", expression = "java(toIntegerExact(result.engagement().checkinCompletedCount()))")
    @Mapping(target = "checkinCompletionRatio", expression = "java(result.engagement().checkinCompletionRatio())")
    @Mapping(target = "engagementScoreCalculationVersion", ignore = true)
    @Mapping(target = "exerciseCompletionRatio", expression = "java(result.exerciseCompletion().ratio())")
    @Mapping(target = "exerciseCompletionCalculationVersion", ignore = true)
    @Mapping(target = "maxRiskLevel", expression = "java(result.maxRisk().riskLevel())")
    @Mapping(target = "riskEventCount", expression = "java(result.maxRisk().riskEventCount())")
    @Mapping(target = "maxRiskCalculationVersion", ignore = true)
    @Mapping(target = "explicitCoverage", source = "result.explicitCoverage")
    @Mapping(target = "inferredConfidence", source = "result.inferredConfidence")
    @Mapping(target = "calculationVersion", ignore = true)
    @Mapping(target = "featureVersion", ignore = true)
    @Mapping(target = "extraFeatures", ignore = true)
    @Mapping(target = "createdAt", source = "createdAt")
    void toEntity(DailyFeatureResult result, UUID entityId, LocalDate featureDate, String timezone, OffsetDateTime createdAt, @MappingTarget UserDailyFeature target);

    @AfterMapping
    default void afterMapping(DailyFeatureResult result, @MappingTarget UserDailyFeature target) {
        if (result.stress() != null) {
            target.setStressScoreCalculationVersion(result.stress().calculationVersion());
        }
        if (result.mood() != null) {
            target.setMoodScoreCalculationVersion(result.mood().calculationVersion());
            target.setMoodRawValue(result.mood().rawLabel());
        }
        if (result.energy() != null) {
            target.setEnergyScoreCalculationVersion(result.energy().calculationVersion());
        }
        if (result.sleep() != null) {
            target.setSleepScoreCalculationVersion(result.sleep().calculationVersion());
            target.setSleepQualityRaw(result.sleep().qualityRaw());
        }
        if (result.anxietySignal() != null) {
            target.setAnxietySignalCalculationVersion(result.anxietySignal().calculationVersion());
        }
        if (result.engagement() != null) {
            target.setEngagementScoreCalculationVersion(result.engagement().calculationVersion());
            target.setMessageCount(toIntegerExact(result.engagement().messageCount()));
            target.setActiveChatSessionCount(toIntegerExact(result.engagement().activeChatSessionCount()));
        }
        if (result.exerciseCompletion() != null) {
            target.setExerciseCompletionCalculationVersion(result.exerciseCompletion().calculationVersion());
        }
        if (result.maxRisk() != null) {
            target.setMaxRiskCalculationVersion(result.maxRisk().calculationVersion());
        }
        target.setFeatureVersion(result.featureVersion());
        target.setCalculationVersion(result.calculationVersion());
    }

    default Integer toIntegerExact(Long value) {
        return value == null ? null : Math.toIntExact(value);
    }

    @Named("mapAnxietySource")
    default String mapAnxietySource(FeatureSource src) {
        if (src == null) return "NONE";
        switch (src) {
            case INFERRED: return "CHAT_ANALYSIS";
            default: return "NONE";
        }
    }
}
