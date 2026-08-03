package com.mindbridge.safety.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.mindbridge.analysis.provider.EvidenceSpan;
import com.mindbridge.safety.domain.MatchType;
import com.mindbridge.safety.domain.SafetyKeywordRule;
import com.mindbridge.safety.domain.SafetyRuleStatus;
import com.mindbridge.safety.dto.MatchedRule;
import com.mindbridge.safety.dto.PreFilterInput;
import com.mindbridge.safety.dto.PreFilterResult;
import com.mindbridge.safety.exception.SafetyPreFilterInputException;
import com.mindbridge.safety.repository.SafetyKeywordRuleRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SafetyPreFilterService}. Pure JUnit 5 +
 * AssertJ + Mockito — does NOT boot a Spring context.
 *
 * <p>Goals:
 * <ul>
 *   <li>Validate that pre-filter never produces a final-risk-style
 *       output — only {@code preliminaryRisk} and matched rule
 *       signals.</li>
 *   <li>Verify status filtering: only {@code APPROVED} rules are
 *       evaluated.</li>
 *   <li>Verify both KEYWORD and REGEX match paths return evidence
 *       offsets, never raw text.</li>
 *   <li>Verify input validation throws the typed exception.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SafetyPreFilterService")
class SafetyPreFilterServiceTest {

    @Mock
    SafetyKeywordRuleRepository ruleRepository;

    TextNormalizer normalizer;

    SafetyPreFilterService service;

    @BeforeEach
    void setUp() {
        normalizer = new TextNormalizer();
        service = new SafetyPreFilterService(ruleRepository, normalizer);
    }

    private SafetyKeywordRule approvedRule(
            String code, String version, MatchType type, String pattern, short risk) {
        SafetyKeywordRule r = SafetyKeywordRule.create(
                UUID.randomUUID(), code, version, pattern, type, risk);
        r.submitForReview();
        r.approve(UUID.randomUUID());
        return r;
    }

    private PreFilterInput input(String content) {
        return new PreFilterInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                "vi-VN");
    }

    // --- Input validation ---

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("null input throws SafetyPreFilterInputException")
        void nullInput() {
            assertThatThrownBy(() -> service.evaluate(null))
                    .isInstanceOf(SafetyPreFilterInputException.class);
        }

        @Test
        @DisplayName("blank content is rejected by PreFilterInput record")
        void blankContent() {
            // PreFilterInput compact constructor throws before service is called.
            assertThatThrownBy(() -> input("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // --- No rule loaded ---

    @Nested
    @DisplayName("No rule loaded")
    class NoRuleLoaded {

        @Test
        @DisplayName("Empty repository → empty result, preliminaryRisk=1")
        void empty() {
            when(ruleRepository.findByStatus(SafetyRuleStatus.APPROVED))
                    .thenReturn(List.of());

            PreFilterResult r = service.evaluate(input("anything"));

            assertThat(r.matchedRules()).isEmpty();
            assertThat(r.preliminaryRisk()).isEqualTo(1);
            assertThat(r.ruleVersion()).isEqualTo("NONE");
            assertThat(r.confidence()).isEqualTo(0.0);
            assertThat(r.providerInfo())
                    .isEqualTo(PreFilterResult.PROVIDER_RULE_ENGINE_V1);
        }
    }

    // --- Single rule match ---

    @Nested
    @DisplayName("Single rule match")
    class SingleRuleMatch {

        @Test
        @DisplayName("KEYWORD rule matches → matched with offsets, no raw text")
        void keyword() {
            SafetyKeywordRule rule = approvedRule(
                    "SAFETY_SELF_HARM",
                    "v1",
                    MatchType.KEYWORD,
                    "không muốn sống",
                    (short) 4);
            when(ruleRepository.findByStatus(SafetyRuleStatus.APPROVED))
                    .thenReturn(List.of(rule));

            PreFilterResult r = service.evaluate(
                    input("hôm nay tôi không muốn sống nữa"));

            assertThat(r.matchedRules()).hasSize(1);
            MatchedRule m = r.matchedRules().get(0);
            assertThat(m.ruleCode()).isEqualTo("SAFETY_SELF_HARM");
            assertThat(m.ruleVersion()).isEqualTo("v1");
            assertThat(m.matchType()).isEqualTo(MatchType.KEYWORD);
            assertThat(m.preliminaryRisk()).isEqualTo(4);
            assertThat(m.evidenceSpans()).hasSize(1);
            EvidenceSpan span = m.evidenceSpans().get(0);
            assertThat(span.start()).isLessThan(span.end());
            assertThat(span.textHash()).hasSize(64); // SHA-256 hex length
            assertThat(r.preliminaryRisk()).isEqualTo(4);
        }

        @Test
        @DisplayName("REGEX rule matches → matched with offsets")
        void regex() {
            // NOTE: Java's \b word boundary is ASCII-only and does NOT
            // fire around Vietnamese letters like 'ự' or 'ữ'. Use
            // Unicode-aware lookarounds instead, so the rule can
            // match "tự tử" even when surrounded by other letters.
            SafetyKeywordRule rule = approvedRule(
                    "SAFETY_FOLLOWUP",
                    "v1",
                    MatchType.REGEX,
                    "(?<![\\p{L}])(tự tử|tự sát)(?![\\p{L}])",
                    (short) 4);
            when(ruleRepository.findByStatus(SafetyRuleStatus.APPROVED))
                    .thenReturn(List.of(rule));

            PreFilterResult r = service.evaluate(
                    input("tôi đang nghĩ đến tự tử"));

            assertThat(r.matchedRules()).hasSize(1);
            assertThat(r.matchedRules().get(0).matchType()).isEqualTo(MatchType.REGEX);
            assertThat(r.preliminaryRisk()).isEqualTo(4);
        }
    }

    // --- Multiple rules and severity aggregation ---

    @Nested
    @DisplayName("Multiple rules")
    class MultipleRules {

        @Test
        @DisplayName("Multiple matches → preliminaryRisk = max")
        void maxAggregation() {
            SafetyKeywordRule lo = approvedRule(
                    "FOLLOWUP", "v1", MatchType.KEYWORD, "lo lắng", (short) 2);
            SafetyKeywordRule hi = approvedRule(
                    "BURNOUT", "v1", MatchType.KEYWORD, "kiệt sức", (short) 3);
            when(ruleRepository.findByStatus(SafetyRuleStatus.APPROVED))
                    .thenReturn(List.of(lo, hi));

            PreFilterResult r = service.evaluate(
                    input("tôi lo lắng và kiệt sức quá"));

            assertThat(r.matchedRules()).hasSize(2);
            // Sorted by preliminaryRisk DESC
            assertThat(r.matchedRules().get(0).ruleCode()).isEqualTo("BURNOUT");
            assertThat(r.matchedRules().get(1).ruleCode()).isEqualTo("FOLLOWUP");
            assertThat(r.preliminaryRisk()).isEqualTo(3);
        }

        @Test
        @DisplayName("No match returns empty list with risk=1")
        void noMatch() {
            SafetyKeywordRule rule = approvedRule(
                    "BURNOUT", "v1", MatchType.KEYWORD, "kiệt sức", (short) 3);
            when(ruleRepository.findByStatus(SafetyRuleStatus.APPROVED))
                    .thenReturn(List.of(rule));

            PreFilterResult r = service.evaluate(input("hôm nay trời đẹp"));
            assertThat(r.matchedRules()).isEmpty();
            assertThat(r.preliminaryRisk()).isEqualTo(1);
        }
    }

    // --- Status filtering ---

    @Nested
    @DisplayName("Status filtering")
    class StatusFilter {

        @Test
        @DisplayName("Only APPROVED rules are evaluated")
        void onlyApproved() {
            // Three rules with the same pattern; only the APPROVED one
            // should match. The DRAFT and RETIRED ones are skipped.
            SafetyKeywordRule draft = SafetyKeywordRule.create(
                    UUID.randomUUID(), "D", "v1", "kiệt sức",
                    MatchType.KEYWORD, (short) 3);
            SafetyKeywordRule retired = approvedRule(
                    "R", "v1", MatchType.KEYWORD, "kiệt sức", (short) 3);
            retired.retire();
            SafetyKeywordRule approved = approvedRule(
                    "A", "v1", MatchType.KEYWORD, "kiệt sức", (short) 3);

            when(ruleRepository.findByStatus(SafetyRuleStatus.APPROVED))
                    .thenReturn(List.of(approved));

            PreFilterResult r = service.evaluate(input("tôi kiệt sức"));
            assertThat(r.matchedRules()).hasSize(1);
            assertThat(r.matchedRules().get(0).ruleCode()).isEqualTo("A");
        }
    }

    // --- False-positive documentation ---

    @Nested
    @DisplayName("Documented limitations")
    class Limitations {

        @Test
        @DisplayName("Documented false-positive: substring match inside unrelated word")
        void documentedFalsePositive() {
            // This is a known limitation of plain substring matching.
            // The rule is APPROVED; the test documents that the match
            // happens even though the user's intent is benign.
            SafetyKeywordRule rule = approvedRule(
                    "BURNOUT", "v1", MatchType.KEYWORD, "kiệt sức", (short) 3);
            when(ruleRepository.findByStatus(SafetyRuleStatus.APPROVED))
                    .thenReturn(List.of(rule));

            PreFilterResult r = service.evaluate(
                    input("hôm nay tôi không kiệt sức gì cả"));
            // The substring "kiệt sức" appears → keyword matches.
            // This is intentional: documenting the limitation so the
            // future rule-tuning task (G3-T08 follow-up) can replace
            // this with a more precise regex pattern.
            assertThat(r.matchedRules()).hasSize(1);
        }
    }

    // --- Rule-set version snapshot ---

    @Nested
    @DisplayName("Rule-set version snapshot")
    class RuleSetSnapshot {

        @Test
        @DisplayName("ruleVersion field snapshots the rule set used")
        void snapshot() {
            SafetyKeywordRule a = approvedRule(
                    "AAA", "v1", MatchType.KEYWORD, "x", (short) 1);
            SafetyKeywordRule b = approvedRule(
                    "ZZZ", "v2", MatchType.KEYWORD, "y", (short) 1);
            when(ruleRepository.findByStatus(SafetyRuleStatus.APPROVED))
                    .thenReturn(List.of(a, b));

            PreFilterResult r = service.evaluate(input("hello"));
            // Sorted by code → "AAA@v1,ZZZ@v2"
            assertThat(r.ruleVersion()).isEqualTo("AAA@v1,ZZZ@v2");
        }
    }
}
