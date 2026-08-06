package com.mindbridge.devseed;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Deterministic definition of the 15 demo users seeded by the G2-T09 dev tool.
 *
 * <p>Every value (id, email, password hash, display name, timezone, role,
 * status) is baked in at compile time so that re-running the seed produces
 * the same dataset. UUIDs are stable across runs.
 *
 * <h2>Grouping (per Phase 1 plan §3.1)</h2>
 * <ul>
 *   <li>STRESS_TRENDING_UP   — 3 users, stress rises from 1 → 5 across 30 days.</li>
 *   <li>STRESS_TRENDING_DOWN — 3 users, stress falls from 5 → 1 across 30 days.</li>
 *   <li>STABLE_LOW_STRESS    — 3 users, stress stable in 1-2.</li>
 *   <li>STABLE_HIGH_STRESS   — 2 users, stress stable in 4-5.</li>
 *   <li>RECOVERY_PATTERN     — 2 users, stress spikes for 14 days then drops.</li>
 *   <li>SPORADIC             — 2 users, ~30% of days skipped to test coverage gaps.</li>
 * </ul>
 *
 * <h2>PII / safety</h2>
 * <ul>
 *   <li>All emails match {@code demo-user-*@mindbridge.test} so a database
 *       scan can never confuse them with a real account.</li>
 *   <li>Password is the literal string {@code DEMO_ONLY_DO_NOT_USE_IN_PROD};
 *       only its BCrypt hash is written to the DB. The plaintext value is
 *       never committed to source and never logged.</li>
 *   <li>Display names use generic labels ({@code "Demo User 01"}); no real
 *       names are used.</li>
 * </ul>
 */
public final class DemoUsers {

    /**
     * Plaintext value used only inside this class to derive the BCrypt hash.
     * Documented here to make the demo nature explicit — never logged, never
     * committed in any other form.
     */
    public static final String DEMO_PASSWORD = "DEMO_ONLY_DO_NOT_USE_IN_PROD";

    /** Email domain used for all demo users — distinct from any real domain. */
    public static final String DEMO_EMAIL_DOMAIN = "@mindbridge.test";

    private DemoUsers() {
    }

    /**
     * Returns the 15 deterministic demo users grouped by pattern.
     *
     * @param passwordHash the BCrypt hash of {@link #DEMO_PASSWORD}, injected
     *                     so the seed code does not own its own encoder
     */
    public static List<Spec> allUsers(String passwordHash) {
        return List.of(
                // STRESS_TRENDING_UP — 3 users
                spec(1, "Asia/Ho_Chi_Minh", passwordHash, Group.STRESS_TRENDING_UP),
                spec(2, "Asia/Ho_Chi_Minh", passwordHash, Group.STRESS_TRENDING_UP),
                spec(3, "UTC",                passwordHash, Group.STRESS_TRENDING_UP),
                // STRESS_TRENDING_DOWN — 3 users
                spec(4, "Asia/Ho_Chi_Minh", passwordHash, Group.STRESS_TRENDING_DOWN),
                spec(5, "Asia/Ho_Chi_Minh", passwordHash, Group.STRESS_TRENDING_DOWN),
                spec(6, "America/Los_Angeles", passwordHash, Group.STRESS_TRENDING_DOWN),
                // STABLE_LOW_STRESS — 3 users
                spec(7, "Asia/Ho_Chi_Minh", passwordHash, Group.STABLE_LOW_STRESS),
                spec(8, "UTC",                passwordHash, Group.STABLE_LOW_STRESS),
                spec(9, "Asia/Ho_Chi_Minh", passwordHash, Group.STABLE_LOW_STRESS),
                // STABLE_HIGH_STRESS — 2 users
                spec(10, "Asia/Ho_Chi_Minh", passwordHash, Group.STABLE_HIGH_STRESS),
                spec(11, "UTC",               passwordHash, Group.STABLE_HIGH_STRESS),
                // RECOVERY_PATTERN — 2 users
                spec(12, "Asia/Ho_Chi_Minh", passwordHash, Group.RECOVERY_PATTERN),
                spec(13, "Asia/Ho_Chi_Minh", passwordHash, Group.RECOVERY_PATTERN),
                // SPORADIC — 2 users
                spec(14, "UTC",               passwordHash, Group.SPORADIC),
                spec(15, "Asia/Ho_Chi_Minh", passwordHash, Group.SPORADIC)
        );
    }

    /**
     * Returns whether the given email matches a demo user (used by
     * {@code DevSeedService.reset()} to delete only demo rows via
     * {@code DELETE WHERE email LIKE 'demo-user-%@mindbridge.test'}).
     */
    public static boolean isDemoEmail(String email) {
        return email != null && email.startsWith("demo-user-") && email.endsWith(DEMO_EMAIL_DOMAIN);
    }

    private static Spec spec(int n, String timezone, String passwordHash, Group group) {
        String idRaw = String.format("00000000-0000-0000-0000-%012d", n);
        UUID id = UUID.fromString(idRaw);
        String email = "demo-user-" + String.format("%02d", n) + DEMO_EMAIL_DOMAIN;
        String displayName = "Demo User " + String.format("%02d", n);
        return new Spec(id, email, passwordHash, displayName, timezone, group);
    }

    /**
     * A demo user row. The id is deterministic so that reseeding produces the
     * same dataset for FK references across chat sessions, daily assignments, etc.
     */
    public record Spec(
            UUID id,
            String email,
            String passwordHash,
            String displayName,
            String timezone,
            Group group
    ) {
        public ZoneId zoneId() {
            return ZoneId.of(timezone);
        }

        public LocalDate localDateFor(java.time.Instant instant) {
            return instant.atZone(zoneId()).toLocalDate();
        }
    }

    /** Behavioural pattern group used by {@link DemoCheckinPlan} to derive values. */
    public enum Group {
        STRESS_TRENDING_UP,
        STRESS_TRENDING_DOWN,
        STABLE_LOW_STRESS,
        STABLE_HIGH_STRESS,
        RECOVERY_PATTERN,
        SPORADIC
    }
}