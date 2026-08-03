package com.mindbridge.safety.response.repository;

import com.mindbridge.safety.response.SafetyResponseTemplateStatus;
import com.mindbridge.safety.response.domain.SafetyResponseTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link SafetyResponseTemplate} (V18).
 *
 * <p>Hot-path queries executed by {@code SafetyResponseTemplateExecutor}
 * (G3-T12) are:
 * <ol>
 *   <li>{@link #findFirstByLocaleAndRiskReasonAndStatusOrderByTemplateVersionDesc(String, String, SafetyResponseTemplateStatus)}
 *      specific lookup  (locale, risk_reason), newest version wins.</li>
 *   <li>{@link #findFirstByLocaleAndIsDefaultTrueAndStatusOrderByTemplateVersionDesc(String, SafetyResponseTemplateStatus)}
 *      fallback lookup  per-locale default row, newest version wins.</li>
 * </ol>
 *
 * <p>Both lookups filter on {@code status = APPROVED}. Together they
 * implement DoD  4.2 ("Response dng  ng template approved/configured") and
 * DoD  4.3 ("Test khi provider AI unavailable vn tr  c Safety response"):
 * the executor reads them WITHOUT calling any AI provider.
 *
 * <p>Status parameters are typed as
 * {@link SafetyResponseTemplateStatus} (the enum) rather than
 * {@code String}. Spring Data JPA maps the enum to its string value
 * at bind time using the entity's
 * {@code @Enumerated(EnumType.STRING)} declaration, so the SQL
 * parameter sent to the DB is still the {@code "APPROVED"} literal.
 * Keeping the enum type in the repository signature also pins the
 * API: callers cannot pass arbitrary strings like
 * {@code "approved"} (lowercase) by accident.
 *
 * <p>Lifecycle / admin queries:
 * <ul>
 *   <li>{@link #findByCodeAndTemplateVersion(String, String)}  exact match
 *       (natural key).</li>
 *   <li>{@link #findAllByCodeOrderByTemplateVersionDesc(String)}  full
 *       version history of one code (admin audit trail).</li>
 * </ul>
 */
@Repository
public interface SafetyResponseTemplateRepository
        extends JpaRepository<SafetyResponseTemplate, UUID> {

    /**
     * Specific lookup: latest APPROVED row for (locale, risk_reason).
     * Backed by {@code safety_response_templates_lookup_idx} (V18).
     */
    Optional<SafetyResponseTemplate> findFirstByLocaleAndRiskReasonAndStatusOrderByTemplateVersionDesc(
            String locale, String riskReason, SafetyResponseTemplateStatus status);

    /**
     * Fallback lookup: latest APPROVED default row for the locale
     * (the sentinel {@code risk_reason = 'DEFAULT'} row, if any).
     * Backed by {@code safety_response_templates_default_lookup_idx} (V18).
     */
    Optional<SafetyResponseTemplate> findFirstByLocaleAndIsDefaultTrueAndStatusOrderByTemplateVersionDesc(
            String locale, SafetyResponseTemplateStatus status);

    /** Exact-match on the natural key. */
    Optional<SafetyResponseTemplate> findByCodeAndTemplateVersion(
            String code, String templateVersion);

    /**
     * All versions of one template family, newest version first. Used by
     * admin tooling to render the version history.
     */
    List<SafetyResponseTemplate> findAllByCodeOrderByTemplateVersionDesc(String code);

    /**
     * Returns any APPROVED default row for the locale across ALL codes.
     * Used by the approval service to enforce the "at most one APPROVED
     * default per locale" invariant in the H2 test schema (where the
     * PostgreSQL partial unique index is not present). Returns at most
     * one row.
     */
    Optional<SafetyResponseTemplate> findFirstByLocaleAndIsDefaultTrueAndStatus(
            String locale, SafetyResponseTemplateStatus status);
}
