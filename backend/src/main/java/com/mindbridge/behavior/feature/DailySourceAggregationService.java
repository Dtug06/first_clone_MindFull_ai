package com.mindbridge.behavior.feature;

import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
import java.time.LocalDate;
import java.util.UUID;

/**
 * G4-T03: Read-only aggregator of one day of source data for the behavior
 * analysis pipeline.
 *
 * <p>This service is the single entry point for {@code DailyFeatureCalculatorService}
 * (G4-T04) and {@code UserDailyFeaturePersistService} (G4-T05) to obtain
 * raw inputs (explicit answers, chat analyses, behavioral events, CBT
 * activity) for one (user, localDate, timezone). It is also used by the
 * read-side controller (G4-T12) to satisfy {@code GET /behavior/daily-features}.
 *
 * <p>Scope intentionally excludes:
 * <ul>
 *   <li>Score computation (lives in G4-T04)</li>
 *   <li>Persisting to {@code user_daily_features} (lives in G4-T05)</li>
 *   <li>Any write operation (this service is read-only)</li>
 * </ul>
 *
 * <p>Concurrency: stateless. Safe to call concurrently from many threads.
 * The whole method runs inside a single {@code @Transactional(readOnly = true)}
 * so all reads see one consistent snapshot of the four source tables.
 */
public interface DailySourceAggregationService {

    /**
     * Aggregates all four source-data streams for one user on one
     * local-date in the given IANA timezone.
     *
     * @param userId    target user (required, non-null)
     * @param timezone  IANA timezone id, e.g. {@code "Asia/Ho_Chi_Minh"} or {@code "UTC"} (required, non-null)
     * @param localDate the calendar day in {@code timezone} whose window we aggregate (required, non-null)
     * @return an immutable snapshot of all sources for that day
     * @throws IllegalArgumentException if any argument is null or {@code timezone} is not a valid IANA id
     */
    DailySourceAggregation aggregateForDay(UUID userId, String timezone, LocalDate localDate);
}