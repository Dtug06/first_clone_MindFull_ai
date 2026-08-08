package com.mindbridge.behavior.feature.window.repository;

import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDailyFeatureWindowRepository extends JpaRepository<UserDailyFeature, UUID> {

    @Query("SELECT f FROM UserDailyFeature f WHERE f.userId = :userId AND f.featureDate >= :windowStart AND f.featureDate <= :targetDate ORDER BY f.featureDate ASC")
    List<UserDailyFeature> findByUserAndWindow(@Param("userId") UUID userId, @Param("windowStart") LocalDate windowStart, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT COUNT(DISTINCT f.featureDate) FROM UserDailyFeature f WHERE f.userId = :userId AND f.featureDate >= :windowStart AND f.featureDate <= :targetDate AND f.stressScore IS NOT NULL")
    long countDaysWithStress(@Param("userId") UUID userId, @Param("windowStart") LocalDate windowStart, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT COUNT(DISTINCT f.featureDate) FROM UserDailyFeature f WHERE f.userId = :userId AND f.featureDate >= :windowStart AND f.featureDate <= :targetDate AND f.moodScore IS NOT NULL")
    long countDaysWithMood(@Param("userId") UUID userId, @Param("windowStart") LocalDate windowStart, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT COUNT(DISTINCT f.featureDate) FROM UserDailyFeature f WHERE f.userId = :userId AND f.featureDate >= :windowStart AND f.featureDate <= :targetDate AND f.energyScore IS NOT NULL")
    long countDaysWithEnergy(@Param("userId") UUID userId, @Param("windowStart") LocalDate windowStart, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT COUNT(DISTINCT f.featureDate) FROM UserDailyFeature f WHERE f.userId = :userId AND f.featureDate >= :windowStart AND f.featureDate <= :targetDate AND f.sleepHours IS NOT NULL")
    long countDaysWithSleep(@Param("userId") UUID userId, @Param("windowStart") LocalDate windowStart, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT COUNT(DISTINCT f.featureDate) FROM UserDailyFeature f WHERE f.userId = :userId AND f.featureDate >= :windowStart AND f.featureDate <= :targetDate AND f.anxietySignal IS NOT NULL")
    long countDaysWithAnxietySignal(@Param("userId") UUID userId, @Param("windowStart") LocalDate windowStart, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT COUNT(DISTINCT f.featureDate) FROM UserDailyFeature f WHERE f.userId = :userId AND f.featureDate >= :windowStart AND f.featureDate <= :targetDate AND f.engagementScore IS NOT NULL")
    long countDaysWithEngagement(@Param("userId") UUID userId, @Param("windowStart") LocalDate windowStart, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT COUNT(DISTINCT f.featureDate) FROM UserDailyFeature f WHERE f.userId = :userId AND f.featureDate >= :windowStart AND f.featureDate <= :targetDate AND f.maxRiskLevel IS NOT NULL")
    long countDaysWithMaxRisk(@Param("userId") UUID userId, @Param("windowStart") LocalDate windowStart, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT COUNT(DISTINCT f.featureDate) FROM UserDailyFeature f WHERE f.userId = :userId AND f.featureDate >= :windowStart AND f.featureDate <= :targetDate AND (f.stressScore IS NOT NULL OR f.moodScore IS NOT NULL OR f.energyScore IS NOT NULL OR f.sleepHours IS NOT NULL)")
    long countDaysWithExplicitData(@Param("userId") UUID userId, @Param("windowStart") LocalDate windowStart, @Param("targetDate") LocalDate targetDate);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM UserDailyFeature f WHERE f.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}
