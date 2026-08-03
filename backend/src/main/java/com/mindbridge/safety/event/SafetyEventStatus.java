package com.mindbridge.safety.event;

/**
 * Lifecycle status of a {@code SafetyEvent}. Mirrors the four values
 * listed in {@code docs/02_DATABASE_MVP.md} section 6.2 and
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} section 8 "Safety Event Status".
 *
 * <p>The transition rules per section 8:
 * <ul>
 *   <li>A new event has status {@link #OPEN}.</li>
 *   <li>Only an allowed role or service may transition the status -
 *       {@code SafetyEventService.recordLevel3Or4Event} only creates
 *       {@code OPEN} events in G3-T11. Transitions
 *       {@code OPEN -> UNDER_REVIEW / RESOLVED / DISMISSED} will be
 *       implemented in G3-T13 (Expert Review).</li>
 *   <li>Every status change must be audited.</li>
 *   <li>{@code RESOLVED} does NOT mean the user has recovered or is
 *       no longer at risk.</li>
 *   <li>{@code DISMISSED} must have a reason code.</li>
 *   <li>Never delete a Safety Event just because it has been handled.</li>
 * </ul>
 */
public enum SafetyEventStatus {

    /** Newly created event. The default for every T11 write. */
    OPEN,

    /**
     * An expert (or automated triage) is reviewing the event. Future
     * state - wired in G3-T13.
     */
    UNDER_REVIEW,

    /**
     * Event has been triaged. Does NOT mean the user has recovered or
     * is no longer at risk (per docs/04 section 8).
     */
    RESOLVED,

    /**
     * Event has been dismissed with a reason code. Still auditable.
     */
    DISMISSED
}