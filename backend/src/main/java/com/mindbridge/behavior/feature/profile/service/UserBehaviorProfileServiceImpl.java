package com.mindbridge.behavior.feature.profile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.behavior.feature.engagement.dto.TopicFrequency;
import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import com.mindbridge.behavior.feature.profile.dto.FeatureSeriesPoint;
import com.mindbridge.behavior.feature.profile.dto.FeatureType;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.profile.dto.WindowType;
import com.mindbridge.behavior.feature.profile.entity.UserBehaviorProfile;
import com.mindbridge.behavior.feature.profile.repository.UserBehaviorProfileRepository;
import com.mindbridge.behavior.feature.window.repository.UserDailyFeatureWindowRepository;
import com.mindbridge.auth.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mindbridge.behavior.feature.job.persistence.DbDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserBehaviorProfileServiceImpl implements UserBehaviorProfileService {

    private static final Logger log =
            LoggerFactory.getLogger(UserBehaviorProfileServiceImpl.class);

    private final UserBehaviorProfileRepository repository;
    private final UserDailyFeatureWindowRepository windowRepository;
    private DbDialect dialect = DbDialect.UNKNOWN;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    private Environment environment;

    @PersistenceContext
    private EntityManager entityManager;

    public UserBehaviorProfileServiceImpl(
            UserBehaviorProfileRepository repository,
            UserDailyFeatureWindowRepository windowRepository,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            Clock clock) {
        this.repository = repository;
        this.windowRepository = windowRepository;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.clock = clock;
        this.dialect = DbDialect.UNKNOWN;
        log.info("G4-T09 dialect unresolved at construction; will resolve on first upsert");
    }
    private DbDialect resolveDialect() {
        String[] keys = {
                "spring.datasource.url",
                "spring.datasource.hikari.jdbc-url",
                "spring.datasource.jdbc-url"
        };
        for (String key : keys) {
            String url = environment.getProperty(key);
            if (url != null) {
                DbDialect d = DbDialect.fromJdbcUrl(url);
                log.info("G4-T09 dialect resolved to {}", d);
                return d;
            }
        }
        log.warn("G4-T09 no datasource URL property found; dialect=UNKNOWN (H2 path assumed)");
        return DbDialect.H2;
    }

    private static final String UPSERT_PG =
            "INSERT INTO user_behavior_profiles ("
                    + "id, user_id, window_end,"
                    + "stress_avg_7d, stress_avg_30d,"
                    + "mood_avg_7d, mood_avg_30d,"
                    + "energy_avg_7d, energy_avg_30d,"
                    + "sleep_avg_7d, sleep_avg_30d,"
                    + "anxiety_avg_7d, anxiety_avg_30d,"
                    + "engagement_score_7d, engagement_score_30d,"
                    + "trend_summary, dominant_topics_7d, dominant_topics_30d,"
                    + "risk_level, risk_history_id,"
                    + "data_coverage, confidence, data_quality_status,"
                    + "profile_version, calculation_version,"
                    + "calculated_at, created_at, updated_at"
                    + ") VALUES ("
                    + ":id, :userId, :windowEnd,"
                    + ":stress7d, :stress30d,"
                    + ":mood7d, :mood30d,"
                    + ":energy7d, :energy30d,"
                    + ":sleep7d, :sleep30d,"
                    + ":anxiety7d, :anxiety30d,"
                    + ":engagement7d, :engagement30d,"
                    + ":trendSummary, CAST(:topics7d AS jsonb), CAST(:topics30d AS jsonb),"
                    + ":riskLevel, :riskHistoryId,"
                    + ":dataCoverage, :confidence, :dataQualityStatus,"
                    + ":profileVersion, :calculationVersion,"
                    + ":calculatedAt, :createdAt, :updatedAt"
                    + ") ON CONFLICT (user_id) DO UPDATE SET"
                    + " window_end = EXCLUDED.window_end,"
                    + " stress_avg_7d = EXCLUDED.stress_avg_7d,"
                    + " stress_avg_30d = EXCLUDED.stress_avg_30d,"
                    + " mood_avg_7d = EXCLUDED.mood_avg_7d,"
                    + " mood_avg_30d = EXCLUDED.mood_avg_30d,"
                    + " energy_avg_7d = EXCLUDED.energy_avg_7d,"
                    + " energy_avg_30d = EXCLUDED.energy_avg_30d,"
                    + " sleep_avg_7d = EXCLUDED.sleep_avg_7d,"
                    + " sleep_avg_30d = EXCLUDED.sleep_avg_30d,"
                    + " anxiety_avg_7d = EXCLUDED.anxiety_avg_7d,"
                    + " anxiety_avg_30d = EXCLUDED.anxiety_avg_30d,"
                    + " engagement_score_7d = EXCLUDED.engagement_score_7d,"
                    + " engagement_score_30d = EXCLUDED.engagement_score_30d,"
                    + " trend_summary = EXCLUDED.trend_summary,"
                    + " dominant_topics_7d = EXCLUDED.dominant_topics_7d,"
                    + " dominant_topics_30d = EXCLUDED.dominant_topics_30d,"
                    + " risk_level = EXCLUDED.risk_level,"
                    + " risk_history_id = EXCLUDED.risk_history_id,"
                    + " data_coverage = EXCLUDED.data_coverage,"
                    + " confidence = EXCLUDED.confidence,"
                    + " data_quality_status = EXCLUDED.data_quality_status,"
                    + " profile_version = EXCLUDED.profile_version,"
                    + " calculation_version = EXCLUDED.calculation_version,"
                    + " calculated_at = EXCLUDED.calculated_at,"
                    + " updated_at = EXCLUDED.updated_at"
                    + " WHERE EXCLUDED.calculated_at >= user_behavior_profiles.calculated_at";
    private static final String INSERT_H2 =
            "INSERT INTO user_behavior_profiles ("
                    + "id, user_id, window_end,"
                    + "stress_avg_7d, stress_avg_30d,"
                    + "mood_avg_7d, mood_avg_30d,"
                    + "energy_avg_7d, energy_avg_30d,"
                    + "sleep_avg_7d, sleep_avg_30d,"
                    + "anxiety_avg_7d, anxiety_avg_30d,"
                    + "engagement_score_7d, engagement_score_30d,"
                    + "trend_summary, dominant_topics_7d, dominant_topics_30d,"
                    + "risk_level, risk_history_id,"
                    + "data_coverage, confidence, data_quality_status,"
                    + "profile_version, calculation_version,"
                    + "calculated_at, created_at, updated_at"
                    + ") VALUES ("
                    + ":id, :userId, :windowEnd,"
                    + ":stress7d, :stress30d,"
                    + ":mood7d, :mood30d,"
                    + ":energy7d, :energy30d,"
                    + ":sleep7d, :sleep30d,"
                    + ":anxiety7d, :anxiety30d,"
                    + ":engagement7d, :engagement30d,"
                    + ":trendSummary, :topics7d, :topics30d,"
                    + ":riskLevel, :riskHistoryId,"
                    + ":dataCoverage, :confidence, :dataQualityStatus,"
                    + ":profileVersion, :calculationVersion,"
                    + ":calculatedAt, :createdAt, :updatedAt"
                    + ")";

    private static final String UPDATE_H2 =
            "UPDATE user_behavior_profiles SET"
                    + " window_end = :windowEnd,"
                    + " stress_avg_7d = :stress7d,"
                    + " stress_avg_30d = :stress30d,"
                    + " mood_avg_7d = :mood7d,"
                    + " mood_avg_30d = :mood30d,"
                    + " energy_avg_7d = :energy7d,"
                    + " energy_avg_30d = :energy30d,"
                    + " sleep_avg_7d = :sleep7d,"
                    + " sleep_avg_30d = :sleep30d,"
                    + " anxiety_avg_7d = :anxiety7d,"
                    + " anxiety_avg_30d = :anxiety30d,"
                    + " engagement_score_7d = :engagement7d,"
                    + " engagement_score_30d = :engagement30d,"
                    + " trend_summary = :trendSummary,"
                    + " dominant_topics_7d = :topics7d,"
                    + " dominant_topics_30d = :topics30d,"
                    + " risk_level = :riskLevel,"
                    + " risk_history_id = :riskHistoryId,"
                    + " data_coverage = :dataCoverage,"
                    + " confidence = :confidence,"
                    + " data_quality_status = :dataQualityStatus,"
                    + " profile_version = :profileVersion,"
                    + " calculation_version = :calculationVersion,"
                    + " calculated_at = :calculatedAt,"
                    + " updated_at = :updatedAt"
                    + " WHERE id = :id AND user_id = :userId";

    @Override
    @Transactional
    public boolean upsert(ProfileSnapshot snapshot) {
        if (dialect == DbDialect.UNKNOWN) {
            synchronized (this) {
                if (dialect == DbDialect.UNKNOWN) {
                    dialect = resolveDialect();
                }
            }
        }
        if (dialect == DbDialect.H2 && !isFresherThanExisting(snapshot)) {
            log.debug("G4-T09 upsert noop (H2 precheck): existing row fresher for userId={}",
                    snapshot.userId());
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        String topics7dJson = serializeTopics(snapshot.dominantTopics7d());
        String topics30dJson = serializeTopics(snapshot.dominantTopics30d());
        UUID id = UUID.randomUUID();

        int rows;
        if (dialect == DbDialect.POSTGRESQL) {
            rows = entityManager.createNativeQuery(UPSERT_PG)
                    .setParameter("id", id)
                    .setParameter("userId", snapshot.userId())
                    .setParameter("windowEnd", snapshot.windowEnd())
                    .setParameter("stress7d", snapshot.stressAvg7d())
                    .setParameter("stress30d", snapshot.stressAvg30d())
                    .setParameter("mood7d", snapshot.moodAvg7d())
                    .setParameter("mood30d", snapshot.moodAvg30d())
                    .setParameter("energy7d", snapshot.energyAvg7d())
                    .setParameter("energy30d", snapshot.energyAvg30d())
                    .setParameter("sleep7d", snapshot.sleepAvg7d())
                    .setParameter("sleep30d", snapshot.sleepAvg30d())
                    .setParameter("anxiety7d", snapshot.anxietyAvg7d())
                    .setParameter("anxiety30d", snapshot.anxietyAvg30d())
                    .setParameter("engagement7d", snapshot.engagementScore7d())
                    .setParameter("engagement30d", snapshot.engagementScore30d())
                    .setParameter("trendSummary", snapshot.trendSummaryJson())
                    .setParameter("topics7d", topics7dJson)
                    .setParameter("topics30d", topics30dJson)
                    .setParameter("riskLevel", snapshot.riskLevel())
                    .setParameter("riskHistoryId", snapshot.riskHistoryId())
                    .setParameter("dataCoverage", snapshot.dataCoverage())
                    .setParameter("confidence", snapshot.confidence())
                    .setParameter("dataQualityStatus", snapshot.dataQualityStatus() == null
                            ? DataQualityStatus.INSUFFICIENT : snapshot.dataQualityStatus().name())
                    .setParameter("profileVersion", UserBehaviorProfile.PROFILE_VERSION)
                    .setParameter("calculationVersion", UserBehaviorProfile.CALCULATION_VERSION)
                    .setParameter("calculatedAt", snapshot.calculatedAt())
                    .setParameter("createdAt", now)
                    .setParameter("updatedAt", now)
                    .executeUpdate();
        } else {
            rows = upsertH2Direct(snapshot, now, topics7dJson, topics30dJson, id);
        }
        boolean wrote = rows > 0;
        if (!wrote) {
            log.debug("G4-T09 upsert noop: existing row fresher for userId={}", snapshot.userId());
        }
        return wrote;
    }
    private int upsertH2Direct(ProfileSnapshot snapshot, OffsetDateTime now,
                               String topics7dJson, String topics30dJson, UUID id) {
        String selectSql = "SELECT id FROM user_behavior_profiles WHERE user_id = :uid";
        @SuppressWarnings("unchecked")
        List<Object> existing = entityManager.createNativeQuery(selectSql)
                .setParameter("uid", snapshot.userId())
                .getResultList();
        UUID existingId = null;
        if (!existing.isEmpty()) {
            Object raw = existing.get(0);
            if (raw instanceof UUID u) {
                existingId = u;
            } else if (raw instanceof byte[] bytes) {
                existingId = toUuid(bytes);
            } else if (raw != null) {
                existingId = UUID.fromString(raw.toString());
            }
        }
        if (existingId == null) {
            return entityManager.createNativeQuery(INSERT_H2)
                    .setParameter("id", id)
                    .setParameter("userId", snapshot.userId())
                    .setParameter("windowEnd", snapshot.windowEnd())
                    .setParameter("stress7d", snapshot.stressAvg7d())
                    .setParameter("stress30d", snapshot.stressAvg30d())
                    .setParameter("mood7d", snapshot.moodAvg7d())
                    .setParameter("mood30d", snapshot.moodAvg30d())
                    .setParameter("energy7d", snapshot.energyAvg7d())
                    .setParameter("energy30d", snapshot.energyAvg30d())
                    .setParameter("sleep7d", snapshot.sleepAvg7d())
                    .setParameter("sleep30d", snapshot.sleepAvg30d())
                    .setParameter("anxiety7d", snapshot.anxietyAvg7d())
                    .setParameter("anxiety30d", snapshot.anxietyAvg30d())
                    .setParameter("engagement7d", snapshot.engagementScore7d())
                    .setParameter("engagement30d", snapshot.engagementScore30d())
                    .setParameter("trendSummary", snapshot.trendSummaryJson())
                    .setParameter("topics7d", topics7dJson)
                    .setParameter("topics30d", topics30dJson)
                    .setParameter("riskLevel", snapshot.riskLevel())
                    .setParameter("riskHistoryId", snapshot.riskHistoryId())
                    .setParameter("dataCoverage", snapshot.dataCoverage())
                    .setParameter("confidence", snapshot.confidence())
                    .setParameter("dataQualityStatus", snapshot.dataQualityStatus() == null
                            ? DataQualityStatus.INSUFFICIENT : snapshot.dataQualityStatus().name())
                    .setParameter("profileVersion", UserBehaviorProfile.PROFILE_VERSION)
                    .setParameter("calculationVersion", UserBehaviorProfile.CALCULATION_VERSION)
                    .setParameter("calculatedAt", snapshot.calculatedAt())
                    .setParameter("createdAt", now)
                    .setParameter("updatedAt", now)
                    .executeUpdate();
        }
        return entityManager.createNativeQuery(UPDATE_H2)
                .setParameter("id", existingId)
                .setParameter("userId", snapshot.userId())
                .setParameter("windowEnd", snapshot.windowEnd())
                .setParameter("stress7d", snapshot.stressAvg7d())
                .setParameter("stress30d", snapshot.stressAvg30d())
                .setParameter("mood7d", snapshot.moodAvg7d())
                .setParameter("mood30d", snapshot.moodAvg30d())
                .setParameter("energy7d", snapshot.energyAvg7d())
                .setParameter("energy30d", snapshot.energyAvg30d())
                .setParameter("sleep7d", snapshot.sleepAvg7d())
                .setParameter("sleep30d", snapshot.sleepAvg30d())
                .setParameter("anxiety7d", snapshot.anxietyAvg7d())
                .setParameter("anxiety30d", snapshot.anxietyAvg30d())
                .setParameter("engagement7d", snapshot.engagementScore7d())
                .setParameter("engagement30d", snapshot.engagementScore30d())
                .setParameter("trendSummary", snapshot.trendSummaryJson())
                .setParameter("topics7d", topics7dJson)
                .setParameter("topics30d", topics30dJson)
                .setParameter("riskLevel", snapshot.riskLevel())
                .setParameter("riskHistoryId", snapshot.riskHistoryId())
                .setParameter("dataCoverage", snapshot.dataCoverage())
                .setParameter("confidence", snapshot.confidence())
                .setParameter("dataQualityStatus", snapshot.dataQualityStatus() == null
                        ? DataQualityStatus.INSUFFICIENT : snapshot.dataQualityStatus().name())
                .setParameter("profileVersion", UserBehaviorProfile.PROFILE_VERSION)
                .setParameter("calculationVersion", UserBehaviorProfile.CALCULATION_VERSION)
                .setParameter("calculatedAt", snapshot.calculatedAt())
                .setParameter("updatedAt", now)
                .executeUpdate();
    }

    private boolean isFresherThanExisting(ProfileSnapshot snapshot) {
        Optional<UserBehaviorProfile> existing = repository.findByUserId(snapshot.userId());
        if (existing.isEmpty()) return true;
        OffsetDateTime stored = existing.get().getCalculatedAt();
        if (stored == null) return true;
        return !snapshot.calculatedAt().isBefore(stored);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfileSnapshot> findLatestForUser(UUID userId) {
        return repository.findByUserId(userId).map(this::toSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureSeriesPoint> getSeries(UUID userId, WindowType windowType, FeatureType feature) {
        int windowSize = windowType == WindowType.WINDOW_7D ? 7 : 30;
        ZoneId userZone = userRepository.findById(userId)
                .map(user -> user.getTimezone())
                .filter(value -> value != null && !value.isBlank())
                .map(ZoneId::of)
                .orElse(ZoneId.of("UTC"));
        LocalDate targetDate = LocalDate.now(clock.withZone(userZone));
        LocalDate windowStart = targetDate.minusDays(windowSize - 1);

        List<UserDailyFeature> rows = windowRepository.findByUserAndWindow(userId, windowStart, targetDate);
        Map<LocalDate, UserDailyFeature> rowMap = rows.stream()
                .collect(Collectors.toMap(UserDailyFeature::getFeatureDate, r -> r));

        List<FeatureSeriesPoint> points = new ArrayList<>(windowSize);
        for (int i = 0; i < windowSize; i++) {
            LocalDate date = windowStart.plusDays(i);
            UserDailyFeature row = rowMap.get(date);
            if (row == null) {
                points.add(FeatureSeriesPoint.noData(date));
            } else {
                BigDecimal value = getFeatureValue(row, feature);
                if (value != null) {
                    points.add(FeatureSeriesPoint.explicit(date, value));
                } else {
                    points.add(FeatureSeriesPoint.noData(date));
                }
            }
        }
        return points;
    }

    private BigDecimal getFeatureValue(UserDailyFeature row, FeatureType feature) {
        return switch (feature) {
            case STRESS -> row.getStressScore();
            case MOOD -> row.getMoodScore();
            case ENERGY -> row.getEnergyScore();
            case SLEEP -> row.getSleepHours();
            case ANXIETY -> row.getAnxietySignal();
            case ENGAGEMENT -> row.getEngagementScore();
        };
    }

    private ProfileSnapshot toSnapshot(UserBehaviorProfile e) {
        return new ProfileSnapshot(
                e.getUserId(),
                e.getWindowEnd(),
                e.getStressAvg7d(),
                e.getStressAvg30d(),
                e.getMoodAvg7d(),
                e.getMoodAvg30d(),
                e.getEnergyAvg7d(),
                e.getEnergyAvg30d(),
                e.getSleepAvg7d(),
                e.getSleepAvg30d(),
                e.getAnxietyAvg7d(),
                e.getAnxietyAvg30d(),
                e.getEngagementScore7d(),
                e.getEngagementScore30d(),
                e.getTrendSummary(),
                deserializeTopics(e.getDominantTopics7d()),
                deserializeTopics(e.getDominantTopics30d()),
                e.getRiskLevel(),
                e.getRiskHistoryId(),
                nz(e.getDataCoverage()),
                nz(e.getConfidence()),
                e.getDataQualityStatus(),
                e.getCalculatedAt());
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private String serializeTopics(List<TopicFrequency> topics) {
        try {
            return objectMapper.writeValueAsString(topics);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("G4-T09 failed to serialize dominantTopics list", e);
        }
    }

    private List<TopicFrequency> deserializeTopics(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<TopicFrequency>>() {});
        } catch (JsonProcessingException e) {
            log.warn("G4-T09 failed to deserialize dominantTopics; returning empty", e);
            return List.of();
        }
    }

    private static UUID toUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException(
                    "Expected 16-byte UUID payload, got length=" + (bytes == null ? 0 : bytes.length));
        }
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++) { msb = (msb << 8) | (bytes[i] & 0xffL); }
        for (int i = 8; i < 16; i++) { lsb = (lsb << 8) | (bytes[i] & 0xffL); }
        return new UUID(msb, lsb);
    }
}
