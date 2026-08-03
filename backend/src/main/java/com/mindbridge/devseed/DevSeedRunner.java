package com.mindbridge.devseed;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * CLI entry point for the G2-T09 dev seed.
 *
 * <h2>Activation gate</h2>
 * <p>The bean only exists when {@code mindbridge.seed.run=true} is set
 * explicitly. Default is {@code false} in {@code application.yml}, so a
 * stock {@code ./mvnw spring-boot:run} never invokes the seed.
 *
 * <h2>Production guard</h2>
 * <p>Even with the flag set, the seed refuses to run when the active
 * profile is {@code prod}. This is a defensive check in case an operator
 * accidentally copies {@code mindbridge.seed.run=true} from a dev config
 * into a production deploy.
 *
 * <h2>Typical invocation</h2>
 * <pre>
 * java -jar backend/target/mindbridge-backend-*.jar \
 *      --spring.profiles.active=local \
 *      --mindbridge.seed.run=true \
 *      --mindbridge.seed.scenario=DEFAULT
 * </pre>
 */
@Component
@ConditionalOnProperty(prefix = "mindbridge.seed", name = "run", havingValue = "true")
public class DevSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private final DevSeedProperties properties;
    private final DevSeedService service;
    private final Environment environment;

    public DevSeedRunner(DevSeedProperties properties,
                         DevSeedService service,
                         Environment environment) {
        this.properties = properties;
        this.service = service;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        List<String> activeProfiles = List.of(environment.getActiveProfiles());
        if (activeProfiles.contains("prod")) {
            throw new IllegalStateException(
                    "Dev seed is not allowed on profile=prod (active profiles: "
                            + activeProfiles + ")");
        }

        log.info("DevSeedRunner active — resetting demo data and seeding scenario={}",
                properties.scenario());

        service.reset();
        SeedResult result = service.run(properties.scenario());

        log.info("DevSeedRunner finished in {} ms — users={}, assignments={}, answers={}, "
                        + "sessions={}, messages={}, events={}",
                result.elapsed().toMillis(),
                result.usersCreated(),
                result.assignmentsCreated(),
                result.answersCreated(),
                result.chatSessionsCreated(),
                result.chatMessagesCreated(),
                result.behavioralEventsEmitted());
    }
}