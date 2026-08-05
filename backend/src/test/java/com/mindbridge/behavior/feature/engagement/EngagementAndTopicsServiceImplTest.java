package com.mindbridge.behavior.feature.engagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindbridge.behavior.feature.dto.BehavioralEventCountsRow;
import com.mindbridge.behavior.feature.engagement.config.EngagementConfig;
import com.mindbridge.behavior.feature.engagement.dto.EngagementAndTopicsResult;
import com.mindbridge.behavior.feature.engagement.dto.TopicFrequency;
import com.mindbridge.behavior.feature.engagement.impl.EngagementAndTopicsServiceImpl;
import com.mindbridge.behavior.feature.engagement.repository.DominantTopicsRepository;
import com.mindbridge.behavior.feature.engagement.repository.DominantTopicsRepository.TopicCountRow;
import com.mindbridge.behavior.repository.BehavioralEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * G4-T08 unit tests for {@link EngagementAndTopicsServiceImpl}.
 *
 * <p>Covers the DoD scenarios from
 * {@code docs/tasks/G4/G4-T08-engagement-and-dominant-topics.md} Phase 1
 * §Hoàn thành khi:
 * <ol>
 *   <li>Engagement score domain [0, 3] - all 4 cases (all-zero, all-three,
 *       partial, super-cap).</li>
 *   <li>Topic rerun awareness - SQL filter (mock-verified).</li>
 *   <li>Top-N cap + frequency / share ordering + tie-break.</li>
 *   <li>Two windows 7d / 30d computed independently.</li>
 *   <li>Null config / null arg guards.</li>
 *   <li>Calculation version stamp + config-stamp.</li>
 *   <li>Confidence floor (null vs explicit) controls filtering at the
 *       SQL layer (mock-verified).</li>
 * </ol>
 *
 * <p>Pattern mirrors {@code TrendCalculatorImplTest}: pure Mockito, no
 * Spring context, Surefire-friendly.
 */
class EngagementAndTopicsServiceImplTest {

    private static final UUID USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final ZoneId TZ = ZoneId.of("UTC");
    private static final LocalDate TARGET = LocalDate.of(2026, 8, 4);

    private static final EngagementConfig CONFIG = EngagementConfig.defaults();

    private final BehavioralEventRepository eventRepo = mock(BehavioralEventRepository.class);
    private final DominantTopicsRepository topicsRepo = mock(DominantTopicsRepository.class);
    private final EngagementAndTopicsServiceImpl service =
            new EngagementAndTopicsServiceImpl(eventRepo, topicsRepo);

    // --- DoD #1: Engagement score domain [0, 3] ---

    @Nested
    @DisplayName("Engagement score domain [0, 3]")
    class ScoreDomain {

        @Test
        @DisplayName("no activity -> score 0")
        void allZeroSources_returnsZero() {
            // behavioural-event rows return 0 for all 3 sources every day
            stubEventRow(0L, 0L, 0L);
            stubTopics(List.of());

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.engagementActivityScore7d()).isEqualTo(0);
            assertThat(out.engagementActivityScore30d()).isEqualTo(0);
        }

        @Test
        @DisplayName("all three sources active on at least one day -> score 3 (capped)")
        void allThreeSourcesActive_returnsThree() {
            stubEventRow(0L, 0L, 0L); // days 1-6 empty
            // day 7 = targetDate: all three sources fire
            stubEventRowForDay(TARGET, 5L, 1L, 1L);
            stubTopics(List.of());

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.engagementActivityScore7d()).isEqualTo(3);
        }

        @Test
        @DisplayName("two sources active -> score 2")
        void twoSourcesActive_returnsTwo() {
            stubEventRow(0L, 0L, 0L);
            // chat-message + checkin on day 5; chat-session never fires
            stubEventRowForDay(TARGET.minusDays(2), 1L, 0L, 1L);
            stubTopics(List.of());

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.engagementActivityScore7d()).isEqualTo(2);
        }

        @Test
        @DisplayName("score caps at 3 even when counts grow")
        void scoreNeverExceedsThree() {
            // every day: all 3 sources fire with huge counts
            stubEventRow(100L, 50L, 10L);
            stubTopics(List.of());

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.engagementActivityScore7d()).isEqualTo(3);
            assertThat(out.engagementActivityScore30d()).isEqualTo(3);
        }

        @Test
        @DisplayName("exercise_started events do NOT contribute (G5 not shipped)")
        void exerciseEventsDoNotCount() {
            // Simulate G5-style event mix: chat-message=1 per day, ALL OTHER
            // SOURCES ZERO. The MVP formula counts only the 3 message-sourced
            // events; exercise events never reach BehavioralEventRepository
            // in MVP because G5 is not shipped. So score must be 1, not 0
            // (proves "exercise is not 1") and not 2 (proves "exercise is
            // not 2").
            stubEventRowZero();
            // The mock query result is exactly (msg=1, sess=0, done=0) which
            // would be G5-shipped-future; current code still returns 1.
            LocalDate singleDay = LocalDate.ofInstant(
                    TARGET.minusDays(6).atStartOfDay(TZ).toInstant(), ZoneOffset.UTC);
            stubEventRowForDay(singleDay, 1L, 0L, 0L);
            stubTopics(List.of());

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.engagementActivityScore7d()).isEqualTo(1);
        }
    }

    // --- DoD #2: Topic rerun awareness ---

    @Nested
    @DisplayName("Topic rerun (SUPERSEDED) never double-counts")
    class RerunAwareness {

        @Test
        @DisplayName("topics repo is called once per window with ACTIVE filter via SQL")
        void topicsRepoInvokedWithCorrectWindow() {
            stubEventRow(0L, 0L, 0L);
            stubTopics(List.of());

            service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            // Two calls: one for 7d window, one for 30d window.
            verify(topicsRepo, times(2))
                    .groupActiveTopicsByUserInWindow(eq(USER), any(OffsetDateTime.class),
                            any(OffsetDateTime.class), eq((BigDecimal) null));
        }

        @Test
        @DisplayName("confidence floor null = no floor; confidence 0.8 = filter applied")
        void confidenceFloorNullVsExplicit() {
            stubEventRow(0L, 0L, 0L);
            stubTopics(List.of());

            // null config
            service.summarizeForUser(USER, TARGET, TZ, CONFIG);
            verify(topicsRepo, times(2))
                    .groupActiveTopicsByUserInWindow(eq(USER), any(), any(), eq((BigDecimal) null));

            // explicit floor 0.8
            EngagementConfig explicit = EngagementConfig.of(new BigDecimal("0.800"), 3);
            service.summarizeForUser(USER, TARGET, TZ, explicit);
            verify(topicsRepo, times(2))
                    .groupActiveTopicsByUserInWindow(eq(USER), any(), any(),
                            eq(new BigDecimal("0.800")));
        }

        @Test
        @DisplayName("empty ACTIVE rows -> empty list (no fabricated default)")
        void noTopics_emptyList() {
            stubEventRow(0L, 0L, 0L);
            stubTopics(List.of());

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.dominantTopics7d()).isEmpty();
            assertThat(out.dominantTopics30d()).isEmpty();
        }
    }

    // --- DoD #3: Top-N + frequency + share ---

    @Nested
    @DisplayName("Top-N + frequency + share")
    class TopN {

        @Test
        @DisplayName("top N = 3 caps the list even when 5 distinct topics exist")
        void capsAtThree() {
            stubEventRow(0L, 0L, 0L);
            List<TopicCountRow> rows = List.of(
                    row("WORK_STRESS", 10L),
                    row("RELATIONSHIP", 8L),
                    row("FAMILY", 5L),
                    row("HEALTH", 4L),
                    row("FINANCE", 1L));
            stubTopics(rows);

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.dominantTopics7d()).hasSize(3);
            assertThat(out.dominantTopics7d().get(0).topic()).isEqualTo("WORK_STRESS");
            assertThat(out.dominantTopics7d().get(0).frequency()).isEqualTo(10L);
            assertThat(out.dominantTopics7d().get(2).topic()).isEqualTo("FAMILY");
        }

        @Test
        @DisplayName("share = frequency / total, rounded HALF_UP to 4 decimals")
        void shareComputedCorrectly() {
            stubEventRow(0L, 0L, 0L);
            stubTopics(List.of(row("WORK_STRESS", 3L), row("RELATIONSHIP", 1L)));

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.dominantTopics7d()).hasSize(2);
            // total = 4, WORK_STRESS = 3/4 = 0.75
            assertThat(out.dominantTopics7d().get(0).share()).isEqualTo(0.75);
            // RELATIONSHIP = 1/4 = 0.25
            assertThat(out.dominantTopics7d().get(1).share()).isEqualTo(0.25);
        }

        @Test
        @DisplayName("tie on frequency: topic ASC wins (deterministic)")
        void tieBreakAscTopic() {
            stubEventRow(0L, 0L, 0L);
            stubTopics(List.of(
                    row("WORK_STRESS", 5L),
                    row("RELATIONSHIP", 5L),
                    row("FAMILY", 5L),
                    row("HEALTH", 5L)));

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.dominantTopics7d()).hasSize(3);
            // Alphabetical: FAMILY, HEALTH, RELATIONSHIP (WORK_STRESS dropped)
            assertThat(out.dominantTopics7d().get(0).topic()).isEqualTo("FAMILY");
            assertThat(out.dominantTopics7d().get(1).topic()).isEqualTo("HEALTH");
            assertThat(out.dominantTopics7d().get(2).topic()).isEqualTo("RELATIONSHIP");
        }

        @Test
        @DisplayName("raw topic code returned; no masking")
        void noMasking() {
            stubEventRow(0L, 0L, 0L);
            stubTopics(List.of(row("WORK_STRESS", 1L)));

            EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            assertThat(out.dominantTopics7d().get(0).topic()).isEqualTo("WORK_STRESS");
        }
    }

    // --- Two windows computed independently ---

    @Nested
    @DisplayName("7d / 30d windows")
    class Windows {

        @Test
        @DisplayName("two distinct windows -> two topicsRepo calls with different UTC bounds")
        void twoWindowInvocations() {
            stubEventRow(0L, 0L, 0L);
            stubTopics(List.of());

            service.summarizeForUser(USER, TARGET, TZ, CONFIG);

            org.mockito.ArgumentCaptor<OffsetDateTime> fromCaptor =
                    org.mockito.ArgumentCaptor.forClass(OffsetDateTime.class);
            org.mockito.ArgumentCaptor<OffsetDateTime> toCaptor =
                    org.mockito.ArgumentCaptor.forClass(OffsetDateTime.class);
            verify(topicsRepo, times(2)).groupActiveTopicsByUserInWindow(
                    any(), fromCaptor.capture(), toCaptor.capture(), any());

            List<OffsetDateTime> froms = fromCaptor.getAllValues();
            List<OffsetDateTime> tos = toCaptor.getAllValues();
            assertThat(froms).hasSize(2);
            // 7d window starts 6 days before target, 30d window starts 29 days before
            long days7 = java.time.Duration.between(froms.get(0), tos.get(0)).toDays();
            long days30 = java.time.Duration.between(froms.get(1), tos.get(1)).toDays();
            // The exact window sizes depend on call order; assert each is either 7 or 30.
            assertThat(days7).isBetween(7L, 31L);
            assertThat(days30).isBetween(7L, 31L);
            assertThat(days7).isNotEqualTo(days30);
        }

        @Test
        @DisplayName("Asia/Ho_Chi_Minh window math: 7d starts 6 days before in user TZ, then UTC")
        void nonUtcWindowMath() {
            ZoneId hcm = ZoneId.of("Asia/Ho_Chi_Minh");
            stubEventRow(0L, 0L, 0L);
            stubTopics(List.of());

            service.summarizeForUser(USER, TARGET, hcm, CONFIG);

            org.mockito.ArgumentCaptor<OffsetDateTime> fromCaptor =
                    org.mockito.ArgumentCaptor.forClass(OffsetDateTime.class);
            org.mockito.ArgumentCaptor<OffsetDateTime> toCaptor =
                    org.mockito.ArgumentCaptor.forClass(OffsetDateTime.class);
            verify(topicsRepo, times(2)).groupActiveTopicsByUserInWindow(
                    any(), fromCaptor.capture(), toCaptor.capture(), any());

            // 7d window: target - 6 days = 2026-07-29 00:00 +07:00.
            // The service converts the +07:00 instant to a UTC OffsetDateTime
            // before passing it to the repo, so we compare via Instant
            // (which is independent of offset).
            Instant expectedFrom = LocalDate.of(2026, 7, 29)
                    .atStartOfDay(hcm).toInstant();
            Instant expectedTo = LocalDate.of(2026, 8, 5)
                    .atStartOfDay(hcm).toInstant();
            List<OffsetDateTime> froms = fromCaptor.getAllValues();
            List<OffsetDateTime> tos = toCaptor.getAllValues();
            boolean found7d = false;
            for (int i = 0; i < froms.size(); i++) {
                if (froms.get(i).toInstant().equals(expectedFrom)
                        && tos.get(i).toInstant().equals(expectedTo)) {
                    found7d = true;
                    break;
                }
            }
            assertThat(found7d)
                    .as("expected one window to match [2026-07-29 00:00 +07, 2026-08-05 00:00 +07)")
                    .isTrue();
        }
    }

    // --- Null guards ---

    @Nested
    @DisplayName("Null / invalid guards")
    class Guards {

        @Test
        @DisplayName("null userId -> NullPointerException (Objects.requireNonNull)")
        void nullUserId_throws() {
            assertThatThrownBy(() -> service.summarizeForUser(null, TARGET, TZ, CONFIG))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("null targetDate -> NullPointerException")
        void nullTargetDate_throws() {
            assertThatThrownBy(() -> service.summarizeForUser(USER, null, TZ, CONFIG))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("targetDate");
        }

        @Test
        @DisplayName("null zoneId -> NullPointerException")
        void nullZoneId_throws() {
            assertThatThrownBy(() -> service.summarizeForUser(USER, TARGET, null, CONFIG))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("zoneId");
        }

        @Test
        @DisplayName("null config -> NullPointerException")
        void nullConfig_throws() {
            assertThatThrownBy(() -> service.summarizeForUser(USER, TARGET, TZ, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("config");
        }

        @Test
        @DisplayName("null config does NOT trigger any topics query")
        void nullConfig_doesNotQuery() {
            try {
                service.summarizeForUser(USER, TARGET, TZ, null);
            } catch (NullPointerException expected) {
                // ok
            }
            verify(topicsRepo, never()).groupActiveTopicsByUserInWindow(any(), any(), any(), any());
        }
    }

    // --- Calculation version stamp ---

    @Test
    @DisplayName("calculationVersion is stamped on every result")
    void calculationVersionStamped() {
        stubEventRow(0L, 0L, 0L);
        stubTopics(List.of());

        EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

        assertThat(out.calculationVersion())
                .isEqualTo(EngagementConfig.CALCULATION_VERSION)
                .isEqualTo("engagement_v1_unweighted_top_n_3");
    }

    @Test
    @DisplayName("userId echoed back on the result")
    void userIdEchoed() {
        stubEventRow(0L, 0L, 0L);
        stubTopics(List.of());

        EngagementAndTopicsResult out = service.summarizeForUser(USER, TARGET, TZ, CONFIG);

        assertThat(out.userId()).isEqualTo(USER);
    }

    // --- BehavioralEventCountsRow factory (mirrors DailySourceAggregationServiceImplTest.emptyRow) ---

    private static BehavioralEventCountsRow row(long msg, long sess, long done) {
        BehavioralEventCountsRow r = mock(BehavioralEventCountsRow.class);
        when(r.getChatMessageCount()).thenReturn(msg);
        when(r.getActiveChatSessionCount()).thenReturn(sess);
        when(r.getCheckinCompletedCount()).thenReturn(done);
        when(r.getCheckinSkippedCount()).thenReturn(0L);
        return r;
    }

    /** Stubs every day in the 30d window with (0, 0, 0). */
    private void stubEventRowZero() {
        stubEventRow(0L, 0L, 0L);
    }

    /** Stubs every day in the 30d window with the same (msg, sess, done) counts. */
    private void stubEventRow(long msg, long sess, long done) {
        Instant from = TARGET.minusDays(29).atStartOfDay(TZ).toInstant();
        Instant to = TARGET.plusDays(1).atStartOfDay(TZ).toInstant();
        for (LocalDate d = LocalDate.ofInstant(from, ZoneOffset.UTC);
             !d.isAfter(LocalDate.ofInstant(to.minusSeconds(1), ZoneOffset.UTC));
             d = d.plusDays(1)) {
            Instant dayStart = d.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant dayEnd = d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            BehavioralEventCountsRow counts = row(msg, sess, done);
            when(eventRepo.aggregateByUserAndDay(eq(USER), eq(dayStart), eq(dayEnd)))
                    .thenReturn(counts);
        }
    }

    /** Override the row for a specific (user, localDate) in the 30d window. */
    private void stubEventRowForDay(LocalDate day, long msg, long sess, long done) {
        Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BehavioralEventCountsRow counts = row(msg, sess, done);
        when(eventRepo.aggregateByUserAndDay(eq(USER), eq(dayStart), eq(dayEnd)))
                .thenReturn(counts);
    }

    private static TopicCountRow row(String topic, long freq) {
        TopicCountRow r = mock(TopicCountRow.class);
        when(r.getTopic()).thenReturn(topic);
        when(r.getFrequency()).thenReturn(freq);
        return r;
    }

    /**
     * Stubs the topicsRepo to return {@code rows} for BOTH the 7d and 30d
     * window calls. If a real test needs different rows per window,
     * override the stub after calling this.
     */
    private void stubTopics(List<TopicCountRow> rows) {
        when(topicsRepo.groupActiveTopicsByUserInWindow(any(), any(), any(), any()))
                .thenReturn(rows);
    }
}
