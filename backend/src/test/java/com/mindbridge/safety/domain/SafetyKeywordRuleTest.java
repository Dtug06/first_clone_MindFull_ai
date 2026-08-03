package com.mindbridge.safety.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link SafetyKeywordRule} entity — focuses on
 * factory validation and the controlled state-machine transitions.
 *
 * <p>JPA persistence behaviour is exercised separately by the
 * integration test.
 */
@DisplayName("SafetyKeywordRule")
class SafetyKeywordRuleTest {

    private static final UUID APPROVER_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static SafetyKeywordRule newDraft(String code) {
        return SafetyKeywordRule.create(
                UUID.randomUUID(),
                code,
                "v1",
                "không muốn sống",
                MatchType.KEYWORD,
                (short) 4);
    }

    @Nested
    @DisplayName("Factory validation")
    class FactoryValidation {

        @Test
        @DisplayName("create() rejects null id")
        void nullId() {
            assertThatThrownBy(() -> SafetyKeywordRule.create(
                    null, "R", "v1", "p", MatchType.KEYWORD, (short) 1))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("create() rejects blank code")
        void blankCode() {
            assertThatThrownBy(() -> SafetyKeywordRule.create(
                    UUID.randomUUID(), "  ", "v1", "p", MatchType.KEYWORD, (short) 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code");
        }

        @Test
        @DisplayName("create() rejects blank pattern")
        void blankPattern() {
            assertThatThrownBy(() -> SafetyKeywordRule.create(
                    UUID.randomUUID(), "R", "v1", "", MatchType.KEYWORD, (short) 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pattern");
        }

        @Test
        @DisplayName("create() rejects preliminary_risk outside [1, 4]")
        void outOfRangeRisk() {
            assertThatThrownBy(() -> SafetyKeywordRule.create(
                    UUID.randomUUID(), "R", "v1", "p", MatchType.KEYWORD, (short) 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("preliminaryRisk");
            assertThatThrownBy(() -> SafetyKeywordRule.create(
                    UUID.randomUUID(), "R", "v1", "p", MatchType.KEYWORD, (short) 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("preliminaryRisk");
        }
    }

    @Nested
    @DisplayName("State transitions")
    class Transitions {

        @Test
        @DisplayName("DRAFT → PENDING_REVIEW → APPROVED happy path")
        void happyPath() {
            SafetyKeywordRule r = newDraft("R1");
            assertThat(r.getStatus()).isEqualTo(SafetyRuleStatus.DRAFT);
            assertThat(r.getApprovedBy()).isNull();
            assertThat(r.getApprovedAt()).isNull();

            r.submitForReview();
            assertThat(r.getStatus()).isEqualTo(SafetyRuleStatus.PENDING_REVIEW);

            r.approve(APPROVER_ID);
            assertThat(r.getStatus()).isEqualTo(SafetyRuleStatus.APPROVED);
            assertThat(r.getApprovedBy()).isEqualTo(APPROVER_ID);
            assertThat(r.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("submitForReview fails when not DRAFT")
        void submitForReviewOnlyFromDraft() {
            SafetyKeywordRule r = newDraft("R2");
            r.submitForReview();
            assertThatThrownBy(r::submitForReview)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING_REVIEW");
        }

        @Test
        @DisplayName("approve fails when not PENDING_REVIEW")
        void approveOnlyFromPendingReview() {
            SafetyKeywordRule r = newDraft("R3");
            // skip PENDING_REVIEW
            assertThatThrownBy(() -> r.approve(APPROVER_ID))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("retire fails when not APPROVED")
        void retireOnlyFromApproved() {
            SafetyKeywordRule r = newDraft("R4");
            // Not yet approved
            assertThatThrownBy(r::retire)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("APPROVED → RETIRED is allowed")
        void approveThenRetire() {
            SafetyKeywordRule r = newDraft("R5");
            r.submitForReview();
            r.approve(APPROVER_ID);
            r.retire();
            assertThat(r.getStatus()).isEqualTo(SafetyRuleStatus.RETIRED);
        }
    }
}
