package com.mindbridge.safety.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.analysis.provider.EvidenceSpan;
import com.mindbridge.safety.classifier.RiskClassifierOutput;
import com.mindbridge.safety.dto.PreFilterResult;
import com.mindbridge.safety.resolver.dto.ResolverDecision;
import com.mindbridge.safety.resolver.dto.ResolverInput;
import com.mindbridge.safety.resolver.service.SafetyResolverService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration test that boots the full Spring context with the
 * {@code com.mindbridge.safety.resolver} package on the classpath,
 * then exercises {@link SafetyResolverService} end-to-end against the
 * H2 schema.
 *
 * <p>This test verifies:
 * <ul>
 *   <li>Spring context boots cleanly with the resolver package
 *       wired (and the existing {@code TimeConfig.systemClock} bean
 *       injected).</li>
 *   <li>Append-only invariant: every {@code resolve} call inserts a
 *       new row; old rows remain untouched.</li>
 *   <li>Latest-by-time read: {@code getCurrentRiskState} returns the
 *       row with the greatest {@code occurred_at} (tie-break by id
 *       DESC, mirroring G2 acceptance decision #2 fix).</li>
 *   <li>Static safety invariants: no public method in the resolver
 *       package downgrades risk; the entity has no
 *       {@code @PreUpdate}/{@code @PreRemove} hooks (append-only at
 *       the JPA layer too).</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mindbridge.ai.provider=mock",
        "mindbridge.ai.mock.force-scenario="
})
@Sql(scripts = "/schema-risk-state-history.sql")
@DisplayName("SafetyResolverService integration")
class SafetyResolverIntegrationTest {

    @Autowired
    private SafetyResolverService resolver;

    @Autowired
    private RiskStateHistoryRepository historyRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private static UUID newUser() {
        return UUID.randomUUID();
    }

    private static PreFilterResult preFilter(int risk, double confidence) {
        return new PreFilterResult(List.of(), risk, "SAFETY_TEST@v1", confidence,
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

    @Test
    @DisplayName("Service bean is wired and Spring context boots")
    void contextWiring() {
        assertThat(resolver).isNotNull();
        assertThat(historyRepository).isNotNull();
    }

    @Test
    @DisplayName("First resolve inserts exactly one row; current state returns it")
    void firstResolve_insertsOneRow() {
        UUID user = newUser();
        ResolverDecision d = resolver.resolve(new ResolverInput(
                user, RiskStateSourceType.KEYWORD_PRE_FILTER, null,
                preFilter(3, 0.8), classifier(1, 0.4)));

        assertThat(d.finalRiskLevel()).isEqualTo((short) 3);
        assertThat(d.previousRiskLevel()).isNull();
        assertThat(d.reasonCodes()).isNotEmpty();
        assertThat(d.reasonCodes()[d.reasonCodes().length - 1]).isEqualTo("MAX_WINS_L3");
        assertThat(historyRepository.countByUserId(user)).isEqualTo(1L);

        Optional<RiskStateHistory> current = resolver.getCurrentRiskState(user);
        assertThat(current).isPresent();
        assertThat(current.get().getRiskLevel()).isEqualTo((short) 3);
        assertThat(current.get().getReasonCodes())
                .containsExactly(d.reasonCodes());
    }

    @Test
    @DisplayName("Two resolves insert two rows; old row preserved (append-only)")
    void secondResolve_appendsRow_oldPreserved() {
        UUID user = newUser();
        resolver.resolve(new ResolverInput(
                user, RiskStateSourceType.KEYWORD_PRE_FILTER, null,
                preFilter(2, 0.7), classifier(1, 0.4)));
        ResolverDecision second = resolver.resolve(new ResolverInput(
                user, RiskStateSourceType.LLM_CLASSIFIER, null,
                preFilter(3, 0.8), classifier(3, 0.85)));

        assertThat(historyRepository.countByUserId(user)).isEqualTo(2L);
        assertThat(second.finalRiskLevel()).isEqualTo((short) 3);
        assertThat(second.previousRiskLevel()).isEqualTo(Short.valueOf((short) 2));

        // The "previous" row is still there with its original level —
        // never mutated by the second resolve.
        List<RiskStateHistory> rows =
                historyRepository.findByUserIdOrderByOccurredAtDescIdDesc(user);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getRiskLevel()).isEqualTo((short) 3);
        assertThat(rows.get(1).getRiskLevel()).isEqualTo((short) 2);
    }

    @Test
    @DisplayName("getCurrentRiskState returns the row with the greatest occurred_at")
    void getCurrentRiskState_returnsLatestByTime() {
        UUID user = newUser();
        resolver.resolve(new ResolverInput(
                user, RiskStateSourceType.KEYWORD_PRE_FILTER, null,
                preFilter(2, 0.7), classifier(1, 0.4)));
        // Sleep is not used here — relies on OffsetDateTime.now(Clock)
        // which advances at least at millisecond granularity; H2
        // TIMESTAMP preserves microseconds so the two rows are
        // guaranteed distinct.
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        resolver.resolve(new ResolverInput(
                user, RiskStateSourceType.LLM_CLASSIFIER, null,
                preFilter(4, 0.95), classifier(4, 0.95)));

        RiskStateHistory current = resolver.getCurrentRiskState(user).orElseThrow();
        assertThat(current.getRiskLevel()).isEqualTo((short) 4);
    }

    @Test
    @DisplayName("Empty history → getCurrentRiskState returns Optional.empty")
    void getCurrentRiskState_emptyUser_returnsEmpty() {
        Optional<RiskStateHistory> result =
                resolver.getCurrentRiskState(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Reason codes follow the structured JSONB shape per DB-MVP §6.1")
    void reasonCodes_areStructured() {
        UUID user = newUser();
        ResolverDecision d = resolver.resolve(new ResolverInput(
                user, RiskStateSourceType.KEYWORD_PRE_FILTER, null,
                preFilter(3, 0.8), classifier(1, 0.4)));
        assertThat(d.reasonCodes()).isNotEmpty();
        // Every row carries at least a path-code so audit can group by
        // path without re-parsing free text.
        assertThat(d.reasonCodes()[d.reasonCodes().length - 1])
                .isIn("MAX_WINS_L1", "MAX_WINS_L2", "MAX_WINS_L3", "MAX_WINS_L4",
                        "MANUAL_REVIEW_REQUIRED");
    }

    @Test
    @DisplayName("Static invariant: no resolver-package method or type suggests downgrade")
    void noDowngradeMethodInResolverPackage() {
        String[] beans = applicationContext.getBeanNamesForAnnotation(
                org.springframework.stereotype.Component.class);
        for (String beanName : beans) {
            Object bean = applicationContext.getBean(beanName);
            String pkg = bean.getClass().getPackageName();
            if (!pkg.startsWith("com.mindbridge.safety.resolver")) {
                continue;
            }
            for (Method m : bean.getClass().getMethods()) {
                String name = m.getName().toLowerCase();
                if (name.contains("downgrade")
                        || name.contains("decreaserisk")
                        || name.contains("lowerisk")
                        || name.contains("reducrisk")) {
                    throw new AssertionError(
                            "resolver bean " + beanName + " has method "
                                    + m.getName() + " — resolver must never downgrade risk");
                }
            }
        }
    }

    @Test
    @DisplayName("Static invariant: RiskStateHistory has no @PreUpdate or @PreRemove hooks")
    void riskStateHistory_isAppendOnly() throws Exception {
        Class<?> cls = RiskStateHistory.class;
        for (Method m : cls.getDeclaredMethods()) {
            if (m.isAnnotationPresent(jakarta.persistence.PreUpdate.class)
                    || m.isAnnotationPresent(jakarta.persistence.PreRemove.class)) {
                throw new AssertionError(
                        "RiskStateHistory must be append-only but has a "
                                + m.getName() + " lifecycle hook");
            }
        }
        // Also assert that no public setter exists for any of the
        // recorded fields. We treat the entity as immutable at the
        // application boundary.
        for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
            String setter = "set" + Character.toUpperCase(f.getName().charAt(0))
                    + f.getName().substring(1);
            try {
                Method setterMethod = cls.getMethod(setter, f.getType());
                if (Modifier.isPublic(setterMethod.getModifiers())) {
                    throw new AssertionError(
                            "RiskStateHistory must not expose public setter "
                                    + setterMethod.getName() + " (append-only contract)");
                }
            } catch (NoSuchMethodException ignored) {
                // No setter of this exact shape — that's the expected case.
            }
        }
    }

    @Test
    @DisplayName("Schema-level: row carries the configured columns verbatim")
    void rowCarriesAllConfiguredColumns() {
        UUID user = newUser();
        UUID sourceId = UUID.randomUUID();
        ResolverDecision d = resolver.resolve(new ResolverInput(
                user, RiskStateSourceType.LLM_CLASSIFIER, sourceId,
                preFilter(2, 0.7), classifier(2, 0.75)));

        RiskStateHistory row = d.historyRow();
        assertThat(row.getUserId()).isEqualTo(user);
        assertThat(row.getSourceType()).isEqualTo(RiskStateSourceType.LLM_CLASSIFIER);
        assertThat(row.getSourceId()).isEqualTo(sourceId);
        assertThat(row.getRuleVersion()).isEqualTo("SAFETY_TEST@v1");
        assertThat(row.getModelVersion()).isEqualTo(RiskClassifierOutput.PROVIDER_MOCK_V1);
        assertThat(row.getPromptVersion()).isEqualTo(RiskClassifierOutput.DEMO_PROMPT_VERSION);
        assertThat(row.getConfidence()).isEqualByComparingTo(new BigDecimal("0.750"));
        assertThat(row.getSchemaVersion()).isEqualTo("V1");
        assertThat(row.getOccurredAt()).isNotNull();
        // OffsetDateTime.now() returns a value with offset; the column
        // type is TIMESTAMP WITH TIME ZONE in H2 — round-tripping
        // preserves the absolute instant.
        assertThat(row.getOccurredAt().toInstant())
                .isCloseTo(OffsetDateTime.now().toInstant(),
                        org.assertj.core.api.Assertions.within(5L, java.time.temporal.ChronoUnit.SECONDS));
    }
}
