package com.mindbridge.behavior.feature.profile;

/**
 * G4-T11 data quality status for a user behavior profile.
 *
 * <p>Three-tier status derived from explicit coverage and inferred confidence.
 * Drives matching deferral and dashboard messaging.
 *
 * <p>Decision rules (user-confirmed 2026-08-05):
 * <ul>
 *   <li>{@link #SUFFICIENT}: {@code dataCoverage >= minCoverageForSufficient}
 *       AND {@code confidence >= minConfidence}.
 *       Matching may run normally.</li>
 *   <li>{@link #LOW}: {@code dataCoverage >= minCoverageForLow}
 *       (and &lt; sufficient) AND {@code confidence >= minConfidence}.
 *       Matching may run but confidence is reduced.</li>
 *   <li>{@link #INSUFFICIENT}: data is too sparse or confidence too low.
 *       Matching SHOULD defer. Profile returns scores with reduced confidence.
 *       Frontend shows "insufficient data" message.</li>
 * </ul>
 *
 * <p>Thresholds come from {@code DataQualityConfig} and are
 * {@code TODO_EXPERT_REVIEW} per
 * {@code docs/analysis/FEATURE_DICTIONARY_v1.md \u00a710.1}.
 * Values default to {@code null} to fail fast — the system
 * refuses to produce a status without expert-approved thresholds.
 *
 * <p>G6 (Program Matching) derives its internal {@code deferRequired}
 * boolean from this enum:
 * {@code INSUFFICIENT \u21d2 deferRequired = true}.
 * The boolean is computed at the G6 layer; this enum is the canonical
 * source of truth.
 */
public enum DataQualityStatus {

    /**
     * Profile has sufficient explicit data coverage and inferred confidence
     * to produce reliable scores. Matching runs normally.
     */
    SUFFICIENT,

    /**
     * Profile has partial explicit data coverage or reduced confidence.
     * Matching may run but results carry lower confidence.
     * Frontend shows a "limited data" advisory.
     */
    LOW,

    /**
     * Profile does not have enough explicit data or inferred confidence.
     * Matching should defer. Scores are present but marked uncertain.
     * Frontend shows "insufficient data" message prompting more check-ins.
     */
    INSUFFICIENT
}
