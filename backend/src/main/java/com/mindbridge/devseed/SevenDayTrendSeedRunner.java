package com.mindbridge.devseed;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * CLI entry point for the seven-day trend seed.
 *
 * <h2>Activation gate</h2>
 * <p>The bean only exists when
 * {@code mindbridge.dev-seed.seven-day-trend.enabled=true} is set explicitly.
 * Default in application.yml is {@code false}, so a stock
 * {@code ./mvnw spring-boot:run} never invokes the seed.
 *
 * <h2>Production guard</h2>
 * <p>Even with the flag set, the seed refuses to run when the active
 * profile is {@code prod}. This is a defensive check.
 *
 * <h2>Typical invocation</h2>
 * <pre>
 * java -jar backend/target/mindbridge-backend-*.jar \
 *      --spring.profiles.active=local \
 *      --mindbridge.dev-seed.seven-day-trend.enabled=true \
 *      --mindbridge.dev-seed.seven-day-trend.user-email=you@email.com
 * </pre>
 * Or with a fixed target date:
 * <pre>
 * java -jar backend/target/mindbridge-backend-*.jar \
 *      --spring.profiles.active=local \
 *      --mindbridge.dev-seed.seven-day-trend.enabled=true \
 *      --mindbridge.dev-seed.seven-day-trend.user-email=you@email.com \
 *      --mindbridge.dev-seed.seven-day-trend.target-date=2026-08-07
 * </pre>
 */
@Component
@ConditionalOnProperty(
        prefix = "mindbridge.dev-seed.seven-day-trend",
        name = "enabled",
        havingValue = "true"
)
public class SevenDayTrendSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SevenDayTrendSeedRunner.class);

    private final SevenDayTrendSeedProperties properties;
    private final SevenDayTrendSeedService service;
    private final Environment environment;

    public SevenDayTrendSeedRunner(
            SevenDayTrendSeedProperties properties,
            SevenDayTrendSeedService service,
            Environment environment) {
        this.properties = properties;
        this.service = service;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        // --- Production guard ---
        List<String> activeProfiles = List.of(environment.getActiveProfiles());
        if (activeProfiles.contains("prod")) {
            throw new IllegalStateException(
                    "Seven-day trend seed is not allowed on profile=prod (active profiles: "
                            + activeProfiles + ")");
        }

        // --- User email required ---
        String email = properties.userEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "mindbridge.dev-seed.seven-day-trend.user-email must be set. "
                            + "Register a user first, then pass --mindbridge.dev-seed.seven-day-trend.user-email=you@example.com");
        }

        log.info("SevenDayTrendSeedRunner active - user={} targetDate={}",
                email, properties.targetDate());

        SevenDayTrendSeedResult result = service.run(email, properties.targetDate());

        log.info("SevenDayTrendSeedRunner finished: user={} day1={} targetDate={} "
                        + "assignments={} answers={} profileUpserted={}",
                result.userEmail(),
                result.day1(),
                result.targetDate(),
                result.assignmentsCreated(),
                result.answersCreated(),
                result.profileUpserted());

        // Print the trend summary
        log.info("=== Seven-day trend summary ===");
        for (var day : result.dayResults()) {
            log.info("Day {}: stress={} mood={} sleep={} energy={}",
                    day.localDate(),
                    day.values().get(0), // STRESS
                    day.values().get(1), // MOOD
                    day.values().get(2), // SLEEP
                    day.values().get(3)  // ENERGY
            );
        }
        log.info("=== Expected: stress decreasing, mood/energy/sleep increasing ===");
    }
}