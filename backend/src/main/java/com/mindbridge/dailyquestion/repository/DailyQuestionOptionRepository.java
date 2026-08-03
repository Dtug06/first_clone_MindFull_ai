package com.mindbridge.dailyquestion.repository;

import com.mindbridge.dailyquestion.domain.DailyQuestionOption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyQuestionOptionRepository extends JpaRepository<DailyQuestionOption, UUID> {

    /**
     * Returns all options for a template, ordered by display order.
     */
    List<DailyQuestionOption> findByTemplateIdOrderByOrderIndexAsc(UUID templateId);
}
