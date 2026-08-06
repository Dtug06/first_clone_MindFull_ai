package com.mindbridge.behavior.feature.profile.job;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * G4-T09 scheduled job: refreshes the current behavior profile for every
 * ACTIVE user every day at 03:15 UTC (15 min after T05 daily-feature job).
 *
 * <p>Disabled by default ({@code mindbridge.profile-aggregation.enabled=false})
 * to keep local development predictable. The CLI runner
 * ({@code UserBehaviorProfileAggregationCliRunner}) is always available
 * for backfill / dev usage.
 */
@Component
@ConditionalOnProperty(name = "mindbridge.profile-aggregation.enabled",
        havingValue = "true", matchIfMissing = false)
public class UserBehaviorProfileAggregationJob {

    private static final Logger log =
            LoggerFactory.getLogger(UserBehaviorProfileAggregationJob.class);

    private final UserBehaviorProfileAggregationJobService service;
    private final Clock clock;

    public UserBehaviorProfileAggregationJob(
            UserBehaviorProfileAggregationJobService service,
            Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(cron = "${mindbridge.profile-aggregation.schedule-cron:0 15 3 * * *}")
    public void runDailyAggregation() {
        LocalDate yesterday = LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(1);
        log.info("G4-T09 scheduled job starting for date={}", yesterday);
        try {
            int attempted = service.aggregateAllForDate(yesterday);
            log.info("G4-T09 scheduled job finished: attempted={}", attempted);
        } catch (Exception e) {
            log.error("G4-T09 scheduled job failed", e);
        }
    }
}
