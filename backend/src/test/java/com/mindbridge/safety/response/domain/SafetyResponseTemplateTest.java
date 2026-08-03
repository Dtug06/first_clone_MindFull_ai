package com.mindbridge.safety.response.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.safety.response.SafetyResponseTemplateStatus;
import com.mindbridge.safety.response.exception.SafetyResponseTemplateInputException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link SafetyResponseTemplate} entity.
 *
 * <p>Focuses on:
 * <ul>
 *   <li>Factory validation (null / blank inputs, locale whitelist,
 *       risk-reason UPPER_SNAKE_CASE, isDefault / DEFAULT entanglement).</li>
 *   <li>State-machine transitions (DRAFT - PENDING_REVIEW - APPROVED - RETIRED).</li>
 *   <li>Content edit gate (only-while-DRAFT).</li>
 *   <li>Append-only fields (no public setter for status / approvedBy).</li>
 * </ul>
 *
 * <p>JPA persistence behaviour is exercised separately by the
 * integration test for {@code SafetyResponseTemplateService}.
 */
@DisplayName("SafetyResponseTemplate")
class SafetyResponseTemplateTest {

    private static final UUID APPROVER_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static SafetyResponseTemplate newDraft(String code, String reason) {
        return SafetyResponseTemplate.create(
                UUID.randomUUID(),
                code,
                "v1",
                "vi",
                reason,
                "placeholder content",
                "DEFAULT".equals(reason));
    }

    @Nested
    @DisplayName("Factory validation")
    class FactoryValidation {

        @Test
        @DisplayName("create() rejects null id")
        void nullId() {
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    null, "C", "v1", "vi", "REASON_A", "x", false))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("create() rejects blank code")
        void blankCode() {
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    UUID.randomUUID(), "  ", "v1", "vi", "REASON_A", "x", false))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("code");
        }

        @Test
        @DisplayName("create() rejects blank templateVersion")
        void blankVersion() {
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    UUID.randomUUID(), "C", " ", "vi", "REASON_A", "x", false))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("templateVersion");
        }

        @Test
        @DisplayName("create() rejects blank locale")
        void blankLocale() {
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    UUID.randomUUID(), "C", "v1", "  ", "REASON_A", "x", false))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("locale");
        }

        @Test
        @DisplayName("create() rejects locale outside MVP whitelist")
        void invalidLocale() {
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    UUID.randomUUID(), "C", "v1", "en", "REASON_A", "x", false))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("locale");
        }

        @Test
        @DisplayName("create() rejects blank content")
        void blankContent() {
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    UUID.randomUUID(), "C", "v1", "vi", "REASON_A", "   ", false))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("content");
        }

        @Test
        @DisplayName("create() rejects risk_reason not in UPPER_SNAKE_CASE")
        void invalidRiskReason() {
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    UUID.randomUUID(), "C", "v1", "vi", "lower_case", "x", false))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("riskReason");
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    UUID.randomUUID(), "C", "v1", "vi", "1DIGIT", "x", false))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("riskReason");
        }

        @Test
        @DisplayName("create() rejects isDefault=true with non-DEFAULT reason")
        void defaultFlagWithoutDefaultReason() {
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    UUID.randomUUID(), "C", "v1", "vi", "REASON_X", "x", true))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("DEFAULT");
        }

        @Test
        @DisplayName("create() rejects isDefault=false with DEFAULT reason")
        void defaultReasonWithoutDefaultFlag() {
            assertThatThrownBy(() -> SafetyResponseTemplate.create(
                    UUID.randomUUID(), "C", "v1", "vi", "DEFAULT", "x", false))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("DEFAULT");
        }

        @Test
        @DisplayName("create() accepts DEFAULT reason + isDefault=true")
        void defaultHappyPath() {
            SafetyResponseTemplate t = SafetyResponseTemplate.create(
                    UUID.randomUUID(), "C", "v1", "vi", "DEFAULT", "x", true);
            assertThat(t.isDefault()).isTrue();
            assertThat(t.getRiskReason()).isEqualTo("DEFAULT");
        }
    }

    @Nested
    @DisplayName("Content edit gate")
    class ContentEditGate {

        @Test
        @DisplayName("updateContent allowed in DRAFT")
        void updateInDraft() {
            SafetyResponseTemplate t = newDraft("C1", "REASON_A");
            t.updateContent("new content");
            assertThat(t.getContent()).isEqualTo("new content");
        }

        @Test
        @DisplayName("updateContent rejected after submitForReview")
        void updateAfterSubmit() {
            SafetyResponseTemplate t = newDraft("C2", "REASON_A");
            t.submitForReview();
            assertThatThrownBy(() -> t.updateContent("new content"))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("updateContent");
        }

        @Test
        @DisplayName("updateContent rejects blank content")
        void updateBlank() {
            SafetyResponseTemplate t = newDraft("C3", "REASON_A");
            assertThatThrownBy(() -> t.updateContent("   "))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("newContent");
        }
    }

    @Nested
    @DisplayName("State transitions")
    class Transitions {

        @Test
        @DisplayName("DRAFT -> PENDING_REVIEW -> APPROVED happy path")
        void happyPath() {
            SafetyResponseTemplate t = newDraft("C4", "REASON_A");
            assertThat(t.getStatus()).isEqualTo(SafetyResponseTemplateStatus.DRAFT);
            assertThat(t.getApprovedBy()).isNull();
            assertThat(t.getApprovedAt()).isNull();

            t.submitForReview();
            assertThat(t.getStatus()).isEqualTo(
                    SafetyResponseTemplateStatus.PENDING_REVIEW);

            t.approve(APPROVER_ID);
            assertThat(t.getStatus()).isEqualTo(SafetyResponseTemplateStatus.APPROVED);
            assertThat(t.getApprovedBy()).isEqualTo(APPROVER_ID);
            assertThat(t.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("submitForReview fails when not DRAFT")
        void submitOnlyFromDraft() {
            SafetyResponseTemplate t = newDraft("C5", "REASON_A");
            t.submitForReview();
            assertThatThrownBy(t::submitForReview)
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("submitForReview");
        }

        @Test
        @DisplayName("approve fails when not PENDING_REVIEW")
        void approveOnlyFromPending() {
            SafetyResponseTemplate t = newDraft("C6", "REASON_A");
            assertThatThrownBy(() -> t.approve(APPROVER_ID))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("approve");
        }

        @Test
        @DisplayName("approve fails on null approverId")
        void approveRejectsNullApprover() {
            SafetyResponseTemplate t = newDraft("C7", "REASON_A");
            t.submitForReview();
            assertThatThrownBy(() -> t.approve(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("retire fails when not APPROVED")
        void retireOnlyFromApproved() {
            SafetyResponseTemplate t = newDraft("C8", "REASON_A");
            assertThatThrownBy(t::retire)
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("retire");
        }

        @Test
        @DisplayName("APPROVED -> RETIRED is allowed")
        void approveThenRetire() {
            SafetyResponseTemplate t = newDraft("C9", "REASON_A");
            t.submitForReview();
            t.approve(APPROVER_ID);
            t.retire();
            assertThat(t.getStatus()).isEqualTo(
                    SafetyResponseTemplateStatus.RETIRED);
        }
    }

    @Nested
    @DisplayName("Encapsulation")
    class Encapsulation {

        @Test
        @DisplayName("no public setter for status")
        void noStatusSetter() throws Exception {
            try {
                SafetyResponseTemplate.class.getMethod(
                        "setStatus", SafetyResponseTemplateStatus.class);
                org.junit.jupiter.api.Assertions.fail(
                        "SafetyResponseTemplate must not expose a public setStatus method");
            } catch (NoSuchMethodException expected) {
                // Expected
            }
        }

        @Test
        @DisplayName("no public setter for approvedBy")
        void noApprovedBySetter() throws Exception {
            try {
                SafetyResponseTemplate.class.getMethod(
                        "setApprovedBy", UUID.class);
                org.junit.jupiter.api.Assertions.fail(
                        "SafetyResponseTemplate must not expose a public setApprovedBy method");
            } catch (NoSuchMethodException expected) {
                // Expected
            }
        }
    }
}
