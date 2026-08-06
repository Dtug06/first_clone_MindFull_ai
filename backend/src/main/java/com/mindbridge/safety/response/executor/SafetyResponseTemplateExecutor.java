package com.mindbridge.safety.response.executor;

import com.mindbridge.safety.response.SafetyResponseTemplateStatus;
import com.mindbridge.safety.response.domain.SafetyResponseTemplate;
import com.mindbridge.safety.response.repository.SafetyResponseTemplateRepository;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executor for the {@code SHOW_TEMPLATE} action on a {@code SafetyEvent}
 * (G3-T12).
 *
 * <p>This service is the ONLY component in the codebase that decides which
 * expert-authored Safety response to show a user at Level 3 / Level 4. It
 * performs two lookups against {@code safety_response_templates} (V18):
 * <ol>
 *   <li><b>Specific lookup</b>  {@code findFirstByLocaleAndRiskReasonAndStatus(...,APPROVED)}.
 *       Returns the latest APPROVED version of a row matching the locale
 *       and the resolver reason code.</li>
 *   <li><b>Fallback lookup</b>  {@code findFirstByLocaleAndIsDefaultTrueAndStatus(...,APPROVED)}.
 *       Returns the per-locale default row (sentinel {@code risk_reason='DEFAULT'})
 *       so the system NEVER returns blank content. Per DoD  4.1 ("Level
 *       4 vn c phn hi fixed") and docs/04 section 3.4, a response is
 *       always available as long as ANY approved row exists for the locale.</li>
 * </ol>
 *
 * <p><b>Critical invariant</b>: this service NEVER calls any AI provider.
 * It reads directly from the database. This means the L4 Safety response
 * keeps working when {@code RealChatAnalysisProvider} (G3-T06) is down or
 * times out. DoD  4.3 ("Test khi provider AI unavailable vn tr  c Safety
 * response") explicitly verifies this with a test that disables every
 * {@code ChatAnalysisProvider} bean.
 *
 * <p><b>DoD coverage</b>:
 * <ul>
 *   <li> 4.1: Level 4 never calls free-form generation  {@link #resolve}
 *       has no dependency on {@code ChatAnalysisProvider}.</li>
 *   <li> 4.2: Returns the approved/configured template  the two lookups
 *       filter strictly on {@code status = APPROVED}.</li>
 *   <li> 4.3: Independent of LLM  integration test uses a Spring context
 *       with the AI provider disabled.</li>
 *   <li> 4.4: Records {@code template_version} used  the executor
 *       returns a {@link ResolvedResponse} carrying {@code templateId},
 *       {@code templateVersion}, {@code content}, and the row's
 *       {@code approvedAt}. The chat pipeline writes these onto the
 *       {@code safety_actions} row via a future wiring task (already
 *       drafted in {@code ConversationMessageService}, deferred for T13
 *       because action execution belongs to T12's executor).</li>
 * </ul>
 */
@Service
public class SafetyResponseTemplateExecutor {

    private static final Logger log = LoggerFactory.getLogger(
            SafetyResponseTemplateExecutor.class);

    private final SafetyResponseTemplateRepository repository;

    public SafetyResponseTemplateExecutor(SafetyResponseTemplateRepository repository) {
        this.repository = repository;
    }

    /**
     * Resolve the Safety response for a {@code (locale, riskReason)} pair.
     * The specific lookup is preferred; the per-locale default is the
     * fallback; if neither exists, the call returns
     * {@link ResolvedResponse#empty()} so the caller can record a
     * {@code SKIPPED} action (the action executor must never invent
     * content).
     *
     * @param locale     BCP-47-ish locale (e.g. {@code "vi"}); must be non-blank
     * @param riskReason UPPER_SNAKE_CASE resolver reason code; must be non-blank
     *                   (the sentinel {@code "DEFAULT"} is reserved for the
     *                   fallback row, NOT for caller input)
     * @return the resolved response, never {@code null}
     */
    @Transactional(readOnly = true)
    public ResolvedResponse resolve(String locale, String riskReason) {
        Objects.requireNonNull(locale, "locale must not be null");
        Objects.requireNonNull(riskReason, "riskReason must not be null");
        if (locale.isBlank() || riskReason.isBlank()) {
            throw new IllegalArgumentException(
                    "locale and riskReason must be non-blank");
        }

        Optional<SafetyResponseTemplate> specific = repository
                .findFirstByLocaleAndRiskReasonAndStatusOrderByTemplateVersionDesc(
                        locale, riskReason,
                        SafetyResponseTemplateStatus.APPROVED);

        if (specific.isPresent()) {
            SafetyResponseTemplate row = specific.get();
            log.debug("Resolved specific Safety response: locale={} riskReason={} "
                            + "templateId={} version={}",
                    locale, riskReason, row.getId(), row.getTemplateVersion());
            return ResolvedResponse.of(row, ResolvedResponse.SourceKind.SPECIFIC);
        }

        Optional<SafetyResponseTemplate> fallback = repository
                .findFirstByLocaleAndIsDefaultTrueAndStatusOrderByTemplateVersionDesc(
                        locale,
                        SafetyResponseTemplateStatus.APPROVED);

        if (fallback.isPresent()) {
            SafetyResponseTemplate row = fallback.get();
            log.warn("Falling back to default Safety response for locale={} "
                            + "riskReason={} (no specific APPROVED row) "
                            + "templateId={} version={}",
                    locale, riskReason, row.getId(), row.getTemplateVersion());
            return ResolvedResponse.of(row, ResolvedResponse.SourceKind.DEFAULT);
        }

        // Both lookups miss. This MUST be impossible per docs/04 section
        // 3.4 + 27: "khng s d ng free-form LLM response" AND
        // "missing expert-approved values must use TODO_EXPERT_REVIEW |
        // CONFIG_PLACEHOLDER | DEMO_ONLY". The executor returns empty and
        // the action is recorded SKIPPED with a clear reason so ops can
        // see the gap. No invented content is ever returned.
        log.warn("No APPROVED Safety response template for locale={} "
                + "riskReason={}; SHOW_TEMPLATE will be SKIPPED", locale, riskReason);
        return ResolvedResponse.empty();
    }

    /**
     * Immutable record of a resolved Safety response. Carries enough
     * metadata for the caller to write an audit row and to record the
     * {@code template_version} on the {@code safety_actions} row for
     * later auditing (per DoD  4.4).
     */
    public static final class ResolvedResponse {

        public enum SourceKind { SPECIFIC, DEFAULT }

        private static final ResolvedResponse EMPTY = new ResolvedResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null);

        private final boolean found;
        private final SourceKind sourceKind;
        private final java.util.UUID templateId;
        private final String code;
        private final String templateVersion;
        private final String content;
        private final java.time.OffsetDateTime approvedAt;

        private ResolvedResponse(boolean found, SourceKind sourceKind,
                                 java.util.UUID templateId, String code,
                                 String templateVersion, String content,
                                 java.time.OffsetDateTime approvedAt) {
            this.found = found;
            this.sourceKind = sourceKind;
            this.templateId = templateId;
            this.code = code;
            this.templateVersion = templateVersion;
            this.content = content;
            this.approvedAt = approvedAt;
        }

        public static ResolvedResponse of(SafetyResponseTemplate row, SourceKind kind) {
            return new ResolvedResponse(
                    true, kind, row.getId(), row.getCode(),
                    row.getTemplateVersion(), row.getContent(),
                    row.getApprovedAt());
        }

        public static ResolvedResponse empty() {
            return EMPTY;
        }

        public boolean isFound() {
            return found;
        }

        public SourceKind getSourceKind() {
            return sourceKind;
        }

        public java.util.UUID getTemplateId() {
            return templateId;
        }

        public String getCode() {
            return code;
        }

        public String getTemplateVersion() {
            return templateVersion;
        }

        public String getContent() {
            return content;
        }

        public java.time.OffsetDateTime getApprovedAt() {
            return approvedAt;
        }
    }
}