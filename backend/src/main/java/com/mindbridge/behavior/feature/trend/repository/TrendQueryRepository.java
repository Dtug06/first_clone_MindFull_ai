package com.mindbridge.behavior.feature.trend.repository;

import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * G4-T07 repository for streak queries that the trend calculator needs in
 * addition to the 7d/30d aggregates already exposed by
 * {@code WindowAggregationService}.
 *
 * <p>Why a separate repository:
 * <ul>
 *   <li>Check-in streak queries {@code daily_question_answers}, not
 *       {@code user_daily_features}.</li>
 *   <li>High-stress streak filters {@code user_daily_features} by
 *       {@code stress_score >= :highStressThreshold} which T06 does NOT
 *       expose (T06 returns only the count of days, not the day list).</li>
 * </ul>
 *
 * <p>All queries are JPQL and parameterised; no native SQL.
 */
@Repository
public interface TrendQueryRepository extends JpaRepository<UserDailyFeature, UUID> {

    /**
     * Returns the distinct local_dates within {@code [from..to]} (inclusive)
     * for which the user has at least one {@code daily_question_answers}
     * row. Used to compute the {@code checkInStreak} (consecutive days back
     * from {@code targetDate}).
     *
     * <p>Filtering is on {@code assignment.assignedForDate} (NOT
     * {@code answer.answeredAt}) per the late-arriving policy established in
     * G4-T03 (Q1=A): an answer submitted late is still attributed to its
     * original assignment's local date.
     */
    @Query("""
            SELECT DISTINCT assg.assignedForDate
            FROM DailyQuestionAnswer a
              JOIN a.assignment assg
            WHERE a.userId = :userId
              AND assg.assignedForDate >= :from
              AND assg.assignedForDate <= :to
            ORDER BY assg.assignedForDate DESC
            """)
    List<LocalDate> findCheckInDatesByUserInRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Returns the distinct {@code feature_date} values within
     * {@code [from..to]} for which {@code stress_score >= :threshold}.
     * Used to compute {@code highStressStreak}. The threshold is the
     * normalized 0-1 {@code HIGH_STRESS_THRESHOLD} value (caller-supplied
     * via {@code TrendConfig}).
     */
    @Query("""
            SELECT DISTINCT f.featureDate
            FROM UserDailyFeature f
            WHERE f.userId = :userId
              AND f.featureDate >= :from
              AND f.featureDate <= :to
              AND f.stressScore IS NOT NULL
              AND f.stressScore >= :threshold
            ORDER BY f.featureDate DESC
            """)
    List<LocalDate> findHighStressDatesByUserInRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("threshold") java.math.BigDecimal threshold);
}