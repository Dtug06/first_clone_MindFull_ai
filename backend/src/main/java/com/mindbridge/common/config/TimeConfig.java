package com.mindbridge.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a Clock bean so that time-sensitive services (e.g. daily question
 * assignment) can be tested with a fixed instant.
 *
 * - production / local: Clock.systemUTC()
 * - tests: override via @TestConfiguration if needed
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
