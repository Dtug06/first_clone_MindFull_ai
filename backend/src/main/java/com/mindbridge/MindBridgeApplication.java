package com.mindbridge;

import com.mindbridge.behavior.feature.job.DailyFeatureAggregationProperties;
import com.mindbridge.behavior.feature.job.cli.DailyFeatureAggregationCliProperties;
import com.mindbridge.behavior.feature.profile.config.TrendConfigProperties;
import com.mindbridge.behavior.feature.profile.job.UserBehaviorProfileAggregationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 *
 * <p>G4-T05 adds two configuration property classes
 * ({@link DailyFeatureAggregationProperties} for the scheduler,
 * {@link DailyFeatureAggregationCliProperties} for the CLI runner)
 * and enables Spring's scheduled-task support via {@link EnableScheduling}.
 *
 * <p>Both job classes use {@code @ConditionalOnProperty}, so
 * scheduling activates only when
 * {@code mindbridge.feature-aggregation.enabled=true}; CLI runs only
 * when {@code mindbridge.feature-aggregation.run.enabled=true};
 * default for both is false so CI / local dev are unaffected.
 *
 * <p>G4-T09 adds {@link UserBehaviorProfileAggregationProperties} for
 * the profile aggregation scheduler (cron 03:15 UTC, runs after T05).
 *
 * <p>G4-T12 adds {@link TrendConfigProperties} for the trend / streak
 * thresholds consumed by the calculator (close-out of T07 F-1).
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        DailyFeatureAggregationProperties.class,
        DailyFeatureAggregationCliProperties.class,
        UserBehaviorProfileAggregationProperties.class,
        TrendConfigProperties.class
})
public class MindBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindBridgeApplication.class, args);
    }
}
