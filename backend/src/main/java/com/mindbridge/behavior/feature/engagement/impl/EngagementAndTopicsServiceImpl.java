package com.mindbridge.behavior.feature.engagement.impl;

import com.mindbridge.behavior.feature.engagement.EngagementAndTopicsService;
import com.mindbridge.behavior.feature.engagement.config.EngagementConfig;
import com.mindbridge.behavior.feature.engagement.dto.EngagementAndTopicsResult;
import com.mindbridge.behavior.feature.engagement.dto.TopicFrequency;
import com.mindbridge.behavior.feature.engagement.repository.DominantTopicsRepository;
import com.mindbridge.behavior.feature.dto.BehavioralEventCountsRow;
import com.mindbridge.behavior.repository.BehavioralEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * G4-T08 default implementation of {@link EngagementAndTopicsService}.
 *
 * <p><b>Algorithm (v1-unweighted).</b>
 * <ol>
 *   <li>Validate inputs (fail-fast on null userId / targetDate / zoneId /
 *       config).</li>
 *   <li>Compute the 7d window {@code [targetDate-6..targetDate]} and the
 *       30d window {@code [targetDate-29..targetDate]} in the user's
 *       local timezone (each window's UTC bounds are then derived).</li>
 *   <li>For each window, run {@code BehavioralEventRepository.aggregateByUserAndDay}
 *       per day (7 + 30 = 57 calls in MVP — acceptable because the
 *       query is O(1) thanks to the 4 conditional aggregates). Sum the
 *       three source counts and cap at the unweighted ceiling of 3.
 *       <p><i>Sum-then-cap</i> matches the "binary per source per day"
 *       formula: each of {@code chat-message}, {@code chat-session},
 *       {@code checkin-completed} contributes either 0 or 1 per day,
 *       and 7/30 days of 1's from one source sum to 7/30 - but the
 *       MVP semantics are "was the source active on AT LEAST ONE day
 *       in the window", so we cap per source at 1, then sum the 3
 *       capped values.</p></li>
 *   <li>For each window, run {@code DominantTopicsRepository.groupActiveTopicsByUserInWindow}
 *       to GROUP BY topic, filter ACTIVE rows + optional confidence floor,
 *       then order by frequency DESC (tie-break: topic ASC for
 *       determinism), take the top {@link EngagementConfig#getMaxTopicCount()},
 *       compute {@code share = frequency / totalFrequency}.</li>
 *   <li>Assemble the {@link EngagementAndTopicsResult} with the
 *       stamped {@link EngagementConfig#CALCULATION_VERSION}.</li>
 * </ol>
 *
 * <p><b>Why per-day loop instead of one window query.</b> The MVP
 * behavioural-events table is small (G2-T07 estimate &lt; 1k rows per
 * active user per month) and the existing 4-aggregate query already
 * takes the closed-open UTC window. A future optimisation is to add a
 * single-window aggregate to the repository, but that would expand the
 * query from 4 to 4 * 2 = 8 conditional aggregates - more bytes on the
 * wire and a bigger coupling surface. MVP stays explicit.
 *
 * <p><b>Not persisted.</b> Per Phase 1 Q1 (xung đột #1, decision B),
 * the result is NOT written to {@code user_daily_features.engagement_score}
 * (V21 domain = [0, 1]). The {@link EngagementAndTopicsResult} is
 * derived on demand by the profile endpoint / future controllers.
 */
@Service
public class EngagementAndTopicsServiceImpl implements EngagementAndTopicsService {

    private static final Logger log = LoggerFactory.getLogger(EngagementAndTopicsServiceImpl.class);

    /** Recent short window size, in days (FEATURE_DICTIONARY §9.3, T06). */
    static final int SHORT_WINDOW_DAYS = 7;
    /** Recent long window size, in days (FEATURE_DICTIONARY §9.3, T06). */
    static final int LONG_WINDOW_DAYS = 30;

    /**
     * Three MVP engagement sources, mapped to their behavioural-event
     * counters via {@link BehavioralEventCountsRow}. Order is significant:
     * it defines the [0, 3] domain semantics.
     */
    private static final int MAX_ACTIVITY_SCORE = 3;

    private final BehavioralEventRepository behavioralEventRepository;
    private final DominantTopicsRepository dominantTopicsRepository;

    public EngagementAndTopicsServiceImpl(
            BehavioralEventRepository behavioralEventRepository,
            DominantTopicsRepository dominantTopicsRepository) {
        this.behavioralEventRepository = behavioralEventRepository;
        this.dominantTopicsRepository = dominantTopicsRepository;
        log.info("EngagementAndTopicsService initialized, calculationVersion={}",
                EngagementConfig.CALCULATION_VERSION);
    }

    @Override
    @Transactional(readOnly = true)
    public EngagementAndTopicsResult summarizeForUser(
            UUID userId, LocalDate targetDate, ZoneId zoneId, EngagementConfig config) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(targetDate, "targetDate");
        Objects.requireNonNull(zoneId, "zoneId");
        Objects.requireNonNull(config, "config");

        Window7d w7 = computeShortWindow(targetDate, zoneId);
        Window30d w30 = computeLongWindow(targetDate, zoneId);

        int score7d = computeEngagementActivityScore(userId, w7.from, w7.to);
        int score30d = computeEngagementActivityScore(userId, w30.from, w30.to);

        List<TopicFrequency> topics7d = computeDominantTopics(
                userId, w7.from, w7.to, config.getMinTopicConfidence(), config.getMaxTopicCount());
        List<TopicFrequency> topics30d = computeDominantTopics(
                userId, w30.from, w30.to, config.getMinTopicConfidence(), config.getMaxTopicCount());

        if (log.isDebugEnabled()) {
            log.debug("G4-T08 summarize: userId={} tz={} targetDate={} score7d={} score30d={} top7d={} top30d={}",
                    userId, zoneId, targetDate,
                    score7d, score30d, topics7d.size(), topics30d.size());
        }

        return new EngagementAndTopicsResult(
                userId,
                score7d, score30d,
                topics7d, topics30d,
                EngagementConfig.CALCULATION_VERSION);
    }

    // --- Score calculation ---

    /**
     * The v1-unweighted formula: for each of the three MVP sources
     * ({@code chat-message}, {@code chat-session}, {@code checkin-completed}),
     * return 1 if the source was active on AT LEAST ONE day in the window,
     * else 0. Sum the three bits and cap at {@link #MAX_ACTIVITY_SCORE} = 3.
     *
     * <p>The {@code exercise-started / exercise-completed} sources are
     * NOT counted here because they require G5 runtime
     * ({@code exercise_assignments} table). Per Phase 1 Q-noted answer
     * on {@code NOT_APPLICABLE}, the score is therefore upper-bounded at
     * 3 - same as {@link #MAX_ACTIVITY_SCORE} - so the cap is a no-op
     * for MVP. When G5 ships, this method will gain 2 more source bits
     * and the cap will move to 5. The domain check in
     * {@link EngagementAndTopicsResult} will then need a corresponding
     * bump; that future change must come with an explicit task spec.
     */
    int computeEngagementActivityScore(UUID userId, Instant fromUtc, Instant toUtc) {
        int score = 0;
        for (LocalDate day = toDay(fromUtc); !day.isAfter(toDay(toUtc)); day = day.plusDays(1)) {
            BehavioralEventCountsRow row = behavioralEventRepository.aggregateByUserAndDay(
                    userId,
                    day.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                    day.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
            if (row == null) {
                continue;
            }
            // Per-source "active at least once today" flag.
            if (row.getChatMessageCount() > 0L)         score++;
            if (row.getActiveChatSessionCount() > 0L)  score++;
            if (row.getCheckinCompletedCount() > 0L)   score++;
            if (score >= MAX_ACTIVITY_SCORE) {
                return MAX_ACTIVITY_SCORE;
            }
        }
        return Math.min(score, MAX_ACTIVITY_SCORE);
    }

    // --- Topic aggregation ---

    List<TopicFrequency> computeDominantTopics(
            UUID userId, Instant fromUtc, Instant toUtc,
            java.math.BigDecimal minConfidence, int maxTopicCount) {
        if (maxTopicCount < 1) {
            throw new IllegalArgumentException(
                    "maxTopicCount must be >= 1; got " + maxTopicCount);
        }
        List<DominantTopicsRepository.TopicCountRow> rows = dominantTopicsRepository
                .groupActiveTopicsByUserInWindow(userId, toUtc(fromUtc), toUtc(toUtc), minConfidence);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        // Order by frequency DESC, tie-break by topic ASC for determinism
        // (the same input must produce the same output for the same
        // database state - required by DoD #2 rerun-aware testing).
        List<DominantTopicsRepository.TopicCountRow> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator
                .comparingLong(DominantTopicsRepository.TopicCountRow::getFrequency).reversed()
                .thenComparing(DominantTopicsRepository.TopicCountRow::getTopic));

        long total = sorted.stream().mapToLong(DominantTopicsRepository.TopicCountRow::getFrequency).sum();
        if (total <= 0L) {
            return List.of();
        }

        int cap = Math.min(maxTopicCount, sorted.size());
        List<TopicFrequency> result = new ArrayList<>(cap);
        for (int i = 0; i < cap; i++) {
            DominantTopicsRepository.TopicCountRow r = sorted.get(i);
            double share = roundHalfUp(((double) r.getFrequency()) / ((double) total), 4);
            result.add(new TopicFrequency(r.getTopic(), r.getFrequency(), share));
        }
        return result;
    }

    // --- Window math ---

    private record Window7d(Instant from, Instant to) {}
    private record Window30d(Instant from, Instant to) {}

    private Window7d computeShortWindow(LocalDate targetDate, ZoneId zoneId) {
        LocalDate fromDate = targetDate.minusDays(SHORT_WINDOW_DAYS - 1L);
        return new Window7d(
                toUtc(fromDate.atStartOfDay(zoneId).toOffsetDateTime()),
                toUtc(targetDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime()));
    }

    private Window30d computeLongWindow(LocalDate targetDate, ZoneId zoneId) {
        LocalDate fromDate = targetDate.minusDays(LONG_WINDOW_DAYS - 1L);
        return new Window30d(
                toUtc(fromDate.atStartOfDay(zoneId).toOffsetDateTime()),
                toUtc(targetDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime()));
    }

    private static Instant toUtc(OffsetDateTime odt) {
        return odt.toInstant();
    }

    private static LocalDate toDay(Instant utc) {
        return utc.atOffset(java.time.ZoneOffset.UTC).toLocalDate();
    }

    private static OffsetDateTime toUtc(Instant instant) {
        return instant.atOffset(java.time.ZoneOffset.UTC);
    }

    /**
     * Round a double to {@code decimals} places, HALF_UP. Pulled out so
     * the topic-share semantics are explicit and do not depend on the
     * default rounding of {@link Double#toString()} (which can produce
     * 0.30000000000000004 artefacts that break DoD #3 round-trip
     * verification).
     */
    private static double roundHalfUp(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return value;
        }
        double scale = Math.pow(10.0, decimals);
        return Math.round(value * scale) / scale;
    }
}
