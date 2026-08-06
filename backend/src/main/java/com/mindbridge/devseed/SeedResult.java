package com.mindbridge.devseed;

import java.time.Duration;

/**
 * Summary record returned by the G2-T09 seed after a successful run.
 *
 * <p>Captures the counts that downstream assertions can verify:
 * <ul>
 *   <li>Users created.</li>
 *   <li>Daily-question assignments created (15 users × 30 days × 5 templates
 *       = 2250 expected, modulo sporadic skips).</li>
 *   <li>Answers created (subset of assignments — sporadic group skips ~30%).</li>
 *   <li>Chat sessions created.</li>
 *   <li>Chat messages created.</li>
 *   <li>Behavioral events emitted by the existing services.</li>
 *   <li>Total elapsed wall time of the seed run.</li>
 * </ul>
 */
public record SeedResult(
        int usersCreated,
        int assignmentsCreated,
        int answersCreated,
        int chatSessionsCreated,
        int chatMessagesCreated,
        long behavioralEventsEmitted,
        Duration elapsed
) {
    public static SeedResult empty() {
        return new SeedResult(0, 0, 0, 0, 0, 0L, Duration.ZERO);
    }
}