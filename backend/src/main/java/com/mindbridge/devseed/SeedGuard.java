package com.mindbridge.devseed;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Runtime guard for dev seed methods.
 *
 * <p>Both the G2-T09 seed ({@code mindbridge.seed.run}) and the G4
 * seven-day trend seed ({@code mindbridge.dev-seed.seven-day-trend.enabled})
 * call into production services via {@code *ForSeed(...)} methods. Those
 * methods are public only because Java has no cross-package "friend"
 * mechanism. This guard is the runtime backstop that prevents accidental
 * invocation from production code.
 *
 * <p>The check passes only if:
 * <ul>
 *   <li>the active Spring profile contains {@code test} (integration
 *       tests call into seed methods directly), OR</li>
 *   <li>{@code mindbridge.seed.run=true} is set (G2-T09), OR</li>
 *   <li>{@code mindbridge.dev-seed.seven-day-trend.enabled=true} is set (G4).</li>
 * </ul>
 *
 * <p>If none of the above holds, the call throws {@link IllegalStateException}
 * before touching any business state. The throw is intentionally loud -
 * failing fast is preferable to silently bypassing auth checks.
 */
@Component
@EnableConfigurationProperties({DevSeedProperties.class, SevenDayTrendSeedProperties.class})
public class SeedGuard {

    private final DevSeedProperties devSeedProperties;
    private final SevenDayTrendSeedProperties trendSeedProperties;
    private final Environment environment;

    public SeedGuard(DevSeedProperties devSeedProperties,
                     SevenDayTrendSeedProperties trendSeedProperties,
                     Environment environment) {
        this.devSeedProperties = devSeedProperties;
        this.trendSeedProperties = trendSeedProperties;
        this.environment = environment;
    }

    /**
     * Throws {@link IllegalStateException} if no seed flag and no test profile
     * is active. Called from the top of every {@code *ForSeed(...)} method.
     */
    public void requireSeedAllowed() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean testProfile = false;
        for (String p : activeProfiles) {
            if ("test".equals(p)) {
                testProfile = true;
                break;
            }
        }
        boolean g2Seed = devSeedProperties != null && devSeedProperties.run();
        boolean g4Seed = trendSeedProperties != null && trendSeedProperties.enabled();
        if (testProfile || g2Seed || g4Seed) {
            return;
        }
        throw new IllegalStateException(
                "DevSeedForSeed method invoked while all seed flags are false "
                        + "(mindbridge.seed.run=false, "
                        + "mindbridge.dev-seed.seven-day-trend.enabled=false) and "
                        + "no 'test' profile is active. This is a programming error - "
                        + "seed methods are opt-in only. Production code must use the "
                        + "regular service methods that derive userId from the JWT "
                        + "principal via CurrentUserService.");
    }
}