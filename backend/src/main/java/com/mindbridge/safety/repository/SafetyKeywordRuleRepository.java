package com.mindbridge.safety.repository;

import com.mindbridge.safety.domain.SafetyKeywordRule;
import com.mindbridge.safety.domain.SafetyRuleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link SafetyKeywordRule}.
 *
 * <p>The hot path for the pre-filter service is
 * {@link #findByStatus(SafetyRuleStatus)} with
 * {@link SafetyRuleStatus#APPROVED} — this is what the service loads on
 * each evaluation to apply every active rule.
 *
 * <p>Write-side helpers (status transitions) are exposed on the entity
 * itself ({@code submitForReview/approve/retire}) and called inside
 * service-layer {@code @Transactional} methods; the repository does
 * not provide convenience update methods.
 */
@Repository
public interface SafetyKeywordRuleRepository extends JpaRepository<SafetyKeywordRule, UUID> {

    /**
     * All rules with the given status. The {@code APPROVED} variant is the
     * pre-filter hot path. Backed by a partial index on
     * {@code (code) WHERE status = 'APPROVED'} (see V13 migration) so the
     * query stays cheap as the rule table grows.
     */
    List<SafetyKeywordRule> findByStatus(SafetyRuleStatus status);

    /**
     * Lookup by natural key. Used by admin/review tooling to fetch a
     * specific version of a rule.
     */
    SafetyKeywordRule findByCodeAndRuleVersion(String code, String ruleVersion);

    /**
     * Latest version of a rule by code, regardless of status. Returns the
     * newest {@code ruleVersion} for the given {@code code} or {@code null}
     * if no row exists.
     *
     * <p>Implementation note: the method name uses Spring Data
     * derived-query ordering. For MVP the rule table is small
     * (single-digit rows) so ordering in-memory is acceptable; if the
     * table grows beyond ~100 rows, switch to a {@code @Query} with
     * {@code ORDER BY rule_version DESC LIMIT 1}.
     */
    List<SafetyKeywordRule> findByCodeOrderByRuleVersionDesc(String code);

    /**
     * Count rules per status. Used by integration tests and by
     * admin tooling (future task) to surface the state of the rule
     * pipeline.
     */
    long countByStatus(SafetyRuleStatus status);
}
