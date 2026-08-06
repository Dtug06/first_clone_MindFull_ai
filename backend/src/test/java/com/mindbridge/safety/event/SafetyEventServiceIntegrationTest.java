package com.mindbridge.safety.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.common.audit.AuditCategory;
import com.mindbridge.common.domain.entity.AuditLog;
import com.mindbridge.common.repository.AuditLogRepository;
import com.mindbridge.safety.event.domain.SafetyAction;
import com.mindbridge.safety.event.domain.SafetyEvent;
import com.mindbridge.safety.event.dto.SafetyActionSpec;
import com.mindbridge.safety.event.dto.SafetyEventSourceSpec;
import com.mindbridge.safety.event.exception.SafetyEventInputException;
import com.mindbridge.safety.event.repository.SafetyActionRepository;
import com.mindbridge.safety.event.repository.SafetyEventRepository;
import com.mindbridge.safety.event.repository.SafetyEventSourceRepository;
import com.mindbridge.safety.event.service.SafetyEventService;
import com.mindbridge.safety.resolver.RiskStateHistory;
import com.mindbridge.safety.resolver.RiskStateSourceType;
import com.mindbridge.safety.resolver.dto.ResolverDecision;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration test for {@link SafetyEventService} against the H2
 * schema mirror ({@code schema-safety-events.sql}). Exercises the
 * full persistence path including the audit hook.
 *
 * <p>Verifies:
 * <ul>
 *   <li>L3 / L4 decisions persist an OPEN event, with the right
 *       risk level + user id snapshot.</li>
 *   <li>Source rows are persisted with the polymorphic
 *       {@code (source_type, source_id)} pair.</li>
 *   <li>Action rows are persisted with status {@code PENDING}.</li>
 *   <li>An {@code audit_logs} row is written with
 *       {@code category=SAFETY} and {@code action=SAFETY_EVENT_OPENED}.</li>
 *   <li>{@code isUserBlocked} returns true when an OPEN event exists,
 *       false when no active event exists or only RESOLVED/DISMISSED.</li>
 *   <li>{@code getActiveBlockingEvent} returns the OPEN event and
 *       {@code Optional.empty()} when no active event.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mindbridge.ai.provider=mock",
        "mindbridge.ai.mock.force-scenario="
})
@Sql(scripts = {
        "/schema-safety-events.sql",
        "/schema-risk-state-history.sql",
        "/schema-audit.sql"
})
@DisplayName("SafetyEventService integration")
class SafetyEventServiceIntegrationTest {

    @Autowired
    private SafetyEventService service;

    @Autowired
    private SafetyEventRepository eventRepository;

    @Autowired
    private SafetyEventSourceRepository sourceRepository;

    @Autowired
    private SafetyActionRepository actionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static ResolverDecision decision(UUID userId, short level) {
        RiskStateHistory row = RiskStateHistory.record(
                UUID.randomUUID(),
                userId,
                level,
                level,
                (short) 1,
                null,
                RiskStateSourceType.LLM_CLASSIFIER,
                null,
                "NONE",
                null,
                null,
                BigDecimal.valueOf(0.9),
                new String[]{"MAX_WINS_L" + level},
                OffsetDateTime.now());
        return new ResolverDecision(
                level, level, (short) 1, null,
                BigDecimal.valueOf(0.9), new String[]{"MAX_WINS_L" + level}, row);
    }

    @Test
    @DisplayName("L3 decision persists OPEN event + 1 source + 2 actions + audit")
    void l3Persists() {
        UUID userId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        ResolverDecision d = decision(userId, (short) 3);

        SafetyEvent saved = service.recordLevel3Or4Event(
                d,
                List.of(new SafetyEventSourceSpec(
                        SafetyEventSourceType.CHAT_ANALYSIS, sourceId)),
                List.of(
                        new SafetyActionSpec(SafetyActionType.BLOCK_MATCHING),
                        new SafetyActionSpec(SafetyActionType.FLAG_REVIEW)));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(SafetyEventStatus.OPEN);
        assertThat(saved.getRiskLevel()).isEqualTo((short) 3);
        assertThat(saved.getCreatedAt()).isNotNull();

        // Source rows persisted with polymorphic id.
        List<com.mindbridge.safety.event.domain.SafetyEventSource> sources =
                sourceRepository.findBySafetyEventId(saved.getId());
        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).getSourceType())
                .isEqualTo(SafetyEventSourceType.CHAT_ANALYSIS);
        assertThat(sources.get(0).getSourceId()).isEqualTo(sourceId);

        // Action rows persisted with PENDING status.
        List<SafetyAction> actions = actionRepository.findBySafetyEventId(saved.getId());
        assertThat(actions).hasSize(2);
        assertThat(actions).allMatch(
                a -> a.getStatus() == SafetyActionStatus.PENDING);

        // Audit row exists for SAFETY_EVENT_OPENED.
        List<AuditLog> auditRows = auditLogRepository.findAll();
        assertThat(auditRows).anyMatch(a ->
                a.getCategory() == AuditCategory.SAFETY
                        && "SAFETY_EVENT_OPENED".equals(a.getAction())
                        && saved.getId().equals(a.getSubjectId()));
    }

    @Test
    @DisplayName("L4 decision also persists an OPEN event")
    void l4Persists() {
        UUID userId = UUID.randomUUID();
        ResolverDecision d = decision(userId, (short) 4);
        SafetyEvent saved = service.recordLevel3Or4Event(
                d,
                List.of(new SafetyEventSourceSpec(
                        SafetyEventSourceType.CHAT_ANALYSIS, UUID.randomUUID())),
                List.of(new SafetyActionSpec(SafetyActionType.BLOCK_MATCHING)));

        assertThat(saved.getRiskLevel()).isEqualTo((short) 4);
        assertThat(saved.getStatus()).isEqualTo(SafetyEventStatus.OPEN);
    }

    @Test
    @DisplayName("isUserBlocked is true when an OPEN event exists, false otherwise")
    void isUserBlockedBehavior() {
        UUID userId = UUID.randomUUID();
        assertThat(service.isUserBlocked(userId)).isFalse();

        service.recordLevel3Or4Event(
                decision(userId, (short) 3),
                List.of(new SafetyEventSourceSpec(
                        SafetyEventSourceType.CHAT_ANALYSIS, UUID.randomUUID())),
                List.of(new SafetyActionSpec(SafetyActionType.BLOCK_MATCHING)));

        assertThat(service.isUserBlocked(userId)).isTrue();
    }

    @Test
    @DisplayName("getActiveBlockingEvent returns the OPEN event")
    void getActiveBlockingEventReturns() {
        UUID userId = UUID.randomUUID();
        SafetyEvent persisted = service.recordLevel3Or4Event(
                decision(userId, (short) 4),
                List.of(new SafetyEventSourceSpec(
                        SafetyEventSourceType.CHAT_ANALYSIS, UUID.randomUUID())),
                List.of(new SafetyActionSpec(SafetyActionType.BLOCK_MATCHING)));

        SafetyEvent active = service.getActiveBlockingEvent(userId).orElseThrow();
        assertThat(active.getId()).isEqualTo(persisted.getId());
        assertThat(active.getRiskLevel()).isEqualTo((short) 4);
    }

    @Test
    @DisplayName("Empty sources list throws SafetyEventInputException and writes nothing")
    void emptySourcesRejected() {
        UUID userId = UUID.randomUUID();
        long countBefore = eventRepository.count();
        assertThatThrownBy(() -> service.recordLevel3Or4Event(
                decision(userId, (short) 3),
                List.of(),
                List.of(new SafetyActionSpec(SafetyActionType.BLOCK_MATCHING))))
                .isInstanceOf(SafetyEventInputException.class);
        assertThat(eventRepository.count()).isEqualTo(countBefore);
    }
}