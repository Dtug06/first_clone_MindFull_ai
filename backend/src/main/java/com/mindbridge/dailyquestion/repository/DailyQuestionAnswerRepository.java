package com.mindbridge.dailyquestion.repository;

import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}