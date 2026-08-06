package com.mindbridge.safety.event.service;

import com.mindbridge.common.audit.AuditActorType;
import com.mindbridge.common.audit.AuditCategory;
import com.mindbridge.common.audit.AuditService;
import com.mindbridge.safety.event.SafetyActionStatus;
import com.mindbridge.safety.event.SafetyActionType;
import com.mindbridge.safety.event.SafetyEventStatus;
import com.mindbridge.safety.event.domain.SafetyAction;
import com.mindbridge.safety.event.domain.SafetyEvent;
import com.mindbridge.safety.event.domain.SafetyEventSource;
import com.mindbridge.safety.event.dto.SafetyActionSpec;
import com.mindbridge.safety.event.dto.SafetyEventSourceSpec;
import com.mindbridge.safety.event.exception.SafetyEventInputException;
import com.mindbridge.safety.event.repository.SafetyActionRepository;
import com.mindbridge.safety.event.repository.SafetyEventRepository;
import com.mindbridge.safety.event.repository.SafetyEventSourceRepository;
import com.mindbridge.safety.resolver.dto.ResolverDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SafetyEventService {

        /**
     * Minimum risk level that should trigger a Safety Event and
     * matching block. Per docs/04_SAFETY_AND_CBT_RULES.md and the
     * G3-T11 recordLevel3Or4Event guard, anything L3+ opens an event.
     * Referenced by ConversationMessageService after every resolver
     * decision (constant hoisted here so the chat pipeline does not
     * hard-code the threshold).
     */
    public static final short BLOCKING_THRESHOLD = 3;

    private final SafetyEventRepository eventRepository;
    private final SafetyEventSourceRepository sourceRepository;
    private final SafetyActionRepository actionRepository;
    private final AuditService auditService;

    public SafetyEventService(
            SafetyEventRepository eventRepository,
            SafetyEventSourceRepository sourceRepository,
            SafetyActionRepository actionRepository,
            AuditService auditService) {
        this.eventRepository = eventRepository;
        this.sourceRepository = sourceRepository;
        this.actionRepository = actionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SafetyEvent recordLevel3Or4Event(
            ResolverDecision decision,
            List<SafetyEventSourceSpec> sources,
            List<SafetyActionSpec> actions) {

        if (decision == null) {
            throw new SafetyEventInputException("decision must not be null");
        }

        short riskLevel = decision.finalRiskLevel();
        if (riskLevel < 3 || riskLevel > 4) {
            throw new SafetyEventInputException(
                    "riskLevel must be in range [3, 4], got: " + riskLevel);
        }

        if (sources == null || sources.isEmpty()) {
            throw new SafetyEventInputException(
                    "At least one source is required per Safety history rules");
        }

        if (actions == null || actions.isEmpty()) {
            throw new SafetyEventInputException(
                    "At least one action is required");
        }

        SafetyEvent event = SafetyEvent.open(
                UUID.randomUUID(),
                decision.historyRow(),
                decision.finalRiskLevel() + " - " + String.join(",", decision.reasonCodes())
        );

        SafetyEvent savedEvent = eventRepository.save(event);

        List<SafetyEventSource> sourceRows = new ArrayList<>();
        for (SafetyEventSourceSpec spec : sources) {
            SafetyEventSource source = SafetyEventSource.of(
                    UUID.randomUUID(),
                    savedEvent.getId(),
                    spec.sourceType(),
                    spec.sourceId()
            );
            sourceRows.add(source);
        }
        sourceRepository.saveAll(sourceRows);

        List<SafetyAction> actionRows = new ArrayList<>();
        for (SafetyActionSpec spec : actions) {
            SafetyAction action = SafetyAction.pending(
                    UUID.randomUUID(),
                    savedEvent.getId(),
                    spec.actionType()
            );
            actionRows.add(action);
        }
        actionRepository.saveAll(actionRows);

        auditService.record(
                AuditCategory.SAFETY,
                "SAFETY_EVENT_OPENED",
                AuditActorType.SYSTEM,
                savedEvent.getUserId(),
                "SafetyEvent",
                savedEvent.getId(),
                "riskLevel=" + savedEvent.getRiskLevel() + ",reasonCodes=" +
                        String.join(",", decision.reasonCodes())
        );

        return savedEvent;
    }

    @Transactional(readOnly = true)
    public boolean isUserBlocked(UUID userId) {
        List<SafetyEventStatus> blockingStatuses = List.of(
                SafetyEventStatus.OPEN,
                SafetyEventStatus.UNDER_REVIEW
        );
        return eventRepository.existsByUserIdAndStatusIn(userId, blockingStatuses);
    }

    @Transactional(readOnly = true)
    public Optional<SafetyEvent> getActiveBlockingEvent(UUID userId) {
        List<SafetyEvent> events = eventRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, SafetyEventStatus.OPEN);
        if (!events.isEmpty()) {
            return Optional.of(events.get(0));
        }
        events = eventRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, SafetyEventStatus.UNDER_REVIEW);
        if (!events.isEmpty()) {
            return Optional.of(events.get(0));
        }
        return Optional.empty();
    }

    /** Record the exact approved template delivered for a SHOW_TEMPLATE action. */
    @Transactional
    public void markShowTemplateSucceeded(
            UUID safetyEventId, UUID templateId, String templateVersion) {
        SafetyAction action = requirePendingShowTemplate(safetyEventId);
        action.markSucceeded(templateId, templateVersion);
        actionRepository.save(action);
    }

    /** Record that no approved template was available; never invent content. */
    @Transactional
    public void markShowTemplateSkipped(UUID safetyEventId, String reason) {
        SafetyAction action = requirePendingShowTemplate(safetyEventId);
        action.markSkipped(null, null, reason);
        actionRepository.save(action);
    }

    private SafetyAction requirePendingShowTemplate(UUID safetyEventId) {
        return actionRepository.findBySafetyEventId(safetyEventId).stream()
                .filter(action -> action.getActionType() == SafetyActionType.SHOW_TEMPLATE)
                .filter(action -> action.getStatus() == SafetyActionStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new SafetyEventInputException(
                        "Pending SHOW_TEMPLATE action not found for SafetyEvent "
                                + safetyEventId));
    }
}
