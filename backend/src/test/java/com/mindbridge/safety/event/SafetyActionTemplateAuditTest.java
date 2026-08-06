package com.mindbridge.safety.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.safety.event.domain.SafetyAction;
import com.mindbridge.safety.event.repository.SafetyActionRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration test for the G3-T12 template-audit columns on
 * {@code safety_actions} ({@code template_id} + {@code template_version}).
 *
 * <p>Exercises the full persistence path against the H2 schema mirror
 * ({@code schema-safety-events.sql}, which now carries the V19 ALTER).
 * Verifies:
 * <ul>
 *   <li>The two new columns exist and accept NULL on a fresh row.</li>
 *   <li>{@code markSucceeded(templateId, templateVersion)} persists both
 *       columns and can be read back through JPA.</li>
 *   <li>{@code markFailed(templateId, templateVersion, errorMessage)}
 *       persists both columns + the error message.</li>
 *   <li>{@code markSkipped(null, null, reason)} keeps the columns NULL
 *       while transitioning to SKIPPED.</li>
 *   <li>{@code markSkipped(templateId, templateVersion, reason)}
 *       records the default-row reference for audit.</li>
 * </ul>
 *
 * <p>The test does NOT exercise the full chat pipeline
 * (chat message -> resolver -> SafetyEvent creation). It uses the
 * existing {@code SafetyAction.pending(...)} factory + the controlled
 * markXxx transitions to write directly. End-to-end coverage lives in
 * {@code SafetyEventServiceIntegrationTest} (T11) which still passes.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mindbridge.ai.provider=mock",
        "mindbridge.ai.mock.force-scenario="
})
@Sql(scripts = {
        "/schema-safety-events.sql"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("SafetyAction template-audit (G3-T12) integration")
class SafetyActionTemplateAuditTest {

    @Autowired
    private SafetyActionRepository repository;

    @BeforeEach
    void cleanTable() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("fresh PENDING row has null template_id + template_version")
    void freshPendingHasNulls() {
        UUID id = UUID.randomUUID();
        SafetyAction a = SafetyAction.pending(
                id, UUID.randomUUID(), SafetyActionType.SHOW_TEMPLATE);
        SafetyAction saved = repository.save(a);

        SafetyAction reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getTemplateId()).isNull();
        assertThat(reloaded.getTemplateVersion()).isNull();
        assertThat(reloaded.getStatus()).isEqualTo(SafetyActionStatus.PENDING);
        // Sanity: the saved id round-trips.
        assertThat(reloaded.getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("markSucceeded(templateId, version) persists both columns")
    void markSucceededPersistsBothColumns() {
        UUID id = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        SafetyAction a = SafetyAction.pending(
                id, UUID.randomUUID(), SafetyActionType.SHOW_TEMPLATE);
        repository.save(a);

        a.markSucceeded(templateId, "v1");
        repository.save(a);

        SafetyAction reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SafetyActionStatus.SUCCEEDED);
        assertThat(reloaded.getTemplateId()).isEqualTo(templateId);
        assertThat(reloaded.getTemplateVersion()).isEqualTo("v1");
        assertThat(reloaded.getExecutedAt()).isNotNull();
    }

    @Test
    @DisplayName("markFailed(templateId, version, error) persists both columns + error")
    void markFailedPersistsAllThreeColumns() {
        UUID id = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        SafetyAction a = SafetyAction.pending(
                id, UUID.randomUUID(), SafetyActionType.SHOW_TEMPLATE);
        repository.save(a);

        a.markFailed(templateId, "v2", "delivery pipeline exception");
        repository.save(a);

        SafetyAction reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SafetyActionStatus.FAILED);
        assertThat(reloaded.getTemplateId()).isEqualTo(templateId);
        assertThat(reloaded.getTemplateVersion()).isEqualTo("v2");
        assertThat(reloaded.getErrorMessage()).isEqualTo("delivery pipeline exception");
    }

    @Test
    @DisplayName("markSkipped(null, null, reason) keeps the template columns NULL")
    void markSkippedWithoutTemplateKeepsColumnsNull() {
        UUID id = UUID.randomUUID();
        SafetyAction a = SafetyAction.pending(
                id, UUID.randomUUID(), SafetyActionType.SHOW_TEMPLATE);
        repository.save(a);

        a.markSkipped(null, null, "no approved template for locale=vi reason=DEFAULT");
        repository.save(a);

        SafetyAction reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SafetyActionStatus.SKIPPED);
        assertThat(reloaded.getTemplateId()).isNull();
        assertThat(reloaded.getTemplateVersion()).isNull();
        assertThat(reloaded.getErrorMessage())
                .isEqualTo("no approved template for locale=vi reason=DEFAULT");
    }

    @Test
    @DisplayName("markSkipped(templateId, version, reason) records default-row reference")
    void markSkippedWithDefaultRecordsReference() {
        UUID id = UUID.randomUUID();
        UUID defaultTemplateId = UUID.randomUUID();
        SafetyAction a = SafetyAction.pending(
                id, UUID.randomUUID(), SafetyActionType.SHOW_TEMPLATE);
        repository.save(a);

        a.markSkipped(defaultTemplateId, "default-v1",
                "specific row missing - locale default served");
        repository.save(a);

        SafetyAction reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SafetyActionStatus.SKIPPED);
        assertThat(reloaded.getTemplateId()).isEqualTo(defaultTemplateId);
        assertThat(reloaded.getTemplateVersion()).isEqualTo("default-v1");
    }

    @Test
    @DisplayName("non-template overloads (PAUSE_PROGRAM path) keep template columns NULL")
    void noTemplateOverloadsKeepNulls() {
        UUID id = UUID.randomUUID();
        SafetyAction a = SafetyAction.pending(
                id, UUID.randomUUID(), SafetyActionType.PAUSE_PROGRAM);
        repository.save(a);

        a.markSucceeded();
        repository.save(a);

        SafetyAction reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SafetyActionStatus.SUCCEEDED);
        assertThat(reloaded.getTemplateId()).isNull();
        assertThat(reloaded.getTemplateVersion()).isNull();
    }
}