package com.mindbridge.chat.personalization;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.profile.dto.TrendEntryResponse;
import com.mindbridge.behavior.feature.profile.dto.UserBehaviorProfileResponse;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileResponseMapper;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileService;
import com.mindbridge.chat.personalization.PersonalizationContext.BehaviorProfileObservation;
import com.mindbridge.chat.personalization.PersonalizationContext.DailyObservation;
import com.mindbridge.chat.personalization.PersonalizationContext.TrendObservation;
import com.mindbridge.consent.service.ConsentGuard;
import com.mindbridge.dailyquestion.domain.AnswerType;
import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import com.mindbridge.dailyquestion.repository.DailyQuestionAnswerRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads a minimum-data personalization snapshot before the external AI call. */
@Service
public class PersonalizationContextService {

    private static final Set<String> ALLOWED_DAILY_CODES =
            Set.of("STRESS", "MOOD", "ENERGY", "SLEEP");

    private final ConsentGuard consentGuard;
    private final UserRepository userRepository;
    private final DailyQuestionAnswerRepository answerRepository;
    private final UserBehaviorProfileService profileService;
    private final UserBehaviorProfileResponseMapper profileMapper;
    private final Clock clock;

    public PersonalizationContextService(
            ConsentGuard consentGuard,
            UserRepository userRepository,
            DailyQuestionAnswerRepository answerRepository,
            UserBehaviorProfileService profileService,
            UserBehaviorProfileResponseMapper profileMapper,
            Clock clock) {
        this.consentGuard = consentGuard;
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
        this.profileService = profileService;
        this.profileMapper = profileMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PersonalizationContext load(UUID userId) {
        if (userId == null || !consentGuard.hasPersonalizationConsent(userId)) {
            return PersonalizationContext.empty();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != User.UserStatus.ACTIVE) {
            return PersonalizationContext.empty();
        }

        ZoneId zoneId = safeZoneId(user.getTimezone());
        LocalDate contextDate = LocalDate.now(clock.withZone(zoneId));
        List<DailyObservation> daily = answerRepository
                .findWithAssignmentByUserIdAndAssignedForDate(userId, contextDate)
                .stream()
                .filter(this::isAllowedTypedAnswer)
                .map(this::toDailyObservation)
                .toList();

        BehaviorProfileObservation profile = profileService.findLatestForUser(userId)
                .map(profileMapper::toResponse)
                .map(this::toProfileObservation)
                .orElse(null);

        return new PersonalizationContext(
                sanitizeDisplayName(user.getDisplayName()),
                contextDate,
                daily,
                profile);
    }

    private boolean isAllowedTypedAnswer(DailyQuestionAnswer answer) {
        String code = answer.getAssignment().getTemplateCode();
        return ALLOWED_DAILY_CODES.contains(code)
                && answer.getAnswerType() != AnswerType.TEXT;
    }

    private DailyObservation toDailyObservation(DailyQuestionAnswer answer) {
        return new DailyObservation(
                answer.getAssignment().getTemplateCode(),
                answer.getNumericValue(),
                answer.getOptionValue());
    }

    private BehaviorProfileObservation toProfileObservation(UserBehaviorProfileResponse profile) {
        List<String> topics = profile.dominantTopics7d().stream()
                .limit(3)
                .map(topic -> topic.topic())
                .toList();
        List<TrendObservation> trends = profile.trendSummary().entries().stream()
                .filter(this::isUsableTrend)
                .limit(6)
                .map(entry -> new TrendObservation(
                        entry.featureCode(),
                        entry.direction().name(),
                        entry.recentAvg(),
                        entry.priorAvg(),
                        entry.recentCoverage()))
                .toList();
        return new BehaviorProfileObservation(
                profile.windowEnd(),
                profile.dataQualityStatus().name(),
                profile.dataCoverage(),
                profile.confidence(),
                profile.stressAvg7d(),
                profile.stressAvg30d(),
                profile.moodAvg7d(),
                profile.moodAvg30d(),
                profile.energyAvg7d(),
                profile.energyAvg30d(),
                profile.sleepAvg7d(),
                profile.sleepAvg30d(),
                topics,
                trends);
    }

    private boolean isUsableTrend(TrendEntryResponse entry) {
        return entry != null
                && entry.direction() != null
                && !"UNKNOWN".equals(entry.direction().name());
    }

    private ZoneId safeZoneId(String value) {
        try {
            return value == null || value.isBlank() ? ZoneId.of("UTC") : ZoneId.of(value);
        } catch (DateTimeException ignored) {
            return ZoneId.of("UTC");
        }
    }

    private String sanitizeDisplayName(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replaceAll("[\\p{Cntrl}]", " ").trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        return sanitized.length() <= 100 ? sanitized : sanitized.substring(0, 100);
    }
}
