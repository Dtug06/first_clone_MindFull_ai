package com.mindbridge.safety.event.repository;

import com.mindbridge.safety.event.domain.SafetyAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SafetyActionRepository extends JpaRepository<SafetyAction, UUID> {

    List<SafetyAction> findBySafetyEventId(UUID safetyEventId);
}