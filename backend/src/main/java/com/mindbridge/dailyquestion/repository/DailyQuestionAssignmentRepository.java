package com.mindbridge.dailyquestion.repository;

import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyQuestionAssignmentRepository extends JpaRepository<DailyQuestionAssignment, UUID> {

    /**
     * Returns all assignments for a user on a specific local date.
     * Used by the service to decide whether to create new assignments.
     */
    List<DailyQuestionAssignment> findByUserIdAndAssignedForDateOrderByTemplateCodeAsc(
            UUID userId, LocalDate assignedForDate);

    /**
     * Returns all assignments for a user (any date) — for admin/debug use only.
     */
    List<DailyQuestionAssignment> findByUserIdOrderByAssignedForDateDesc(UUID userId);

    /** Number of Daily questions actually assigned to the user for that local date. */
    long countByUserIdAndAssignedForDate(UUID userId, LocalDate assignedForDate);
}
