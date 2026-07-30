package com.mindbridge.common.audit;

import com.mindbridge.common.domain.entity.AuditLog;
import com.mindbridge.common.repository.AuditLogRepository;
import com.mindbridge.common.util.RequestContext;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Writes audit events to {@code audit_logs}.
 *
 * Safety rules:
 * - The persist call runs in its own transaction
 *   ({@link TransactionDefinition#PROPAGATION_REQUIRES_NEW}) so audit rows
 *   commit independently of the caller's transaction (e.g. login failure
 *   rolling back must not erase the audit row).
 * - requestId is taken from MDC when available so an event can be correlated
 *   with a single HTTP request.
 * - Never stores raw passwords, JWTs, or chat content.
 * - All DB errors are swallowed after logging: audit failures must never
 *   break the user's request.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final TransactionTemplate requiresNewTemplate;

    public AuditService(AuditLogRepository repository,
                        PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.requiresNewTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Persists an audit row in its own transaction. Swallows persistence
     * exceptions after warning so audit failures never break the caller's
     * domain logic.
     */
    public void record(AuditCategory category, String action, AuditActorType actorType,
                       UUID actorId, String subjectType, UUID subjectId,
                       String metadata) {
        String requestId = RequestContext.getRequestId().orElse(null);
        try {
            requiresNewTemplate.executeWithoutResult(status -> {
                AuditLog row = AuditLog.create(category, action, actorType, actorId,
                        subjectType, subjectId, requestId, metadata);
                repository.save(row);
            });
        } catch (RuntimeException e) {
            log.warn("Audit row persist failed: category={} action={} actor={} cause={}",
                    category, action, actorId, e.toString());
        }
    }
}