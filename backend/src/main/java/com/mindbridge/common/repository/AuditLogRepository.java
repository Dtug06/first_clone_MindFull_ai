package com.mindbridge.common.repository;

import com.mindbridge.common.domain.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, java.util.UUID> {

    List<AuditLog> findByRequestId(String requestId);
}