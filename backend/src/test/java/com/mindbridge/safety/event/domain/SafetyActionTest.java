package com.mindbridge.safety.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.safety.event.SafetyActionStatus;
import com.mindbridge.safety.event.SafetyActionType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link SafetyAction} entity. Focuses on factory
 * validation, the default PENDING status contract, and the G3-T12
 * template-audit controlled transitions
 * ({@code markSucceeded(UUID, String)}, {@code markFailed(UUID, String,
 * String)}, {@code markSkipped(UUID, String, String)}).
 *
 * <p>JPA persistence behaviour is exercised separately by the
 * integration test.
 */
@DisplayName("SafetyAction")
class SafetyActionTest {

    @Nested
    @DisplayName("Factory validation")
    class FactoryValidation {

        @Test
        @DisplayName("pending() rejects null id")
        void nullId() {
            assertThatThrownBy(() -> SafetyAction.pending(
                    null, UUID.randomUUID(),
                    SafetyActionType.BLOCK_MATCHING))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("pending() rejects null safetyEventId")
        void nullSafetyEventId() {
            assertThatThrownBy(() -> SafetyAction.pending(
                    UUID.randomUUID(), null,
                    SafetyActionType.BLOCK_MATCHING))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("pending() rejects null actionType")
        void nullActionType() {
            assertThatThrownBy(() -> SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("pending() starts with null template_id + template_version")
        void pendingTemplateFieldsAreNull() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            assertThat(a.getTemplateId()).isNull();
            assertThat(a.getTemplateVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("Pending-by-default contract")
    class PendingByDefault {

        @Test
        @DisplayName("pending() always sets status to PENDING")
        void statusIsPending() {
            UUID eventId = UUID.randomUUID();
            for (SafetyActionType type : SafetyActionType.values()) {
                SafetyAction a = SafetyAction.pending(
                        UUID.randomUUID(), eventId, type);
                assertThat(a.getStatus()).isEqualTo(SafetyActionStatus.PENDING);
                assertThat(a.getErrorMessage()).isNull();
                assertThat(a.getExecutedAt()).isNull();
                assertThat(a.getActionType()).isEqualTo(type);
            }
        }
    }

    @Nested
    @DisplayName("G3-T12 template-audit transitions")
    class TemplateAuditTransitions {

        private final UUID templateId = UUID.randomUUID();

        @Test
        @DisplayName("markSucceeded(templateId, version) records both fields")
        void succeededRecordsTemplate() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            a.markSucceeded(templateId, "v1");

            assertThat(a.getStatus()).isEqualTo(SafetyActionStatus.SUCCEEDED);
            assertThat(a.getTemplateId()).isEqualTo(templateId);
            assertThat(a.getTemplateVersion()).isEqualTo("v1");
            assertThat(a.getExecutedAt()).isNotNull();
            assertThat(a.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("markFailed(templateId, version, error) records both fields + error")
        void failedRecordsTemplateAndError() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            a.markFailed(templateId, "v1", "boom");

            assertThat(a.getStatus()).isEqualTo(SafetyActionStatus.FAILED);
            assertThat(a.getTemplateId()).isEqualTo(templateId);
            assertThat(a.getTemplateVersion()).isEqualTo("v1");
            assertThat(a.getErrorMessage()).isEqualTo("boom");
            assertThat(a.getExecutedAt()).isNotNull();
        }

        @Test
        @DisplayName("markSkipped(null, null, reason) records null template fields")
        void skippedWithoutTemplateRecordsNulls() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            a.markSkipped(null, null, "no approved template");

            assertThat(a.getStatus()).isEqualTo(SafetyActionStatus.SKIPPED);
            assertThat(a.getTemplateId()).isNull();
            assertThat(a.getTemplateVersion()).isNull();
            assertThat(a.getErrorMessage()).isEqualTo("no approved template");
        }

        @Test
        @DisplayName("markSkipped(templateId, version, reason) records both fields")
        void skippedWithDefaultTemplateRecordsFields() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            a.markSkipped(templateId, "v1", "specific row missing - default used");

            assertThat(a.getStatus()).isEqualTo(SafetyActionStatus.SKIPPED);
            assertThat(a.getTemplateId()).isEqualTo(templateId);
            assertThat(a.getTemplateVersion()).isEqualTo("v1");
        }

        @Test
        @DisplayName("markSkipped rejects templateId XOR templateVersion")
        void skippedRejectsMismatchedTemplateArgs() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            assertThatThrownBy(() -> a.markSkipped(templateId, null, "r"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> a.markSkipped(null, "v1", "r"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("markSucceeded(templateId, version) rejects null templateId")
        void succeededRejectsNullTemplateId() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            assertThatThrownBy(() -> a.markSucceeded(null, "v1"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("markSucceeded(templateId, version) rejects blank version")
        void succeededRejectsBlankVersion() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            assertThatThrownBy(() -> a.markSucceeded(templateId, ""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> a.markSucceeded(templateId, "   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("markSucceeded(templateId, version) rejects version longer than 50 chars")
        void succeededRejectsOversizedVersion() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            String tooLong = "x".repeat(51);
            assertThatThrownBy(() -> a.markSucceeded(templateId, tooLong))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("non-PENDING transitions are rejected for all three overloads")
        void nonPendingTransitionsRejected() {
            SafetyAction a = SafetyAction.pending(
                    UUID.randomUUID(), UUID.randomUUID(),
                    SafetyActionType.SHOW_TEMPLATE);
            a.markSucceeded(templateId, "v1");
            // SUCCEEDED -> SUCCEEDED is rejected for all overloads.
            assertThatThrownBy(() -> a.markSucceeded(templateId, "v2"))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> a.markFailed(templateId, "v2", "x"))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> a.markSkipped(templateId, "v2", "x"))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> a.markSucceeded())
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> a.markFailed("x"))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> a.markSkipped("x"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}