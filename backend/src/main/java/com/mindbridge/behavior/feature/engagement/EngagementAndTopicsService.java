package com.mindbridge.behavior.feature.engagement;

import com.mindbridge.behavior.feature.engagement.config.EngagementConfig;
import com.mindbridge.behavior.feature.engagement.dto.EngagementAndTopicsResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * G4-T08 service interface for the engagement-score + dominant-topics
 * summary that backs the user-matching / behaviour-profile feature.
 *
 * <p>Pure read-only calculator. Combines two data sources:
 * <ol>
 *   <li>{@code behavioral_events} (counted by
 *       {@link com.mindbridge.behavior.repository.BehavioralEventRepository#aggregateByUserAndDay})
 *       to compute the unweighted engagement activity score in [0, 3].</li>
 *   <li>{@code chat_analysis_results} (counted by
 *       {@link com.mindbridge.behavior.feature.engagement.repository.DominantTopicsRepository#groupActiveTopicsByUserInWindow})
 *       to compute the top-N dominant topics per window.</li>
 * </ol>
 *
 * <p><b>DoD coverage.</b>
 * <ul>
 *   <li>DoD #1 ("Engagement nằm trong miền giá trị định nghĩa"):
 *       enforced by the {@code v1-unweighted} formula + range check in
 *       {@link com.mindbridge.behavior.feature.engagement.dto.EngagementAndTopicsResult}.</li>
 *   <li>DoD #2 ("Topic rerun không bị đếm trùng"):
 *       the GROUP BY query filters {@code analysis_status = ACTIVE} at
 *       the SQL layer.</li>
 *   <li>DoD #3 ("Profile chỉ chứa summary cần thiết"):
 *       output DTO carries only the topic {@code name()} and frequency;
 *       no raw text, no evidence spans.</li>
 * </ul>
 */
public interface EngagementAndTopicsService {

    /**
     * @param userId     the owning user (must not be {@code null})
     * @param targetDate the last day of both the 7d window and the 30d
     *                   window (must not be {@code null}; today, or any
     *                   historical date). The windows are computed in
     *                   {@code zoneId} then converted to UTC for the
     *                   SQL query.
     * @param zoneId     the user timezone for local-date boundary
     *                   computation (must not be {@code null}; injected
     *                   by the caller / controller, never
     *                   {@code ZoneId.systemDefault()}).
     * @param config     the engagement + topics configuration
     *                   (must not be {@code null}; see
     *                   {@link EngagementConfig} for the MVP defaults).
     * @return engagement + topics summary, never {@code null}.
     * @throws IllegalArgumentException if any required parameter is
     *         {@code null} or {@code zoneId} is not a valid IANA ID.
     */
    EngagementAndTopicsResult summarizeForUser(
            UUID userId, LocalDate targetDate, ZoneId zoneId, EngagementConfig config);
}
