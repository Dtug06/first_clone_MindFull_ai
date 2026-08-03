package com.mindbridge.safety.resolver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindbridge.analysis.provider.EvidenceSpan;
import com.mindbridge.safety.classifier.RiskClassifierOutput;
import com.mindbridge.safety.domain.MatchType;
import com.mindbridge.safety.dto.MatchedRule;
import com.mindbridge.safety.dto.PreFilterResult;
import com.mindbridge.safety.resolver.RiskStateHistory;
import com.mindbridge.safety.resolver.RiskStateHistoryRepository;
import com.mindbridge.safety.resolver.RiskStateSourceType;
import com.mindbridge.safety.resolver.dto.ResolverDecision;
import com.mindbridge.safety.resolver.dto.ResolverInput;
import com.mindbridge.safety.resolver.exception.SafetyResolverInputException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SafetyResolverService}. Pure JUnit 5 + AssertJ
 * + Mockito — does NOT boot a Spring context. The persistence side
 * uses {@link RiskStateHistoryRepository#save(Object)} via Mockito so
 * the resolver logic can be exercised in isolation.
 *
 * <p>Companion integration tests in
 * {@code SafetyResolverIntegrationTest} verify the schema-level
 * invariants (append-only via the entity, indices, latest-by-time via
 * the repository).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SafetyResolverService")
class SafetyResolverServiceTest {

    private static final Instant FROZEN_INSTANT = Instant.parse("2026-08-01T10:00:00Z");
    private static final Clock FROZEN_CLOCK = Clock.fixed(FROZEN_INSTANT, ZoneOffset.UTC);

    @Mock
    private RiskStateHistoryRepository historyRepository;

    private SafetyResolverService service;

    @BeforeEach
    void setUp() {
        service = new SafetyResolverService(historyRepository, FROZEN_CLOCK);
    }

    private static UUID userId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    /** Pre-filter with no matched rules (used by max-wins / no-signal
     *  tests — they only care about {@code preliminaryRisk} and the
     *  snapshot, not about matched-rule codes). */
    private static PreFilterResult preFilter(int risk, double confidence) {
        return new PreFilterResult(List.of(), risk, "SAFETY_TEST@v1", confidence,
                PreFilterResult.PROVIDER_RULE_ENGINE_V1);
    }

    /** Pre-filter with one synthetic matched rule so we can assert
     *  that the rule code surfaces in {@code reasonCodes}. */
    private static PreFilterResult preFilterWithRule(int risk, double confidence,
            String ruleCode, String ruleVersion) {
        MatchedRule mr = new MatchedRule(ruleCode, ruleVersion,
                MatchType.KEYWORD, risk, List.of());
        return new PreFilterResult(List.of(mr), risk, "SAFETY_TEST@v1", confidence,
                PreFilterResult.PROVIDER_RULE_ENGINE_V1);
    }

    private static RiskClassifierOutput classifier(int risk, double confidence) {
        // SHA-256 hex of "abc" — fixed 64-char placeholder for tests.
        String fixedHash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        return new RiskClassifierOutput(risk, confidence, List.of("REASON_DEMO"),
                List.of(new EvidenceSpan(0, 5, fixedHash)),
                RiskClassifierOutput.DEMO_PROMPT_VERSION,
                RiskClassifierOutput.CURRENT_SCHEMA_VERSION,
                RiskClassifierOutput.PROVIDER_MOCK_V1);
    }

    private static ResolverInput input(PreFilterResult pre, RiskClassifierOutput cls) {
        return new ResolverInput(userId(), RiskStateSourceType.KEYWORD_PRE_FILTER, null, pre, cls);
    }

    private static RiskStateHistory priorRow(UUID user, short level,
            RiskStateSourceType sourceType, String[] reasonCodes, OffsetDateTime at) {
        return RiskStateHistory.record(
                UUID.randomUUID(), user, level,
                null, null, null,
                sourceType, null,
                "SAFETY_TEST@v1", null, null,
                new BigDecimal("0.500"),
                reasonCodes,
                at);
    }

    @Nested
    @DisplayName("resolve() — max-wins rule")
    class MaxWinsRule {

        @Test
        @DisplayName("keyword L4 + classifier L2 → final L4 + MAX_WINS_L4")
        void keywordL4_classifierL2_returnsL4() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(
                    input(preFilter(4, 0.9), classifier(2, 0.7)));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 4);
            assertThat(decision.previousRiskLevel()).isNull();
            assertThat(decision.reasonCodes())
                    .containsExactly("REASON_DEMO", "NO_SIGNAL_DEMO", "MAX_WINS_L4");
        }

        @Test
        @DisplayName("keyword L2 + classifier L4 → final L4")
        void keywordL2_classifierL4_returnsL4() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(
                    input(preFilter(2, 0.7), classifier(4, 0.9)));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 4);
            assertThat(decision.reasonCodes())
                    .endsWith("MAX_WINS_L4");
        }

        @Test
        @DisplayName("keyword L2 + classifier L1 → final L2")
        void keywordL2_classifierL1_returnsL2() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(
                    input(preFilter(2, 0.7), classifier(1, 0.5)));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 2);
            assertThat(decision.reasonCodes())
                    .endsWith("MAX_WINS_L2");
        }
    }

    @Nested
    @DisplayName("resolve() — no-signal inputs")
    class NoSignalInputs {

        @Test
        @DisplayName("classifier null → reasonCodes contains NO_SIGNAL_DEMO")
        void classifierNull_ruleL1_returnsL1() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(
                    input(preFilter(1, 0.0), null));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 1);
            assertThat(decision.modelRiskLevel()).isNull();
            assertThat(decision.ruleRiskLevel()).isEqualTo(Short.valueOf((short) 1));
            assertThat(decision.reasonCodes())
                    .containsExactly("NO_SIGNAL_DEMO", "MAX_WINS_L1");
        }

        @Test
        @DisplayName("both signals null → final L1, only path-code remains")
        void bothNull_returnsL1() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(input(null, null));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 1);
            // No signal at all → only the path-code is present.
            assertThat(decision.reasonCodes()).containsExactly("MAX_WINS_L1");
        }

        @Test
        @DisplayName("keyword null → pre-filter snapshots become NONE / null")
        void keywordNull_snapshotsBecomeNoneAndNull() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(input(null, classifier(2, 0.6)));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 2);
            assertThat(decision.historyRow().getRuleVersion()).isEqualTo("NONE");
            assertThat(decision.historyRow().getModelVersion())
                    .isEqualTo(RiskClassifierOutput.PROVIDER_MOCK_V1);
            assertThat(decision.reasonCodes())
                    .containsExactly("REASON_DEMO", "MAX_WINS_L2");
        }

        @Test
        @DisplayName("pre-filter ran but matched nothing → NO_SIGNAL_DEMO appears")
        void preFilterRanButNoRuleMatched_addsNoSignalDemo() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(input(preFilter(1, 0.0), classifier(2, 0.7)));

            assertThat(decision.reasonCodes())
                    .containsExactly("REASON_DEMO", "NO_SIGNAL_DEMO", "MAX_WINS_L2");
        }

        @Test
        @DisplayName("pre-filter matched one rule → RULE_<code>@<version> appears")
        void preFilterMatchedRule_addsRuleCode() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(input(
                    preFilterWithRule(4, 0.9, "SAFETY_SELF_HARM", "v1"),
                    classifier(2, 0.6)));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 4);
            assertThat(decision.reasonCodes())
                    .containsExactly("REASON_DEMO", "RULE_SAFETY_SELF_HARM@v1", "MAX_WINS_L4");
        }
    }

    @Nested
    @DisplayName("resolve() — downgrade guard (Q3)")
    class DowngradeGuard {

        @Test
        @DisplayName("previous L4 + signals L1 → final L4 + MANUAL_REVIEW_REQUIRED")
        void previousL4_signalsL1_keepsL4() {
            UUID user = userId();
            RiskStateHistory previous = priorRow(user, (short) 4,
                    RiskStateSourceType.LLM_CLASSIFIER,
                    new String[]{"REASON_DEMO", "MAX_WINS_L4"},
                    OffsetDateTime.now(FROZEN_CLOCK).minusMinutes(5));
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(user))
                    .thenReturn(Optional.of(previous));
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(
                    input(preFilter(1, 0.5), classifier(1, 0.4)));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 4);
            assertThat(decision.previousRiskLevel()).isEqualTo(Short.valueOf((short) 4));
            assertThat(decision.reasonCodes())
                    .endsWith("MANUAL_REVIEW_REQUIRED");
        }

        @Test
        @DisplayName("previous L1 + signals L4 → final L4 (max still wins)")
        void previousL1_signalsL4_returnsL4() {
            UUID user = userId();
            RiskStateHistory previous = priorRow(user, (short) 1,
                    RiskStateSourceType.KEYWORD_PRE_FILTER,
                    new String[]{"NO_SIGNAL_DEMO", "MAX_WINS_L1"},
                    OffsetDateTime.now(FROZEN_CLOCK).minusMinutes(5));
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(user))
                    .thenReturn(Optional.of(previous));
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(
                    input(preFilter(4, 0.95), classifier(4, 0.95)));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 4);
            assertThat(decision.reasonCodes()).endsWith("MAX_WINS_L4");
        }

        @Test
        @DisplayName("previous L4 + signals L4 → final L4 (no change path)")
        void previousL4_signalsL4_returnsL4() {
            UUID user = userId();
            RiskStateHistory previous = priorRow(user, (short) 4,
                    RiskStateSourceType.MANUAL_REVIEW,
                    new String[]{"EXPERT_DOWNGRADE_L4"},
                    OffsetDateTime.now(FROZEN_CLOCK).minusMinutes(5));
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(user))
                    .thenReturn(Optional.of(previous));
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(
                    input(preFilter(4, 0.95), classifier(3, 0.9)));

            assertThat(decision.finalRiskLevel()).isEqualTo((short) 4);
            // Even when previous == candidate, we go through the
            // "max wins" branch (candidate >= previous) — the reason
            // code is MAX_WINS_L4, not MANUAL_REVIEW_REQUIRED.
            assertThat(decision.reasonCodes()).endsWith("MAX_WINS_L4");
        }
    }

    @Nested
    @DisplayName("resolve() — deterministic and append-only")
    class DeterministicAndAppendOnly {

        @Test
        @DisplayName("same input + same Clock → same finalRiskLevel and reasonCodes (100 calls)")
        void sameInput_sameRuleVersion_sameDecision() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision first = service.resolve(
                    input(preFilter(3, 0.8), classifier(2, 0.6)));
            for (int i = 0; i < 100; i++) {
                ResolverDecision next = service.resolve(
                        input(preFilter(3, 0.8), classifier(2, 0.6)));
                assertThat(next.finalRiskLevel()).as("iteration %d", i).isEqualTo(first.finalRiskLevel());
                assertThat(next.reasonCodes()).as("iteration %d", i)
                        .containsExactlyElementsOf(List.of(first.reasonCodes()));
            }
        }

        @Test
        @DisplayName("two consecutive resolves for same user → two persisted rows (append-only)")
        void consecutiveResolves_persistTwoRows() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.resolve(input(preFilter(2, 0.7), classifier(1, 0.4)));
            service.resolve(input(preFilter(3, 0.8), classifier(3, 0.8)));

            ArgumentCaptor<RiskStateHistory> captor =
                    ArgumentCaptor.forClass(RiskStateHistory.class);
            verify(historyRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues()).hasSize(2);
            assertThat(captor.getAllValues().get(0).getId())
                    .isNotEqualTo(captor.getAllValues().get(1).getId());
        }

        @Test
        @DisplayName("Confidence picks the larger of the two signals")
        void confidence_picksStronger() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ResolverDecision decision = service.resolve(
                    input(preFilter(2, 0.7), classifier(3, 0.85)));

            assertThat(decision.confidence()).isEqualByComparingTo(new BigDecimal("0.850"));
        }
    }

    @Nested
    @DisplayName("resolve() — input validation")
    class InputValidation {

        @Test
        @DisplayName("null input → SafetyResolverInputException")
        void nullInput_throws() {
            assertThatThrownBy(() -> service.resolve(null))
                    .isInstanceOf(SafetyResolverInputException.class)
                    .hasMessageContaining("ResolverInput must not be null");
            verify(historyRepository, never()).save(any());
        }

        @Test
        @DisplayName("input with null userId → exception at construction time")
        void nullUserId_throws() {
            assertThatThrownBy(() -> new ResolverInput(
                    null, RiskStateSourceType.KEYWORD_PRE_FILTER, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("input with null sourceType → exception at construction time")
        void nullSourceType_throws() {
            assertThatThrownBy(() -> new ResolverInput(
                    userId(), null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceType");
        }

        @Test
        @DisplayName("Phase 3 — exception propagation is caller-responsibility")
        void resolve_propagatesClassifierException() {
            // Per docs/04 §28 "JSON sai schema không được lưu thành
            // công", the resolver must NOT silently convert a broken
            // classifier into a "no signal" row. Callers (T11) wrap
            // the call in try/catch. Here we verify the resolver
            // propagates the exception untouched and never persists.
            RuntimeException classifierBroken = new RuntimeException("classifier timeout");
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenThrow(classifierBroken);
            assertThatThrownBy(() -> service.resolve(input(preFilter(2, 0.7), classifier(2, 0.6))))
                    .isSameAs(classifierBroken);
            verify(historyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCurrentRiskState()")
    class GetCurrentRiskState {

        @Test
        @DisplayName("null userId → SafetyResolverInputException")
        void nullUserId_throws() {
            assertThatThrownBy(() -> service.getCurrentRiskState(null))
                    .isInstanceOf(SafetyResolverInputException.class);
        }

        @Test
        @DisplayName("delegates to repository.findFirstByUserIdOrderByOccurredAtDescIdDesc")
        void delegatesToRepository() {
            UUID user = userId();
            RiskStateHistory row = RiskStateHistory.record(
                    UUID.randomUUID(), user, (short) 2,
                    Short.valueOf((short) 2), Short.valueOf((short) 1), null,
                    RiskStateSourceType.KEYWORD_PRE_FILTER, null,
                    "SAFETY_TEST@v1", null, null,
                    new BigDecimal("0.500"),
                    new String[]{"REASON_DEMO", "NO_SIGNAL_DEMO", "MAX_WINS_L2"},
                    OffsetDateTime.now(FROZEN_CLOCK));
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(user))
                    .thenReturn(Optional.of(row));

            Optional<RiskStateHistory> result = service.getCurrentRiskState(user);

            assertThat(result).isPresent();
            assertThat(result.get().getRiskLevel()).isEqualTo((short) 2);
            assertThat(result.get().getReasonCodes())
                    .containsExactly("REASON_DEMO", "NO_SIGNAL_DEMO", "MAX_WINS_L2");
        }
    }

    @Nested
    @DisplayName("Safety invariants (Phase 3 reflection-scan)")
    class SafetyInvariants {

        @Test
        @DisplayName("ResolveDecision never produces a finalRiskLevel less than previousRiskLevel")
        void resolve_neverDecreasesRiskBelowCurrent() {
            UUID user = userId();
            RiskStateHistory previous = priorRow(user, (short) 4,
                    RiskStateSourceType.MANUAL_REVIEW,
                    new String[]{"EXPERT_REVIEW_DEMO", "MAX_WINS_L4"},
                    OffsetDateTime.now(FROZEN_CLOCK).minusMinutes(10));
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(user))
                    .thenReturn(Optional.of(previous));
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Try a wide spread of weak signals.
            for (int rule = 1; rule <= 4; rule++) {
                for (int model = 1; model <= 4; model++) {
                    ResolverDecision d = service.resolve(
                            input(preFilter(rule, 0.5), classifier(model, 0.5)));
                    assertThat(d.finalRiskLevel())
                            .as("rule=%d model=%d previous=4", rule, model)
                            .isGreaterThanOrEqualTo((short) 4);
                }
            }
        }

        @Test
        @DisplayName("Every persisted row carries a non-empty reasonCodes array ending in a path-code")
        void everyRow_carriesStructuredReasonCodes() {
            when(historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId()))
                    .thenReturn(Optional.empty());
            when(historyRepository.save(any(RiskStateHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.resolve(input(preFilter(3, 0.8), classifier(2, 0.6)));
            service.resolve(input(null, null));
            service.resolve(input(preFilterWithRule(4, 0.9, "SAFETY_SELF_HARM", "v1"),
                    classifier(1, 0.4)));

            ArgumentCaptor<RiskStateHistory> captor =
                    ArgumentCaptor.forClass(RiskStateHistory.class);
            verify(historyRepository, times(3)).save(captor.capture());

            for (RiskStateHistory row : captor.getAllValues()) {
                String[] codes = row.getReasonCodes();
                assertThat(codes).isNotEmpty();
                assertThat(codes[codes.length - 1])
                        .matches(c -> c.equals("MAX_WINS_L1")
                                || c.equals("MAX_WINS_L2")
                                || c.equals("MAX_WINS_L3")
                                || c.equals("MAX_WINS_L4")
                                || c.equals("MANUAL_REVIEW_REQUIRED"));
                for (String code : codes) {
                    assertThat(code).isNotBlank();
                }
            }
        }
    }
}
