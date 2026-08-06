package com.mindbridge.safety.event.repository;

import com.mindbridge.safety.event.SafetyEventStatus;
import com.mindbridge.safety.event.domain.SafetyEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SafetyEventRepository extends JpaRepository<SafetyEvent, UUID> {

    List<SafetyEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<SafetyEvent> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, SafetyEventStatus status);

    List<SafetyEvent> findByStatusIn(List<SafetyEventStatus> statuses);

    boolean existsByUserIdAndStatusIn(UUID userId, List<SafetyEventStatus> statuses);

    Page<SafetyEvent> findByStatus(SafetyEventStatus status, Pageable pageable);

    Page<SafetyEvent> findByRiskLevel(Short riskLevel, Pageable pageable);

    Page<SafetyEvent> findByStatusAndRiskLevel(
            SafetyEventStatus status, Short riskLevel, Pageable pageable);
}
