package com.mindbridge.devseed;

/**
 * Seed scenario identifiers for the G2-T09 dev seed tool.
 *
 * <p>All scenarios produce the same 15 demo users with the same timezones —
 * they differ in the trajectory of stress / sleep / energy values across the
 * 30-day window so downstream behavior analysis (G4) has distinguishable
 * signals to test against (trend up, trend down, stable low, stable high,
 * sporadic coverage).
 */
public enum DevSeedScenario {
    /** Default scenario: one user per pattern group as defined in {@link DemoUsers}. */
    DEFAULT,
    /** Reserved for a future scenario that focuses on a single trajectory. */
    TRENDING_UP,
    /** Reserved for a future scenario that focuses on a single trajectory. */
    TRENDING_DOWN
}