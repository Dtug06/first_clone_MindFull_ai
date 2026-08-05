package com.mindbridge.behavior.feature.profile.repository;

import com.mindbridge.behavior.feature.profile.entity.UserBehaviorProfile;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * G4-T09 repository for the mutable current behavior profile (one row / user).
 *
 * <p>The UPSERT SQL lives in
 * {@code UserBehaviorProfileServiceImpl.upsert(...)} (native query) so we can
 * use {@code ON CONFLICT (user_id) DO UPDATE ... WHERE EXCLUDED.calculated_at
 * >= user_behavior_profiles.calculated_at} - this is the race-safety
 * mechanism (decision #3, Option A confirmed). Spring Data derived
 * methods do not support that pattern.
 *
 * <p>Out of scope (deferred to T10): snapshot table reads, history lookups.
 */
@Repository
public interface UserBehaviorProfileRepository
        extends JpaRepository<UserBehaviorProfile, UUID> {

    Optional<UserBehaviorProfile> findByUserId(UUID userId);

    /**
     * Total profiles whose {@code calculated_at} is older than the cutoff.
     * Used by ops dashboards to alert on stale profiles.
     */
    @Query("SELECT COUNT(p) FROM UserBehaviorProfile p "
            + "WHERE p.calculatedAt < :cutoff")
    long countStaleProfiles(@Param("cutoff") OffsetDateTime cutoff);
}