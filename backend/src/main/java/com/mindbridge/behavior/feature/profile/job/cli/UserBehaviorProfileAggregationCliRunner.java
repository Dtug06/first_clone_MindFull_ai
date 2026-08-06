package com.mindbridge.behavior.feature.profile.job.cli;

import com.mindbridge.behavior.feature.profile.job.UserBehaviorProfileAggregationJobService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Manual G4 profile backfill entry point; disabled unless explicitly enabled. */
@Component
@ConditionalOnProperty(
        name = "mindbridge.profile-aggregation.run.enabled",
        havingValue = "true")
public class UserBehaviorProfileAggregationCliRunner implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(UserBehaviorProfileAggregationCliRunner.class);

    private final UserBehaviorProfileAggregationJobService service;
    private final String target;

    public UserBehaviorProfileAggregationCliRunner(
            UserBehaviorProfileAggregationJobService service,
            @Value("${mindbridge.profile-aggregation.run.target:}") String target) {
        this.service = service;
        this.target = target;
    }

    @Override
    public void run(String... args) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException(
                    "Profile target is required: ALL:YYYY-MM-DD or USER:<uuid>:YYYY-MM-DD");
        }
        String[] parts = target.trim().split(":");
        String kind = parts[0].toUpperCase(Locale.ROOT);
        try {
            if ("ALL".equals(kind) && parts.length == 2) {
                LocalDate date = LocalDate.parse(parts[1]);
                int attempted = service.aggregateAllForDate(date);
                log.info("G4 profile CLI completed kind=ALL date={} attempted={}", date, attempted);
                return;
            }
            if ("USER".equals(kind) && parts.length == 3) {
                UUID userId = UUID.fromString(parts[1]);
                LocalDate date = LocalDate.parse(parts[2]);
                boolean updated = service.aggregateOneUser(userId, date);
                log.info("G4 profile CLI completed kind=USER userId={} date={} updated={}",
                        userId, date, updated);
                return;
            }
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid profile aggregation target: " + target, ex);
        }
        throw new IllegalArgumentException("Invalid profile aggregation target: " + target);
    }
}
