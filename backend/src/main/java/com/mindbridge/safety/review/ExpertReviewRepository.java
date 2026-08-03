package com.mindbridge.safety.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ExpertReviewRepository extends JpaRepository<ExpertReview, UUID> {
    boolean existsBySafetyEventIdAndReviewerId(UUID safetyEventId, UUID reviewerId);
    Page<ExpertReview> findBySafetyEventId(UUID safetyEventId, Pageable pageable);
    Page<ExpertReview> findByReviewerId(UUID reviewerId, Pageable pageable);
    long countBySafetyEventId(UUID safetyEventId);
}