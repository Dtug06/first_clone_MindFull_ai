package com.mindbridge.safety.response.service;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.common.audit.AuditActions;
import com.mindbridge.common.audit.AuditActorType;
import com.mindbridge.common.audit.AuditCategory;
import com.mindbridge.common.audit.AuditService;
import com.mindbridge.safety.response.SafetyResponseTemplateStatus;
import com.mindbridge.safety.response.domain.SafetyResponseTemplate;
import com.mindbridge.safety.response.exception.SafetyResponseTemplateInputException;
import com.mindbridge.safety.response.repository.SafetyResponseTemplateRepository;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lifecycle service for {@link SafetyResponseTemplate} (G3-T12).
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Validate the lifecycle: every status transition is funneled
 *       through a method here so the controller-or-script entry point
 *       never mutates the entity directly.</li>
 *   <li>Enforce the role check on {@link #approve(UUID, UUID)}:
 *       the {@code approverId} MUST reference an EXPERT or ADMIN user
 *       (User.role enum). Otherwise the approval is rejected with
 *       {@link SafetyResponseTemplateInputException}. This is the
 *       governance gate that matches docs/04 sections 9 + 15  no
 *       self-approval is allowed.</li>
 *   <li>Emit an audit row on every successful transition. Audit failures
 *       do NOT break the transition (per the {@link AuditService}
 *       REQUIRES_NEW + swallow semantics).</li>
 *   <li>Enforce partial-uniqueness invariants that H2 cannot express in
 *       the test schema (mirroring how T11 handled this for polymorphic
 *       source ids):
 *       <ul>
 *         <li>"At most one APPROVED row per (code, locale, risk_reason)"
 *             is enforced via a pre-flight count() check before the
 *             {@link SafetyResponseTemplate#approve(UUID)} call.</li>
 *         <li>"At most one APPROVED default row per locale" is enforced
 *             similarly.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>No controller is wired here  templates are inserted/promoted by
 * expert tooling (a future back-office task, likely G7). The MVP path is:
 * seed via direct SQL migration (future task) + approve via this service.
 */
@Service
public class SafetyResponseTemplateService {

    private static final Logger log = LoggerFactory.getLogger(
            SafetyResponseTemplateService.class);

    private final SafetyResponseTemplateRepository repository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public SafetyResponseTemplateService(
            SafetyResponseTemplateRepository repository,
            UserRepository userRepository,
            AuditService auditService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Insert a fresh DRAFT row. (Future task inserts via SQL or admin tool;
     * this method exists so admin tooling has a typed entry point.)
     *
     * @return the persisted entity (status = DRAFT).
     */
    @Transactional
    public SafetyResponseTemplate create(
            String code,
            String templateVersion,
            String locale,
            String riskReason,
            String content,
            boolean isDefault) {

        UUID newId = UUID.randomUUID();

        // Reject duplicate (code, template_version) here even though the
        // unique index would catch it too  fail-fast saves a DB roundtrip
        // and gives the admin tool a clearer message.
        repository.findByCodeAndTemplateVersion(code, templateVersion)
                .ifPresent(existing -> {
                    throw new SafetyResponseTemplateInputException(
                            "(code, template_version) already exists: "
                                    + code + " / " + templateVersion);
                });

        SafetyResponseTemplate row = SafetyResponseTemplate.create(
                newId, code, templateVersion, locale, riskReason, content, isDefault);
        SafetyResponseTemplate saved = repository.save(row);

        auditService.record(
                AuditCategory.SAFETY,
                AuditActions.ADMIN_ACTION,
                AuditActorType.SYSTEM,
                null,
                "SAFETY_RESPONSE_TEMPLATE",
                saved.getId(),
                "{\"action\":\"CREATE\",\"code\":\"" + code
                        + "\",\"templateVersion\":\"" + templateVersion
                        + "\",\"locale\":\"" + locale
                        + "\",\"riskReason\":\"" + riskReason
                        + "\",\"isDefault\":" + isDefault + "}");

        log.info("Created SafetyResponseTemplate id={} code={} version={}",
                saved.getId(), code, templateVersion);
        return saved;
    }

    /** Transition: {@code DRAFT  PENDING_REVIEW}. */
    @Transactional
    public SafetyResponseTemplate submitForReview(UUID templateId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        SafetyResponseTemplate row = loadOrThrow(templateId);
        row.submitForReview();
        SafetyResponseTemplate saved = repository.save(row);

        auditService.record(
                AuditCategory.SAFETY,
                AuditActions.ADMIN_ACTION,
                AuditActorType.SYSTEM,
                null,
                "SAFETY_RESPONSE_TEMPLATE",
                saved.getId(),
                "{\"action\":\"SUBMIT_FOR_REVIEW\",\"code\":\""
                        + saved.getCode() + "\",\"version\":\""
                        + saved.getTemplateVersion() + "\"}");

        log.info("Submitted for review SafetyResponseTemplate id={}", saved.getId());
        return saved;
    }

    /**
     * Transition: {@code PENDING_REVIEW  APPROVED}. Enforces the role
     * check (approver MUST be an EXPERT or ADMIN user) plus the partial
     * uniqueness invariants (no other APPROVED row for the same triple;
     * no other APPROVED default row for the same locale).
     */
    @Transactional
    public SafetyResponseTemplate approve(UUID templateId, UUID approverId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        Objects.requireNonNull(approverId, "approverId must not be null");

        // Role gate: only EXPERT or ADMIN can approve.
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new SafetyResponseTemplateInputException(
                        "approver user not found: " + approverId));
        if (approver.getRole() != User.UserRole.EXPERT
                && approver.getRole() != User.UserRole.ADMIN) {
            throw new SafetyResponseTemplateInputException(
                    "approver must have role EXPERT or ADMIN but was "
                            + approver.getRole() + " for userId " + approverId);
        }

        SafetyResponseTemplate row = loadOrThrow(templateId);
        if (row.getStatus() != SafetyResponseTemplateStatus.PENDING_REVIEW) {
            throw new SafetyResponseTemplateInputException(
                    "approve only valid from PENDING_REVIEW but status was "
                            + row.getStatus());
        }

        // Partial-unique pre-flight: at most one APPROVED row per
        // (code, locale, risk_reason). Mirrors V18 partial unique index
        // `safety_response_templates_one_approved_per_triple_uq` for the
        // H2 test schema (and is a no-op double-check on PostgreSQL where
        // the partial index would also fire).
        long approvedSameTriple = repository.findAllByCodeOrderByTemplateVersionDesc(
                row.getCode()).stream()
                .filter(other -> other.getStatus() == SafetyResponseTemplateStatus.APPROVED)
                .filter(other -> !other.getId().equals(row.getId()))
                .filter(other -> other.getLocale().equals(row.getLocale()))
                .filter(other -> other.getRiskReason().equals(row.getRiskReason()))
                .count();
        if (approvedSameTriple > 0) {
            throw new SafetyResponseTemplateInputException(
                    "another APPROVED row already exists for (code="
                            + row.getCode() + ", locale=" + row.getLocale()
                            + ", riskReason=" + row.getRiskReason() + ")");
        }

        // Partial-unique pre-flight: at most one APPROVED default per locale.
        // V18 partial unique index
        // `safety_response_templates_one_default_per_locale_uq` covers ALL
        // codes (not just one code), so we must look across codes too.
        // The dedicated repo method mirrors the executor's fallback lookup
        // and is portable to H2.
        if (row.isDefault()) {
            repository.findFirstByLocaleAndIsDefaultTrueAndStatus(
                            row.getLocale(),
                            SafetyResponseTemplateStatus.APPROVED)
                    .filter(other -> !other.getId().equals(row.getId()))
                    .ifPresent(other -> {
                        throw new SafetyResponseTemplateInputException(
                                "another APPROVED default row already exists for locale="
                                        + row.getLocale() + " (id=" + other.getId() + ")");
                    });
        }

        row.approve(approverId);
        SafetyResponseTemplate saved = repository.save(row);

        auditService.record(
                AuditCategory.SAFETY,
                AuditActions.ADMIN_ACTION,
                AuditActorType.SYSTEM,
                approverId,
                "SAFETY_RESPONSE_TEMPLATE",
                saved.getId(),
                "{\"action\":\"APPROVE\",\"code\":\"" + saved.getCode()
                        + "\",\"version\":\"" + saved.getTemplateVersion()
                        + "\",\"approverId\":\"" + approverId + "\"}");

        log.info("Approved SafetyResponseTemplate id={} approver={}",
                saved.getId(), approverId);
        return saved;
    }

    /** Transition: {@code APPROVED  RETIRED}. */
    @Transactional
    public SafetyResponseTemplate retire(UUID templateId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        SafetyResponseTemplate row = loadOrThrow(templateId);
        row.retire();
        SafetyResponseTemplate saved = repository.save(row);

        auditService.record(
                AuditCategory.SAFETY,
                AuditActions.ADMIN_ACTION,
                AuditActorType.SYSTEM,
                null,
                "SAFETY_RESPONSE_TEMPLATE",
                saved.getId(),
                "{\"action\":\"RETIRE\",\"code\":\""
                        + saved.getCode() + "\",\"version\":\""
                        + saved.getTemplateVersion() + "\"}");

        log.info("Retired SafetyResponseTemplate id={}", saved.getId());
        return saved;
    }

    private SafetyResponseTemplate loadOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new SafetyResponseTemplateInputException(
                        "template not found: " + id));
    }
}
