package com.mindbridge.behavior.feature.profile.job;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileAggregationService;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileService;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * G4-T09 profile-aggregation orchestration service (drives the scheduled job
 * and the CLI runner).
 *
 * <p>Iterates {@code ACTIVE} users in {@code batchSize} chunks (same pattern
 * as T05 {@code DailyFeatureAggregationServiceImpl.aggregateAllForDate}),
 * aggregates the profile for {@code targetDate}, and hands the result to
 * {@link UserBehaviorProfileService#upsert} which performs the idempotent
 * UPSERT.
 *
 * <p>Per-user failures are logged and skipped so the batch keeps going -
 * same partial-success policy as T05 (G4-T05 decision Q5).
 */
@Service
public class UserBehaviorProfileAggregationJobService {

    private static final Logger log =
            LoggerFactory.getLogger(UserBehaviorProfileAggregationJobService.class);
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final UserBehaviorProfileAggregationService aggregationService;
    private final UserBehaviorProfileService profileService;
    private final UserRepository userRepository;
    private final UserBehaviorProfileAggregationProperties properties;

    public UserBehaviorProfileAggregationJobService(
            UserBehaviorProfileAggregationService aggregationService,
            UserBehaviorProfileService profileService,
            UserRepository userRepository,
            UserBehaviorProfileAggregationProperties properties) {
        this.aggregationService = aggregationService;
        this.profileService = profileService;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    /**
     * Aggregates the profile for every {@code ACTIVE} user at {@code targetDate}.
     *
     * @return number of users attempted
     */
    public int aggregateAllForDate(LocalDate targetDate) {
        int batchSize = (properties != null && properties.batchSize() > 0)
                ? properties.batchSize() : DEFAULT_BATCH_SIZE;
        int page = 0;
        int attempted = 0;
        int succeeded = 0;
        int failed = 0;

        while (true) {
            var userPage = userRepository.findByStatusOrderByIdAsc(
                    User.UserStatus.ACTIVE, PageRequest.of(page, batchSize));
            if (userPage.isEmpty()) break;
            for (User user : userPage) {
                attempted++;
                try {
                    ProfileSnapshot snapshot =
                            aggregationService.aggregateForUser(user.getId(), targetDate);
                    if (profileService.upsert(snapshot)) {
                        succeeded++;
                    } else {
                        succeeded++;
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("G4-T09 aggregateForUser failed: userId={} date={}",
                            user.getId(), targetDate, e);
                }
            }
            page++;
            if (userPage.size() < batchSize) break;
        }

        log.info("G4-T09 aggregateAllForDate finished: date={} attempted={} succeeded={} failed={}",
                targetDate, attempted, succeeded, failed);
        return attempted;
    }

    /**
     * Aggregates the profile for one user at {@code targetDate}.
     *
     * @return {@code true} if the snapshot was upserted, {@code false} if the
     *         existing row was fresher.
     */
    public boolean aggregateOneUser(UUID userId, LocalDate targetDate) {
        ProfileSnapshot snapshot = aggregationService.aggregateForUser(userId, targetDate);
        return profileService.upsert(snapshot);
    }
}