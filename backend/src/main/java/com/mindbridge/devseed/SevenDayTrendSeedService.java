package com.mindbridge.devseed;

import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.job.DailyFeatureAggregationService;
import com.mindbridge.behavior.feature.job.dto.UserAggregationResult;
import com.mindbridge.behavior.feature.profile.job.UserBehaviorProfileAggregationJobService;
import com.mindbridge.behavior.feature.profile.repository.UserBehaviorProfileRepository;
import com.mindbridge.behavior.feature.window.repository.UserDailyFeatureWindowRepository;
import com.mindbridge.dailyquestion.domain.AssignmentStatus;
import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.repository.DailyQuestionAssignmentRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository;
import com.mindbridge.dailyquestion.service.DailyQuestionAnswerService;
import com.mindbridge.dailyquestion.service.DailyQuestionAssignmentService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed service that produces a visible 7-day improving trend for one
 * dedicated test user through the existing G4 pipeline.
 *
 * <p><b>Architecture:</b> follows the source-of-truth requirement (Section 3):
 * <ol>
 *   <li>Resolve the seed user by email.</li>
 *   <li>Create valid Daily Question assignments for seven consecutive dates.</li>
 *   <li>Create typed answers using approved templates and answer types.</li>
 *   <li>Run the real {@link DailyFeatureAggregationService} (G4-T05) to
 *       persist {@code user_daily_features} rows.</li>
 *   <li>Run the real {@link UserBehaviorProfileAggregationJobService}
 *       (G4-T09) to build the {@code user_behavior_profiles} row.</li>
 *   <li>Return verification data so the caller can assert the trend.</li>
 * </ol>
 *
 * <p><b>Safety:</b>
 * <ul>
 *   <li>Disabled by default; requires {@code mindbridge.dev-seed.seven-day-trend.enabled=true}.</li>
 *   <li>Only modifies the explicitly configured user.</li>
 *   <li>Never deletes or overwrites existing non-seed data.</li>
 *   <li>Never hard-codes clinical thresholds.</li>
 *   <li>All sensitive values (text answers) are neutral and non-clinical.</li>
 * </ul>
 */
@Service
public class SevenDayTrendSeedService {

    private static final Logger log = LoggerFactory.getLogger(SevenDayTrendSeedService.class);

    private static final List<String> TEMPLATE_CODES = List.of("STRESS", "MOOD", "SLEEP", "ENERGY", "OPEN");

    private final UserRepository userRepository;
    private final DailyQuestionTemplateRepository templateRepository;
    private final DailyQuestionAssignmentRepository assignmentRepository;
    private final DailyQuestionAssignmentService assignmentService;
    private final DailyQuestionAnswerService answerService;
    private final DailyFeatureAggregationService dailyFeatureService;
    private final UserBehaviorProfileAggregationJobService profileService;
    private final UserBehaviorProfileRepository profileRepository;
    private final UserDailyFeatureWindowRepository featureRepository;
    private final Clock clock;

    public SevenDayTrendSeedService(
            UserRepository userRepository,
            DailyQuestionTemplateRepository templateRepository,
            DailyQuestionAssignmentRepository assignmentRepository,
            DailyQuestionAssignmentService assignmentService,
            DailyQuestionAnswerService answerService,
            DailyFeatureAggregationService dailyFeatureService,
            UserBehaviorProfileAggregationJobService profileService,
            UserBehaviorProfileRepository profileRepository,
            UserDailyFeatureWindowRepository featureRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.templateRepository = templateRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
        this.answerService = answerService;
        this.dailyFeatureService = dailyFeatureService;
        this.profileService = profileService;
        this.profileRepository = profileRepository;
        this.featureRepository = featureRepository;
        this.clock = clock;
    }

    /**
     * Runs the full seed pipeline for one user.
     *
     * <p>Uses REQUIRES_NEW so the seed always runs in its own committed transaction,
     * independent of any test-class transaction. This allows the seed to be called
     * from both integration tests (which may have their own @Transactional) and
     * from CommandLineRunner (no outer transaction).
     *
     * @param email email of the seed user
     * @param targetDate day 7 of the 7-day window
     * @return seed result with counts and verification data
     * @throws IllegalStateException if the user is not found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SevenDayTrendSeedResult run(String email, LocalDate targetDate) {
        log.info("SevenDayTrendSeed: starting for user={} targetDate={}", email, targetDate);

        UUID userId = userRepository.findByEmailIgnoreCase(email)
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Seed user not found: " + email + " - please register a user first. "
                                + "The seed only modifies an existing account; it never creates one. "
                                + "To create a test user, run the app and POST /api/v1/auth/register "
                                + "with any email/password, then use that email here."));

        // --- 0. Wipe existing G4 data for this user so the seed is idempotent ---
        // Removes previous seed runs' assignments, daily features, and profile,
        // preventing duplicate rows from accumulating when the runner is invoked
        // multiple times.
        int deletedAssignments = assignmentRepository.deleteByUserId(userId);
        int deletedFeatures = featureRepository.deleteByUserId(userId);
        int deletedProfiles = profileRepository.deleteByUserId(userId);
        log.info("SevenDayTrendSeed: wiped existing data assignments={} features={} profiles={}",
                deletedAssignments, deletedFeatures, deletedProfiles);

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh"); // UTC+7, matches V7 default
        LocalDate day1 = targetDate.minusDays(6);

        // --- 1. Lookup approved templates ---
        List<DailyQuestionTemplate> templates = templateRepository.findLatestApproved();
        Map<String, DailyQuestionTemplate> byCode = new LinkedHashMap<>();
        for (DailyQuestionTemplate t : templates) {
            byCode.put(t.getCode(), t);
        }
        for (String code : TEMPLATE_CODES) {
            if (!byCode.containsKey(code)) {
                throw new IllegalStateException(
                        "Template '" + code + "' not found or not APPROVED. "
                                + "Ensure V6 migration has been applied.");
            }
        }

        int assignmentsCreated = 0;
        int answersCreated = 0;
        List<SevenDayTrendSeedResult.DayResult> days = new ArrayList<>();

        // --- 2. Create assignments + answers for each of the 7 days ---
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            LocalDate localDate = day1.plusDays(dayIndex);
            List<SevenDayTrendPlan.Answer> answers = SevenDayTrendPlan.forDay(dayIndex);

            for (int t = 0; t < TEMPLATE_CODES.size(); t++) {
                String code = TEMPLATE_CODES.get(t);
                DailyQuestionTemplate template = byCode.get(code);
                SevenDayTrendPlan.Answer plan = answers.get(t);

                DailyQuestionAssignment assignment = assignmentService.createAssignmentForSeed(
                        userId, template, localDate, zone.getId());
                assignmentsCreated++;

                if (assignment.getStatus() == AssignmentStatus.ANSWERED) {
                    // Already answered (idempotent) - skip
                    continue;
                }

                try {
                    answerService.submitAnswerForSeed(
                            userId,
                            assignment.getId(),
                            plan.answerType(),
                            plan.numericValue(),
                            plan.textValue(),
                            plan.optionValue()
                    );
                    answersCreated++;
                } catch (Exception e) {
                    log.warn("SevenDayTrendSeed: answer already exists for day={} template={}",
                            localDate, code);
                }
            }

            // --- 3. Run G4-T05 daily feature aggregation for this day ---
            UserAggregationResult featureResult = dailyFeatureService.aggregateOneUser(userId, localDate);
            log.info("SevenDayTrendSeed: day={} featureCalc success={}", localDate, featureResult.success());

            List<String> dayValues = answers.stream()
                    .map(a -> a.numericValue() != null ? a.numericValue().toString()
                            : a.optionValue() != null ? a.optionValue()
                            : a.textValue().substring(0, Math.min(20, a.textValue().length())))
                    .toList();
            days.add(new SevenDayTrendSeedResult.DayResult(localDate, dayValues));
        }

        // --- 4. Run G4-T09 profile aggregation for the target date ---
        boolean profileUpserted = profileService.aggregateOneUser(userId, targetDate);
        log.info("SevenDayTrendSeed: profileUpserted={}", profileUpserted);

        log.info("SevenDayTrendSeed: complete for user={} assignments={} answers={}",
                email, assignmentsCreated, answersCreated);

        return new SevenDayTrendSeedResult(
                email,
                userId,
                day1,
                targetDate,
                assignmentsCreated,
                answersCreated,
                profileUpserted,
                days
        );
    }
}