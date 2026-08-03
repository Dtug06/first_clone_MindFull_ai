package com.mindbridge.devseed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the G2-T09 dev seed tool.
 *
 * <p>Bound from {@code mindbridge.seed.*} keys in {@code application.yml}.
 * The seed never runs unless {@code mindbridge.seed.run=true} is set explicitly
 * via CLI args, environment variable, or test-profile convention — see
 * {@code DevSeedRunner} for the {@code @ConditionalOnProperty} gate.
 *
 * @param run       master switch — when false, the seed bean is not loaded.
 *                  Default in {@code application.yml} is {@code false}.
 * @param scenario  which deterministic seed scenario to apply. Currently only
 *                  {@link DevSeedScenario#DEFAULT} is implemented; other values
 *                  are reserved for upcoming G2-T09+ follow-ups.
 */
@ConfigurationProperties(prefix = "mindbridge.seed")
public record DevSeedProperties(
        boolean run,
        DevSeedScenario scenario
) {
    public DevSeedProperties {
        if (scenario == null) {
            scenario = DevSeedScenario.DEFAULT;
        }
    }
}