package com.mindbridge.safety.event.repository;

import com.mindbridge.safety.event.domain.SafetyEventSource;
import com.mindbridge.safety.event.SafetyEventSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SafetyEventSourceRepository extends JpaRepository<SafetyEventSource, UUID> {

    List<SafetyEventSource> findBySafetyEventId(UUID safetyEventId);

    List<SafetyEventSource> findBySourceTypeAndSourceId(SafetyEventSourceType sourceType, UUID sourceId);
}