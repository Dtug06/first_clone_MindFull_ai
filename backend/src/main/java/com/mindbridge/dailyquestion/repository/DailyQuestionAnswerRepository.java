package com.mindbridge.dailyquestion.repository;

import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyQuestionAnswerRepository extends JpaRepository<DailyQuestionAnswer, UUID> {

    /**
     * Returns true if an answer already exists for the given assignment.
     * Used by the service to fail fast before attempting INSERT (which would
     * also fail at the DB UNIQUE constraint, but this gives a clearer 409 path).
     */
    boolean existsByAssignmentId(UUID assignmentId);

    /**
     * Returns the user's answers in the given time window, newest first.
     * Used by the history endpoint.
     */
    List<DailyQuestionAnswer> findByUserIdAndAnsweredAtBetweenOrderByAnsweredAtDesc(
            UUID userId, Instant from, Instant to);

    /**
     * G4-T03: returns the user's answered {@code daily_question_answers} rows
     * whose parent assignment has {@code assignedForDate = localDate}.
     *
     * <p>This is the Q1=A (Recommended) policy: filter by the assignment's
     * local-date, NOT by {@code answered_at}. This correctly handles late-
     * arriving answers - an answer submitted at 23:55 UTC for a question
     * assigned for local-date 2026-08-04 is returned even if the user's TZ
     * is UTC+9 and 23:55 UTC is already 2026-08-05 local.
     *
     * <p>The query uses {@code JOIN FETCH} on the {@code assignment} association
     * to avoid an N+1 when the service maps answers into
     * {@code DailySourceAggregation.ExplicitAnswer} (which reads
     * {@code assignment.templateVersion} - one more join).
     *
     * <p>{@code assignedForDate} is the calendar date in the user's TZ at the
     * moment of assignment creation, per DB-MVP section 4.5.
     */
    @Query("""
            SELECT a
            FROM DailyQuestionAnswer a
              JOIN FETCH a.assignment assg
            WHERE a.userId = :userId
              AND assg.assignedForDate = :assignedForDate
            ORDER BY a.answeredAt ASC
            """)
    List<DailyQuestionAnswer> findWithAssignmentByUserIdAndAssignedForDate(
            @Param("userId") UUID userId,
            @Param("assignedForDate") LocalDate assignedForDate);
}