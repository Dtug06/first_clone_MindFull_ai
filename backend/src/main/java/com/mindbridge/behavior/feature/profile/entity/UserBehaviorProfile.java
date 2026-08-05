package com.mindbridge.behavior.feature.profile.entity;

import com.mindbridge.behavior.feature.engagement.config.EngagementConfig;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_behavior_profiles")
public class UserBehaviorProfile {

    public static final String PROFILE_VERSION = "profile_v1";
    public static final String CALCULATION_VERSION =
            "feat=v1+trend=v1+topic=engagement_v1_unweighted_top_n_3+eng="
                    + EngagementConfig.CALCULATION_VERSION;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "window_end", nullable = false)
    private LocalDate windowEnd;

    @Column(name = "stress_avg_7d")
    private BigDecimal stressAvg7d;
    @Column(name = "stress_avg_30d")
    private BigDecimal stressAvg30d;

    @Column(name = "mood_avg_7d")
    private BigDecimal moodAvg7d;
    @Column(name = "mood_avg_30d")
    private BigDecimal moodAvg30d;

    @Column(name = "energy_avg_7d")
    private BigDecimal energyAvg7d;
    @Column(name = "energy_avg_30d")
    private BigDecimal energyAvg30d;

    @Column(name = "sleep_avg_7d")
    private BigDecimal sleepAvg7d;
    @Column(name = "sleep_avg_30d")
    private BigDecimal sleepAvg30d;

    @Column(name = "anxiety_avg_7d")
    private BigDecimal anxietyAvg7d;
    @Column(name = "anxiety_avg_30d")
    private BigDecimal anxietyAvg30d;

    @Column(name = "engagement_score_7d")
    private Integer engagementScore7d;
    @Column(name = "engagement_score_30d")
    private Integer engagementScore30d;

    @Column(name = "trend_summary", columnDefinition = "TEXT")
    private String trendSummary;

    @Column(name = "dominant_topics_7d", columnDefinition = "jsonb", nullable = false)
    private String dominantTopics7d;

    @Column(name = "dominant_topics_30d", columnDefinition = "jsonb", nullable = false)
    private String dominantTopics30d;

    @Column(name = "risk_level")
    private Short riskLevel;

    @Column(name = "risk_history_id")
    private UUID riskHistoryId;

    @Column(name = "data_coverage", nullable = false)
    private BigDecimal dataCoverage;

    @Column(name = "confidence", nullable = false)
    private BigDecimal confidence;

    @Column(name = "data_quality_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DataQualityStatus dataQualityStatus;

    @Column(name = "profile_version", nullable = false)
    private String profileVersion;

    @Column(name = "calculation_version", nullable = false)
    private String calculationVersion;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public LocalDate getWindowEnd() { return windowEnd; }
    public void setWindowEnd(LocalDate windowEnd) { this.windowEnd = windowEnd; }

    public BigDecimal getStressAvg7d() { return stressAvg7d; }
    public void setStressAvg7d(BigDecimal v) { this.stressAvg7d = v; }
    public BigDecimal getStressAvg30d() { return stressAvg30d; }
    public void setStressAvg30d(BigDecimal v) { this.stressAvg30d = v; }

    public BigDecimal getMoodAvg7d() { return moodAvg7d; }
    public void setMoodAvg7d(BigDecimal v) { this.moodAvg7d = v; }
    public BigDecimal getMoodAvg30d() { return moodAvg30d; }
    public void setMoodAvg30d(BigDecimal v) { this.moodAvg30d = v; }

    public BigDecimal getEnergyAvg7d() { return energyAvg7d; }
    public void setEnergyAvg7d(BigDecimal v) { this.energyAvg7d = v; }
    public BigDecimal getEnergyAvg30d() { return energyAvg30d; }
    public void setEnergyAvg30d(BigDecimal v) { this.energyAvg30d = v; }

    public BigDecimal getSleepAvg7d() { return sleepAvg7d; }
    public void setSleepAvg7d(BigDecimal v) { this.sleepAvg7d = v; }
    public BigDecimal getSleepAvg30d() { return sleepAvg30d; }
    public void setSleepAvg30d(BigDecimal v) { this.sleepAvg30d = v; }

    public BigDecimal getAnxietyAvg7d() { return anxietyAvg7d; }
    public void setAnxietyAvg7d(BigDecimal v) { this.anxietyAvg7d = v; }
    public BigDecimal getAnxietyAvg30d() { return anxietyAvg30d; }
    public void setAnxietyAvg30d(BigDecimal v) { this.anxietyAvg30d = v; }

    public Integer getEngagementScore7d() { return engagementScore7d; }
    public void setEngagementScore7d(Integer v) { this.engagementScore7d = v; }
    public Integer getEngagementScore30d() { return engagementScore30d; }
    public void setEngagementScore30d(Integer v) { this.engagementScore30d = v; }

    public String getTrendSummary() { return trendSummary; }
    public void setTrendSummary(String v) { this.trendSummary = v; }

    public String getDominantTopics7d() { return dominantTopics7d; }
    public void setDominantTopics7d(String v) { this.dominantTopics7d = v; }

    public String getDominantTopics30d() { return dominantTopics30d; }
    public void setDominantTopics30d(String v) { this.dominantTopics30d = v; }

    public Short getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Short v) { this.riskLevel = v; }

    public UUID getRiskHistoryId() { return riskHistoryId; }
    public void setRiskHistoryId(UUID v) { this.riskHistoryId = v; }

    public BigDecimal getDataCoverage() { return dataCoverage; }
    public void setDataCoverage(BigDecimal v) { this.dataCoverage = v; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal v) { this.confidence = v; }

    public DataQualityStatus getDataQualityStatus() { return dataQualityStatus; }
    public void setDataQualityStatus(DataQualityStatus v) { this.dataQualityStatus = v; }

    public String getProfileVersion() { return profileVersion; }
    public void setProfileVersion(String v) { this.profileVersion = v; }

    public String getCalculationVersion() { return calculationVersion; }
    public void setCalculationVersion(String v) { this.calculationVersion = v; }

    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(OffsetDateTime v) { this.calculatedAt = v; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
}