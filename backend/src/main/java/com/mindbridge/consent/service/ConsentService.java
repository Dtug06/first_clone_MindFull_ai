package com.mindbridge.consent.service;

import com.mindbridge.common.audit.AuditActorType;
import com.mindbridge.common.audit.AuditActions;
import com.mindbridge.common.audit.AuditCategory;
import com.mindbridge.common.audit.AuditService;
import com.mindbridge.common.service.CurrentUserService;
import com.mindbridge.consent.domain.ConsentEvent;
import com.mindbridge.consent.domain.enums.ConsentAction;
import com.mindbridge.consent.domain.enums.ConsentType;
import com.mindbridge.consent.dto.ConsentEventRequest;
import com.mindbridge.consent.dto.ConsentEventResponse;
import com.mindbridge.consent.dto.CurrentConsentResponse;
import com.mindbridge.consent.mapper.ConsentEventMapper;
import com.mindbridge.consent.repository.ConsentEventRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only consent management.
 *
 * Invariants:
 * - Each grant/revoke inserts a new row. No update or delete.
 * - Current consent = latest event per (userId, consentType) by occurredAt.
 * - If no event exists for a type, current state is granted=false with null policy.
 */
@Service
public class ConsentService {

    private final ConsentEventRepository repository;
    private final ConsentEventMapper mapper;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public ConsentService(ConsentEventRepository repository,
                          ConsentEventMapper mapper,
                          CurrentUserService currentUserService,
                          AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    /**
     * Records a consent event for the current user (granted or revoked).
     * Always appends — never updates existing rows.
     */
    @Transactional
    public ConsentEventResponse recordConsent(ConsentEventRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        ConsentEvent event = ConsentEvent.record(
                userId,
                request.consentType(),
                request.action(),
                request.policyVersion(),
                null
        );
        ConsentEvent saved = repository.save(event);

        String actionCode = request.action() == ConsentAction.GRANTED
                ? AuditActions.CONSENT_GRANTED
                : AuditActions.CONSENT_REVOKED;
        String metadata = "{\"consentType\":\"" + request.consentType().name()
                + "\",\"policyVersion\":\"" + request.policyVersion() + "\"}";
        auditService.record(AuditCategory.CONSENT, actionCode,
                AuditActorType.USER, userId, "consent_event", saved.getId(), metadata);

        return mapper.toResponse(saved);
    }

    /**
     * Returns the current consent state for all consent types for the current user.
     * If no event exists for a type, returns granted=false with null policy.
     */
    @Transactional(readOnly = true)
    public List<CurrentConsentResponse> getCurrentConsentStates() {
        UUID userId = currentUserService.getCurrentUserId();
        Map<ConsentType, ConsentEvent> latestByType = new EnumMap<>(ConsentType.class);

        for (ConsentEvent event : repository.findLatestPerTypeByUser(userId)) {
            latestByType.putIfAbsent(event.getConsentType(), event);
        }

        List<CurrentConsentResponse> result = new ArrayList<>();
        for (ConsentType type : ConsentType.values()) {
            ConsentEvent event = latestByType.get(type);
            if (event == null) {
                result.add(new CurrentConsentResponse(type, false, null, null));
            } else {
                boolean granted = event.getAction() == ConsentAction.GRANTED;
                result.add(new CurrentConsentResponse(
                        type, granted, event.getPolicyVersion(), event.getOccurredAt()));
            }
        }
        return result;
    }
}