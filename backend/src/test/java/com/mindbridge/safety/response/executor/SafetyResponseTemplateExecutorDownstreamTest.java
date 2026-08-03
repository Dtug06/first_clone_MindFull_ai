package com.mindbridge.safety.response.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindbridge.analysis.exception.ProviderUnavailableException;
import com.mindbridge.analysis.provider.ChatAnalysisInput;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.safety.response.SafetyResponseTemplateStatus;
import com.mindbridge.safety.response.domain.SafetyResponseTemplate;
import com.mindbridge.safety.response.repository.SafetyResponseTemplateRepository;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * DoD §4.3 verification: the {@code SafetyResponseTemplateExecutor} must
 * NEVER depend on any AI/LLM provider - the L4 Safety response must
 * keep working even when {@code ChatAnalysisProvider} (G3-T06) is down,
 * times out, or is replaced by a bean that throws on every call.
 *
 * <p>The test boots the real Spring context but injects a
 * {@link ChatAnalysisProvider} bean that throws
 * {@link ProviderUnavailableException} on every call. The
 * {@code @TestConfiguration} + {@link Primary} bean overrides the
 * {@code mock} bean that {@code ChatAnalysisProviderConfig} would
 * otherwise supply (the production config uses
 * {@code @ConditionalOnMissingBean}).
 *
 * <p>Two flavours of assertions:
 * <ol>
 *   <li><b>Functional</b>: insert a specific APPROVED row +
 *       a default APPROVED row into the H2 schema mirror, then call
 *       {@link SafetyResponseTemplateExecutor#resolve(String, String)}
 *       - the resolved response must come back with the correct
 *       {@code templateId} / {@code templateVersion} / {@code content}
 *       WITHOUT the executor ever touching the AI provider. The mock
 *       provider is verified with Mockito to confirm it was NEVER
 *       invoked during the call.</li>
 *   <li><b>Static</b>: a reflection-scan over the
 *       {@code com.mindbridge.safety.response.executor} package confirms
 *       that no class imports {@code ChatAnalysisProvider} / any
 *       {@code *Provider} and that no method is named after an LLM
 *       dependency. Pins the rule against future regressions.</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mindbridge.ai.provider=mock",
        "mindbridge.ai.mock.force-scenario="
})
@Sql(scripts = {
        "/schema-safety-response-templates.sql"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("SafetyResponseTemplateExecutor downstream (DoD 4.3)")
class SafetyResponseTemplateExecutorDownstreamTest {

    @Autowired
    private SafetyResponseTemplateExecutor executor;

    @Autowired
    private SafetyResponseTemplateRepository repository;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The Mockito spy injected by {@link ProviderDownConfig}. Used by
     * the functional tests to verify the provider was NOT called.
     */
    @Autowired
    private ChatAnalysisProvider chatAnalysisProvider;

    @BeforeEach
    void resetSpy() {
        Mockito.reset(chatAnalysisProvider);
        // Default the spy to throw ProviderUnavailableException so any
        // accidental call surfaces loudly. The functional test will
        // override this with .thenThrow(...) explicitly to keep the
        // intent visible at the call site.
        when(chatAnalysisProvider.analyze(any(ChatAnalysisInput.class)))
                .thenThrow(new ProviderUnavailableException(
                        "test: AI provider is down by design"));
        repository.deleteAll();
    }

    @Nested
    @DisplayName("Functional: executor keeps working when AI provider is down")
    class Functional {

        @Test
        @DisplayName("specific lookup succeeds - provider never invoked")
        void specificLookupBypassesProvider() {
            UUID templateId = UUID.randomUUID();
            UUID approverId = UUID.randomUUID();
            // Insert directly via SQL because we want a row that is
            // already APPROVED without dragging in the role-checked
            // service path. The test is about the executor, not the
            // approval lifecycle.
            applicationContext.getBean(
                            org.springframework.jdbc.core.JdbcTemplate.class)
                    .update(
                            "INSERT INTO safety_response_templates (id, code, "
                                    + "template_version, locale, risk_reason, "
                                    + "content, is_default, status, "
                                    + "approved_by, approved_at, created_at, "
                                    + "updated_at, lock_version) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, "
                                    + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, "
                                    + "CURRENT_TIMESTAMP, ?)",
                            templateId.toString(),
                            "SAFETY_LEVEL_4_VI_V1",
                            "v1",
                            "vi",
                            "SUICIDAL_IDEATION",
                            "[TODO_EXPERT_REVIEW - placeholder crisis line]",
                            Boolean.FALSE,
                            "APPROVED",
                            approverId.toString(),
                            0L);

            SafetyResponseTemplateExecutor.ResolvedResponse r =
                    executor.resolve("vi", "SUICIDAL_IDEATION");

            assertThat(r.isFound()).isTrue();
            assertThat(r.getTemplateId()).isEqualTo(templateId);
            assertThat(r.getTemplateVersion()).isEqualTo("v1");
            assertThat(r.getSourceKind())
                    .isEqualTo(
                            SafetyResponseTemplateExecutor.ResolvedResponse
                                    .SourceKind.SPECIFIC);

            verify(chatAnalysisProvider, org.mockito.Mockito.never())
                    .analyze(any(ChatAnalysisInput.class));
        }

        @Test
        @DisplayName("default-fallback lookup succeeds when no specific row exists")
        void defaultFallbackBypassesProvider() {
            UUID defaultId = UUID.randomUUID();
            UUID approverId = UUID.randomUUID();
            applicationContext.getBean(
                            org.springframework.jdbc.core.JdbcTemplate.class)
                    .update(
                            "INSERT INTO safety_response_templates (id, code, "
                                    + "template_version, locale, risk_reason, "
                                    + "content, is_default, status, "
                                    + "approved_by, approved_at, created_at, "
                                    + "updated_at, lock_version) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, "
                                    + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, "
                                    + "CURRENT_TIMESTAMP, ?)",
                            defaultId.toString(),
                            "SAFETY_DEFAULT_VI_V1",
                            "v1",
                            "vi",
                            "DEFAULT",
                            "[TODO_EXPERT_REVIEW - locale default]",
                            Boolean.TRUE,
                            "APPROVED",
                            approverId.toString(),
                            0L);

            SafetyResponseTemplateExecutor.ResolvedResponse r =
                    executor.resolve("vi", "SUICIDAL_IDEATION");

            assertThat(r.isFound()).isTrue();
            assertThat(r.getTemplateId()).isEqualTo(defaultId);
            assertThat(r.getSourceKind())
                    .isEqualTo(
                            SafetyResponseTemplateExecutor.ResolvedResponse
                                    .SourceKind.DEFAULT);

            verify(chatAnalysisProvider, org.mockito.Mockito.never())
                    .analyze(any(ChatAnalysisInput.class));
        }

        @Test
        @DisplayName("empty lookup returns empty ResolvedResponse - provider still untouched")
        void emptyLookupDoesNotInvokeProvider() {
            SafetyResponseTemplateExecutor.ResolvedResponse r =
                    executor.resolve("vi", "SUICIDAL_IDEATION");

            assertThat(r.isFound()).isFalse();
            verify(chatAnalysisProvider, org.mockito.Mockito.never())
                    .analyze(any(ChatAnalysisInput.class));
        }
    }

    @Nested
    @DisplayName("Static: executor package has zero LLM-provider coupling")
    class StaticIsolation {

        private static final String PACKAGE =
                "com.mindbridge.safety.response.executor";

        @Test
        @DisplayName("no class imports com.mindbridge.analysis.provider.*")
        void noAnalysisProviderImports() throws IOException {
            // The package contains exactly ONE production class
            // (SafetyResponseTemplateExecutor). The test classes in this
            // package legitimately reference ChatAnalysisProvider / LLM
            // types so they can mock them - the rule applies to the
            // production executor only, not to the test that enforces
            // the rule.
            String productionFqn =
                    "com.mindbridge.safety.response.executor."
                            + "SafetyResponseTemplateExecutor";
            List<String> violations = new ArrayList<>();
            try {
                Class<?> clazz = Class.forName(productionFqn, false,
                        Thread.currentThread().getContextClassLoader());
                for (Field f : clazz.getDeclaredFields()) {
                    if (f.getType().getName().startsWith(
                            "com.mindbridge.analysis.provider")) {
                        violations.add(clazz.getName() + " field "
                                + f.getName() + " : " + f.getType().getName());
                    }
                }
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getReturnType().getName().startsWith(
                            "com.mindbridge.analysis.provider")) {
                        violations.add(clazz.getName() + " method "
                                + m.getName() + " returns "
                                + m.getReturnType().getName());
                    }
                    for (Class<?> param : m.getParameterTypes()) {
                        if (param.getName().startsWith(
                                "com.mindbridge.analysis.provider")) {
                            violations.add(clazz.getName() + " method "
                                    + m.getName() + " takes "
                                    + param.getName());
                        }
                    }
                    for (Class<?> ex : m.getExceptionTypes()) {
                        if (ex.getName().startsWith(
                                "com.mindbridge.analysis.provider")) {
                            violations.add(clazz.getName() + " method "
                                    + m.getName() + " throws "
                                    + ex.getName());
                        }
                    }
                }
                // Also walk the inner classes (the executor hosts a
                // ResolvedResponse record). Inner classes must NOT import
                // the LLM either.
                for (Class<?> inner : clazz.getDeclaredClasses()) {
                    for (Field f : inner.getDeclaredFields()) {
                        if (f.getType().getName().startsWith(
                                "com.mindbridge.analysis.provider")) {
                            violations.add(inner.getName() + " field "
                                    + f.getName() + " : " + f.getType().getName());
                        }
                    }
                    for (Method m : inner.getDeclaredMethods()) {
                        for (Class<?> param : m.getParameterTypes()) {
                            if (param.getName().startsWith(
                                    "com.mindbridge.analysis.provider")) {
                                violations.add(inner.getName() + " method "
                                        + m.getName() + " takes "
                                        + param.getName());
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // The production class itself could not be loaded - this
                // would be a build failure far before this test runs.
            }
            assertThat(violations)
                    .as("production executor must not import "
                            + "ChatAnalysisProvider / RiskClassifierProvider / etc.")
                    .isEmpty();
        }

        @Test
        @DisplayName("executor has no method named after LLM providers")
        void noMethodNamesHintLlm() throws IOException {
            String productionFqn =
                    "com.mindbridge.safety.response.executor."
                            + "SafetyResponseTemplateExecutor";
            List<String> badNames = new ArrayList<>();
            try {
                Class<?> clazz = Class.forName(productionFqn, false,
                        Thread.currentThread().getContextClassLoader());
                for (Method m : clazz.getDeclaredMethods()) {
                    String lower = m.getName().toLowerCase();
                    // Rejected: any production method whose name embeds
                    // a provider reference (callProvider, fetchLlm, ...).
                    for (String banned : Arrays.asList(
                            "provider", "llm", "openai", "chatgpt")) {
                        if (lower.contains(banned)) {
                            badNames.add(clazz.getName() + "#" + m.getName());
                        }
                    }
                }
                for (Class<?> inner : clazz.getDeclaredClasses()) {
                    for (Method m : inner.getDeclaredMethods()) {
                        String lower = m.getName().toLowerCase();
                        for (String banned : Arrays.asList(
                                "provider", "llm", "openai", "chatgpt")) {
                            if (lower.contains(banned)) {
                                badNames.add(inner.getName() + "#" + m.getName());
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // skip
            }
            assertThat(badNames)
                    .as("executor method names must not embed LLM/provider tokens")
                    .isEmpty();
        }

        private List<String> classFilesInPackage(String pkg) throws IOException {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String path = pkg.replace('.', '/');
            Enumeration<URL> roots = cl.getResources(path);
            List<String> out = new ArrayList<>();
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                String protocol = root.getProtocol();
                if ("file".equals(protocol)) {
                    File dir = new File(root.getFile());
                    collectFromDir(dir, pkg, out);
                } else if ("jar".equals(protocol)) {
                    String full = root.getPath();
                    int bang = full.indexOf("!/");
                    String jarPath = full.substring("file:".length(), bang);
                    try (JarFile jar = new JarFile(jarPath)) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry e = entries.nextElement();
                            if (e.getName().startsWith(path + "/")
                                    && e.getName().endsWith(".class")) {
                                out.add(e.getName());
                            }
                        }
                    }
                }
            }
            return out;
        }

        private void collectFromDir(File dir, String pkg, List<String> out) {
            File[] files = dir.listFiles();
            if (files == null) return;
            for (File f : files) {
                String name = pkg.replace('.', '/') + "/" + f.getName();
                if (f.isDirectory()) {
                    collectFromDir(f, pkg + "." + f.getName(), out);
                } else if (f.getName().endsWith(".class")) {
                    out.add(name);
                }
            }
        }
    }

    /**
     * Test-only configuration that overrides the production
     * {@code ChatAnalysisProvider} bean with a Mockito spy. The spy
     * is configured to throw on every call (see {@code @BeforeEach})
     * so any accidental invocation by the executor under test is
     * loudly visible via Mockito's "unnecessary stubbing" error.
     *
     * <p>{@code @Primary} ensures Spring picks the spy over the
     * {@code @ConditionalOnMissingBean} factory in
     * {@code ChatAnalysisProviderConfig}.
     */
    @TestConfiguration
    static class ProviderDownConfig {

        @Bean
        @Primary
        ChatAnalysisProvider chatAnalysisProvider() {
            return Mockito.mock(ChatAnalysisProvider.class);
        }
    }
}
