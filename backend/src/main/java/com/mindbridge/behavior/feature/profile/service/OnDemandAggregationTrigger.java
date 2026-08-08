package com.mindbridge.behavior.feature.profile.service;

import com.mindbridge.behavior.feature.job.DailyFeatureAggregationService;
import com.mindbridge.behavior.feature.job.dto.UserAggregationResult;
import com.mindbridge.behavior.feature.profile.job.UserBehaviorProfileAggregationJobService;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs the G4 daily-feature and behavior-profile aggregations for one user.
 *
 * <p>This facade is deliberately fail-soft: callers may use it after a Daily
 * Check-in commit or while lazily loading the Dashboard, and an aggregation
 * failure must never invalidate already-persisted user data.</p>
 */
@Service
public class OnDemandAggregationTrigger {

    private static final Logger log = LoggerFactory.getLogger(OnDemandAggregationTrigger.class);

    private final DailyFeatureAggregationService dailyFeatureAggregationService;
    private final UserBehaviorProfileAggregationJobService profileAggregationService;

    public OnDemandAggregationTrigger(
            DailyFeatureAggregationService dailyFeatureAggregationService,
            UserBehaviorProfileAggregationJobService profileAggregationService) {
        this.dailyFeatureAggregationService = dailyFeatureAggregationService;
        this.profileAggregationService = profileAggregationService;
    }

    /**
     * Attempts T05 followed by T09. This method never throws.
     *
     * @return true only when both aggregation stages report success
     */
    public boolean triggerForUserAndDate(UUID userId, LocalDate date) {
        try {
            UserAggregationResult dailyResult =
                    dailyFeatureAggregationService.aggregateOneUser(userId, date);
            if (dailyResult == null || !dailyResult.success()) {
                log.warn("G4 on-demand daily aggregation did not succeed: userId={} date={}",
                        userId, date);
                return false;
            }
        } catch (Exception e) {
            log.warn("G4 on-demand daily aggregation failed: userId={} date={}",
                    userId, date, e);
            return false;
        }

        try {
            if (!profileAggregationService.aggregateOneUser(userId, date)) {
                log.warn("G4 on-demand profile aggregation did not update a profile: userId={} date={}",
                        userId, date);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("G4 on-demand profile aggregation failed: userId={} date={}",
                    userId, date, e);
            return false;
        }
    }
}
