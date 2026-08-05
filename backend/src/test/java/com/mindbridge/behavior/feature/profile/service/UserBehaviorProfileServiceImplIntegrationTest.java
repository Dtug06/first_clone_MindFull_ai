package com.mindbridge.behavior.feature.profile.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.behavior.feature.engagement.dto.TopicFrequency;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.profile.entity.UserBehaviorProfile;
import com.mindbridge.behavior.feature.profile.repository.UserBehaviorProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = {"classpath:schema-user-behavior-profiles.sql"})
class UserBehaviorProfileServiceImplIntegrationTest {

    @Autowired
    UserBehaviorProfileService profileService;

    @Autowired
    UserBehaviorProfileRepository repository;

    @Test
    @DisplayName("upsert inserts new row, findLatestForUser returns it")
    void upsert_insertNewRow() {
        UUID userId = UUID.randomUUID();
        LocalDate windowEnd = LocalDate.of(2026, 8, 4);
        ProfileSnapshot snapshot = sampleSnapshot(userId, windowEnd);

        boolean wrote = profileService.upsert(snapshot);
        assertThat(wrote).isTrue();

        Optional<ProfileSnapshot> loaded = profileService.findLatestForUser(userId);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().userId()).isEqualTo(userId);
        assertThat(loaded.get().windowEnd()).isEqualTo(windowEnd);
        assertThat(loaded.get().engagementScore7d()).isEqualTo(2);
        assertThat(loaded.get().engagementScore30d()).isEqualTo(3);
        assertThat(loaded.get().dataCoverage()).isEqualByComparingTo("0.600");
        assertThat(loaded.get().confidence()).isEqualByComparingTo("0.500");
        assertThat(loaded.get().dataQualityStatus()).isEqualTo(DataQualityStatus.SUFFICIENT);
        assertThat(loaded.get().dominantTopics7d()).hasSize(1);
        assertThat(loaded.get().dominantTopics7d().get(0).topic()).isEqualTo("WORK_STRESS");
    }

    @Test
    @DisplayName("upsert updates existing row when calculated_at is fresher")
    void upsert_updatesExistingRowWhenFresher() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime older = OffsetDateTime.parse("2026-08-04T00:00:00Z");
        OffsetDateTime newer = OffsetDateTime.parse("2026-08-05T00:00:00Z");

        profileService.upsert(snapshotWithTimestamp(userId, LocalDate.of(2026, 8, 4),
                older, 1, DataQualityStatus.LOW));
        boolean wrote = profileService.upsert(
                snapshotWithTimestamp(userId, LocalDate.of(2026, 8, 5), newer, 2,
                        DataQualityStatus.SUFFICIENT));
        assertThat(wrote).isTrue();

        ProfileSnapshot loaded = profileService.findLatestForUser(userId).orElseThrow();
        assertThat(loaded.engagementScore7d()).isEqualTo(2);
        assertThat(loaded.windowEnd()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(loaded.dataQualityStatus()).isEqualTo(DataQualityStatus.SUFFICIENT);
    }

    @Test
    @DisplayName("upsert is noop when existing row is fresher (race-safety)")
    void upsert_noopWhenExistingRowFresher() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime newer = OffsetDateTime.parse("2026-08-05T00:00:00Z");
        OffsetDateTime older = OffsetDateTime.parse("2026-08-04T00:00:00Z");

        profileService.upsert(snapshotWithTimestamp(userId, LocalDate.of(2026, 8, 5),
                newer, 3, DataQualityStatus.INSUFFICIENT));
        boolean wrote = profileService.upsert(
                snapshotWithTimestamp(userId, LocalDate.of(2026, 8, 4), older, 1,
                        DataQualityStatus.SUFFICIENT));
        assertThat(wrote).isFalse();

        ProfileSnapshot loaded = profileService.findLatestForUser(userId).orElseThrow();
        assertThat(loaded.engagementScore7d()).isEqualTo(3);
        assertThat(loaded.windowEnd()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(loaded.dataQualityStatus()).isEqualTo(DataQualityStatus.INSUFFICIENT);
    }

    @Test
    @DisplayName("concurrent upserts for same user converge to a single row")
    void upsert_concurrentSameUser_converge() throws Exception {
        UUID userId = UUID.randomUUID();
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger writes = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    start.await();
                    OffsetDateTime ts = OffsetDateTime.parse("2026-08-05T00:00:00Z")
                            .plusSeconds(idx);
                    boolean wrote = profileService.upsert(
                            snapshotWithTimestamp(userId, LocalDate.of(2026, 8, 5),
                                    ts, idx, DataQualityStatus.SUFFICIENT));
                    if (wrote) writes.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(finished).isTrue();

        long rowCount = repository.findAll().stream()
                .filter(r -> r.getUserId().equals(userId))
                .count();
        assertThat(rowCount).isEqualTo(1L);
        assertThat(writes.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("findLatestForUser returns empty for unknown user")
    void findLatestForUser_empty() {
        Optional<ProfileSnapshot> loaded =
                profileService.findLatestForUser(UUID.randomUUID());
        assertThat(loaded).isEmpty();
    }

    @Test
    @DisplayName("dominant topics round-trip via JSON serialization")
    void dominantTopicsRoundTrip() {
        UUID userId = UUID.randomUUID();
        LocalDate windowEnd = LocalDate.of(2026, 8, 4);
        ProfileSnapshot snapshot = new ProfileSnapshot(
                userId, windowEnd,
                null, null, null, null, null, null, null, null, null, null,
                1, 2,
                null,
                List.of(
                        new TopicFrequency("WORK_STRESS", 5L, 0.6250),
                        new TopicFrequency("SLEEP", 3L, 0.3750)),
                List.of(),
                null, null,
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(0.5),
                DataQualityStatus.LOW,
                OffsetDateTime.now());

        profileService.upsert(snapshot);
        ProfileSnapshot loaded = profileService.findLatestForUser(userId).orElseThrow();
        assertThat(loaded.dominantTopics7d()).hasSize(2);
        assertThat(loaded.dominantTopics7d().get(0).topic()).isEqualTo("WORK_STRESS");
        assertThat(loaded.dominantTopics7d().get(0).frequency()).isEqualTo(5L);
        assertThat(loaded.dominantTopics7d().get(1).share()).isEqualTo(0.375);
        assertThat(loaded.dataQualityStatus()).isEqualTo(DataQualityStatus.LOW);
    }

    @Test
    @DisplayName("1-row-per-user constraint is enforced (UNIQUE)")
    void uniqueConstraint_perUser() {
        UUID userId = UUID.randomUUID();
        profileService.upsert(sampleSnapshot(userId, LocalDate.of(2026, 8, 4)));
        long count = repository.findAll().stream()
                .filter(r -> r.getUserId().equals(userId))
                .count();
        assertThat(count).isEqualTo(1L);

        UserBehaviorProfile row = repository.findByUserId(userId).orElseThrow();
        assertThat(row.getProfileVersion()).isEqualTo(UserBehaviorProfile.PROFILE_VERSION);
        assertThat(row.getCalculationVersion()).contains("trend=v1");
        assertThat(row.getDataQualityStatus()).isEqualTo(DataQualityStatus.SUFFICIENT);
    }

    private ProfileSnapshot sampleSnapshot(UUID userId, LocalDate windowEnd) {
        return snapshotWithTimestamp(userId, windowEnd, OffsetDateTime.now(), 2,
                DataQualityStatus.SUFFICIENT);
    }

    private ProfileSnapshot snapshotWithTimestamp(UUID userId, LocalDate windowEnd,
                                               OffsetDateTime calculatedAt,
                                               int engagement7d,
                                               DataQualityStatus status) {
        return new ProfileSnapshot(
                userId, windowEnd,
                BigDecimal.valueOf(0.4), BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(0.6), BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(0.6), BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(7.0), BigDecimal.valueOf(7.5),
                BigDecimal.valueOf(0.3), BigDecimal.valueOf(0.3),
                engagement7d, 3,
                "{\"calculationVersion\":\"trend_v1\"}",
                List.of(new TopicFrequency("WORK_STRESS", 5L, 1.0)),
                List.of(),
                (short) 2, UUID.randomUUID(),
                BigDecimal.valueOf(0.6),
                BigDecimal.valueOf(0.5),
                status,
                calculatedAt);
    }
}