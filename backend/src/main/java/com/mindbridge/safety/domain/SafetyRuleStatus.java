package com.mindbridge.safety.domain;

/**
 * Lifecycle status of a {@link SafetyKeywordRule}. Modelled after the CBT
 * content versioning status in {@code docs/04_SAFETY_AND_CBT_RULES.md}
 * §15: the same four values are used so the pre-filter can reuse the
 * approval workflow pattern.
 *
 * <p>Only rules with status {@link #APPROVED} are loaded by the
 * pre-filter service. DRAFT and PENDING_REVIEW rules are ignored to
 * prevent unapproved clinical content from leaking into production
 * inference.
 */
public enum SafetyRuleStatus {

    /** Editable. Never evaluated by the pre-filter. */
    DRAFT,

    /** Awaiting expert review. Never evaluated. */
    PENDING_REVIEW,

    /**
     * Approved by an expert and active in the pre-filter. Only one row per
     * {@code code} may be in this status (enforced by partial unique
     * index).
     */
    APPROVED,

    /** Previously approved, now disabled. Never evaluated. */
    RETIRED
}
