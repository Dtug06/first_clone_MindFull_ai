package com.mindbridge.behavior.feature.job;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mindbridge.feature-aggregation.enabled", havingValue = "true", matchIfMissing = false)
public class DailyFeatureAggregationJob {
    private static final Logger log = LoggerFactory.getLogger(DailyFeatureAggregationJob.class);
    private final DailyFeatureAggregationService service;
    private final Clock clock;

    public DailyFeatureAggregationJob(DailyFeatureAggregationService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(cron = "${mindbridge.feature-aggregation.schedule-cron:0 0 3 * * *}")
    public void runDailyAggregation() {
        LocalDate yesterday = LocalDate.now(clock.withZone(ZoneOffset.UTC)).minusDays(1);
        log.info("G4-T05 scheduled job starting for date={}", yesterday);
        try {
            var summary = service.aggregateAllForDate(yesterday);
            log.info("G4-T05 scheduled job finished: status={} attempted={} succeeded={} failed={}",
                    summary.status(), summary.usersAttempted(), summary.usersSucceeded(), summary.usersFailed());
        } catch (Exception e) {
            log.error("G4-T05 scheduled job failed", e);
        }
    }
}
