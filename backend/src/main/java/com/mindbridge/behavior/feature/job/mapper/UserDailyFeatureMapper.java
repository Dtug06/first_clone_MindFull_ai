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
    @Mapping(target = "checkinAssignedCount", expression = "java(result.engagement().checkinAssignedCount() == null ? null : result.engagement().checkinAssignedCount().intValue())")
    @Mapping(target = "checkinCompletedCount", expression = "java(result.engagement().checkinCompletedCount() == null ? null : result.engagement().checkinCompletedCount().intValue())")
    @Mapping(target = "checkinCompletionRatio", expression = "java(result.engagement().checkinCompletionRatio())")
    @Mapping(target = "engagementScoreCalculationVersion", ignore = true)
    @Mapping(target = "exerciseCompletionRatio", expression = "java(result.exerciseCompletion().ratio())")
    @Mapping(target = "exerciseCompletionCalculationVersion", ignore = true)
    @Mapping(target = "maxRiskLevel", expression = "java(result.maxRisk().riskLevel() == null ? null : result.maxRisk().riskLevel().intValue())")
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
            target.setStressScoreCalculationVersion(parseVersion(result.stress().calculationVersion()));
        }
        if (result.mood() != null) {
            target.setMoodScoreCalculationVersion(parseVersion(result.mood().calculationVersion()));
            target.setMoodRawValue(result.mood().rawLabel() == null ? null : new java.math.BigDecimal(result.mood().rawLabel()));
        }
        if (result.energy() != null) {
            target.setEnergyScoreCalculationVersion(parseVersion(result.energy().calculationVersion()));
        }
        if (result.sleep() != null) {
            target.setSleepScoreCalculationVersion(parseVersion(result.sleep().calculationVersion()));
            if (result.sleep().qualityRaw() != null) {
                target.setSleepQualityRaw(java.math.BigDecimal.valueOf(result.sleep().qualityRaw()));
            }
        }
        if (result.anxietySignal() != null) {
            target.setAnxietySignalCalculationVersion(parseVersion(result.anxietySignal().calculationVersion()));
        }
        if (result.engagement() != null) {
            target.setEngagementScoreCalculationVersion(parseVersion(result.engagement().calculationVersion()));
            target.setMessageCount(result.engagement().messageCount());
            target.setActiveChatSessionCount(result.engagement().activeChatSessionCount());
        }
        if (result.exerciseCompletion() != null) {
            target.setExerciseCompletionCalculationVersion(parseVersion(result.exerciseCompletion().calculationVersion()));
        }
        if (result.maxRisk() != null) {
            target.setMaxRiskCalculationVersion(parseVersion(result.maxRisk().calculationVersion()));
        }
        target.setCalculationVersion(parseVersion(result.calculationVersion()));
    }

    default Integer parseVersion(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v); }
        catch (NumberFormatException e) { return null; }
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
