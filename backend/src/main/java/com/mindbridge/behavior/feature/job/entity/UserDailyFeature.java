package com.mindbridge.behavior.feature.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_daily_features")
public class UserDailyFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "feature_date", nullable = false)
    private LocalDate featureDate;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Column(name = "stress_score")
    private BigDecimal stressScore;
    @Column(name = "stress_raw_value")
    private BigDecimal stressRawValue;
    @Column(name = "stress_score_calculation_version")
    private String stressScoreCalculationVersion;

    @Column(name = "mood_score")
    private BigDecimal moodScore;
    @Column(name = "mood_raw_value")
    private String moodRawValue;
    @Column(name = "mood_score_calculation_version")
    private String moodScoreCalculationVersion;

    @Column(name = "energy_score")
    private BigDecimal energyScore;
    @Column(name = "energy_raw_value")
    private BigDecimal energyRawValue;
    @Column(name = "energy_score_calculation_version")
    private String energyScoreCalculationVersion;

    @Column(name = "sleep_hours")
    private BigDecimal sleepHours;
    @Column(name = "sleep_quality_raw")
    private Short sleepQualityRaw;
    @Column(name = "sleep_score")
    private BigDecimal sleepScore;
    @Column(name = "sleep_score_calculation_version")
    private String sleepScoreCalculationVersion;

    @Column(name = "anxiety_signal")
    private BigDecimal anxietySignal;
    @Column(name = "anxiety_signal_confidence")
    private BigDecimal anxietySignalConfidence;
    @Column(name = "anxiety_signal_calculation_version")
    private String anxietySignalCalculationVersion;
    @Column(name = "anxiety_signal_source")
    private String anxietySignalSource;
    @Column(name = "anxiety_analysis_result_id")
    private UUID anxietyAnalysisResultId;

    @Column(name = "engagement_score")
    private BigDecimal engagementScore;
    @Column(name = "message_count")
    private Integer messageCount;
    @Column(name = "active_chat_session_count")
    private Integer activeChatSessionCount;

    @Column(name = "checkin_assigned_count")
    private Integer checkinAssignedCount;
    @Column(name = "checkin_completed_count")
    private Integer checkinCompletedCount;
    @Column(name = "checkin_completion_ratio")
    private BigDecimal checkinCompletionRatio;

    @Column(name = "engagement_score_calculation_version")
    private String engagementScoreCalculationVersion;
    @Column(name = "exercise_completion_ratio")
    private BigDecimal exerciseCompletionRatio;
    @Column(name = "exercise_completion_calculation_version")
    private String exerciseCompletionCalculationVersion;

    @Column(name = "max_risk_level")
    private Short maxRiskLevel;
    @Column(name = "risk_event_count")
    private Integer riskEventCount;
    @Column(name = "max_risk_calculation_version")
    private String maxRiskCalculationVersion;

    @Column(name = "explicit_coverage")
    private BigDecimal explicitCoverage;
    @Column(name = "inferred_confidence")
    private BigDecimal inferredConfidence;

    @Column(name = "feature_version")
    private String featureVersion;
    @Column(name = "calculation_version")
    private String calculationVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_features", columnDefinition = "jsonb")
    private String extraFeatures;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public LocalDate getFeatureDate() { return featureDate; }
    public void setFeatureDate(LocalDate featureDate) { this.featureDate = featureDate; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public BigDecimal getStressScore() { return stressScore; }
    public void setStressScore(BigDecimal stressScore) { this.stressScore = stressScore; }
    public BigDecimal getStressRawValue() { return stressRawValue; }
    public void setStressRawValue(BigDecimal stressRawValue) { this.stressRawValue = stressRawValue; }
    public String getStressScoreCalculationVersion() { return stressScoreCalculationVersion; }
    public void setStressScoreCalculationVersion(String v) { this.stressScoreCalculationVersion = v; }
    public BigDecimal getMoodScore() { return moodScore; }
    public void setMoodScore(BigDecimal moodScore) { this.moodScore = moodScore; }
    public String getMoodRawValue() { return moodRawValue; }
    public void setMoodRawValue(String moodRawValue) { this.moodRawValue = moodRawValue; }
    public String getMoodScoreCalculationVersion() { return moodScoreCalculationVersion; }
    public void setMoodScoreCalculationVersion(String v) { this.moodScoreCalculationVersion = v; }
    public BigDecimal getEnergyScore() { return energyScore; }
    public void setEnergyScore(BigDecimal energyScore) { this.energyScore = energyScore; }
    public BigDecimal getEnergyRawValue() { return energyRawValue; }
    public void setEnergyRawValue(BigDecimal energyRawValue) { this.energyRawValue = energyRawValue; }
    public String getEnergyScoreCalculationVersion() { return energyScoreCalculationVersion; }
    public void setEnergyScoreCalculationVersion(String v) { this.energyScoreCalculationVersion = v; }
    public BigDecimal getSleepHours() { return sleepHours; }
    public void setSleepHours(BigDecimal sleepHours) { this.sleepHours = sleepHours; }
    /**
     * The database contract stores sleep quality as PostgreSQL SMALLINT. Keep
     * the public calculation/mapping API as Integer while enforcing the
     * SMALLINT range at the entity boundary.
     */
    public Integer getSleepQualityRaw() {
        return sleepQualityRaw == null ? null : sleepQualityRaw.intValue();
    }
    public void setSleepQualityRaw(Integer sleepQualityRaw) {
        if (sleepQualityRaw == null) {
            this.sleepQualityRaw = null;
        } else if (sleepQualityRaw < Short.MIN_VALUE || sleepQualityRaw > Short.MAX_VALUE) {
            throw new ArithmeticException("sleepQualityRaw exceeds SMALLINT range: " + sleepQualityRaw);
        } else {
            this.sleepQualityRaw = sleepQualityRaw.shortValue();
        }
    }
    public void setSleepQualityRaw(Short sleepQualityRaw) {
        this.sleepQualityRaw = sleepQualityRaw;
    }
    public BigDecimal getSleepScore() { return sleepScore; }
    public void setSleepScore(BigDecimal sleepScore) { this.sleepScore = sleepScore; }
    public String getSleepScoreCalculationVersion() { return sleepScoreCalculationVersion; }
    public void setSleepScoreCalculationVersion(String v) { this.sleepScoreCalculationVersion = v; }
    public BigDecimal getAnxietySignal() { return anxietySignal; }
    public void setAnxietySignal(BigDecimal anxietySignal) { this.anxietySignal = anxietySignal; }
    public BigDecimal getAnxietySignalConfidence() { return anxietySignalConfidence; }
    public void setAnxietySignalConfidence(BigDecimal v) { this.anxietySignalConfidence = v; }
    public String getAnxietySignalCalculationVersion() { return anxietySignalCalculationVersion; }
    public void setAnxietySignalCalculationVersion(String v) { this.anxietySignalCalculationVersion = v; }
    public String getAnxietySignalSource() { return anxietySignalSource; }
    public void setAnxietySignalSource(String s) { this.anxietySignalSource = s; }
    public UUID getAnxietyAnalysisResultId() { return anxietyAnalysisResultId; }
    public void setAnxietyAnalysisResultId(UUID u) { this.anxietyAnalysisResultId = u; }
    public BigDecimal getEngagementScore() { return engagementScore; }
    public void setEngagementScore(BigDecimal e) { this.engagementScore = e; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer m) { this.messageCount = m; }
    public void setMessageCount(Long m) {
        this.messageCount = m == null ? null : Math.toIntExact(m);
    }
    public Integer getActiveChatSessionCount() { return activeChatSessionCount; }
    public void setActiveChatSessionCount(Integer a) { this.activeChatSessionCount = a; }
    public void setActiveChatSessionCount(Long a) {
        this.activeChatSessionCount = a == null ? null : Math.toIntExact(a);
    }
    public Integer getCheckinAssignedCount() { return checkinAssignedCount; }
    public void setCheckinAssignedCount(Integer c) { this.checkinAssignedCount = c; }
    public Integer getCheckinCompletedCount() { return checkinCompletedCount; }
    public void setCheckinCompletedCount(Integer c) { this.checkinCompletedCount = c; }
    public BigDecimal getCheckinCompletionRatio() { return checkinCompletionRatio; }
    public void setCheckinCompletionRatio(BigDecimal r) { this.checkinCompletionRatio = r; }
    public String getEngagementScoreCalculationVersion() { return engagementScoreCalculationVersion; }
    public void setEngagementScoreCalculationVersion(String v) { this.engagementScoreCalculationVersion = v; }
    public BigDecimal getExerciseCompletionRatio() { return exerciseCompletionRatio; }
    public void setExerciseCompletionRatio(BigDecimal e) { this.exerciseCompletionRatio = e; }
    public String getExerciseCompletionCalculationVersion() { return exerciseCompletionCalculationVersion; }
    public void setExerciseCompletionCalculationVersion(String v) { this.exerciseCompletionCalculationVersion = v; }
    public Short getMaxRiskLevel() { return maxRiskLevel; }
    public void setMaxRiskLevel(Short m) { this.maxRiskLevel = m; }
    public void setMaxRiskLevel(Integer m) {
        if (m == null) {
            this.maxRiskLevel = null;
        } else if (m < Short.MIN_VALUE || m > Short.MAX_VALUE) {
            throw new ArithmeticException("maxRiskLevel exceeds SMALLINT range: " + m);
        } else {
            this.maxRiskLevel = m.shortValue();
        }
    }
    public Integer getRiskEventCount() { return riskEventCount; }
    public void setRiskEventCount(Integer r) { this.riskEventCount = r; }
    public String getMaxRiskCalculationVersion() { return maxRiskCalculationVersion; }
    public void setMaxRiskCalculationVersion(String v) { this.maxRiskCalculationVersion = v; }
    public BigDecimal getExplicitCoverage() { return explicitCoverage; }
    public void setExplicitCoverage(BigDecimal e) { this.explicitCoverage = e; }
    public BigDecimal getInferredConfidence() { return inferredConfidence; }
    public void setInferredConfidence(BigDecimal i) { this.inferredConfidence = i; }
    public String getFeatureVersion() { return featureVersion; }
    public void setFeatureVersion(String f) { this.featureVersion = f; }
    public String getCalculationVersion() { return calculationVersion; }
    public void setCalculationVersion(String c) { this.calculationVersion = c; }
    public String getExtraFeatures() { return extraFeatures; }
    public void setExtraFeatures(String e) { this.extraFeatures = e; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime c) { this.createdAt = c; }
}
