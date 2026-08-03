package com.mindbridge.devseed;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Runtime guard for the G2-T09 dev seed.
 *
 * <p>The dev seed was deliberately designed so that {@code *ForSeed(...)}
 * methods on production services ({@code ChatSessionService}, etc.) are
 * {@code public} — Java has no cross-package "friend" mechanism. The
 * {@code DevSeedRunner} activation gate ({@code @ConditionalOnProperty})
 * prevents the seed from running automatically, but a future caller in the
 * {@code chat} or {@code dailyquestion} module could still invoke a seed
 * method by mistake. This guard is a runtime backstop.
 *
 * <p>Each {@code *ForSeed(...)} method calls {@link #requireSeedAllowed()}
 * at the top. The check passes only if either:
 * <ul>
 *   <li>the active Spring profile contains {@code test} (unit/integration
 *       tests call into seed methods directly), OR</li>
 *   <li>{@code mindbridge.seed.run=true} is set on the running application.</li>
 * </ul>
 * If neither condition holds, the call throws {@link IllegalStateException}
 * before touching any business state. The throw is intentionally loud —
 * failing fast is preferable to silently bypassing auth checks.
 */
@Component
@EnableConfigurationProperties(DevSeedProperties.class)
public class SeedGuard {

    private final DevSeedProperties properties;
    private final Environment environment;

    public SeedGuard(DevSeedProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /**
     * Throws {@link IllegalStateException} if neither the seed flag nor a
     * test profile is active. Called from the top of every {@code *ForSeed(...)}
     * method on the production services.
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
        if (testProfile || properties.run()) {
            return;
        }
        throw new IllegalStateException(
                "DevSeedForSeed method invoked while mindbridge.seed.run=false and "
                        + "no 'test' profile is active. This is a programming error — the seed "
                        + "is opt-in only. Production code must use the regular service methods "
                        + "that derive userId from the JWT principal via CurrentUserService.");
    }
}