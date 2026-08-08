package com.mindbridge.dailyquestion.service;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.common.service.CurrentUserService;
import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.domain.TemplateStatus;
import com.mindbridge.dailyquestion.dto.AssignmentResponse;
import com.mindbridge.dailyquestion.repository.DailyQuestionAssignmentRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository;
import com.mindbridge.devseed.SeedGuard;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Manages daily question assignments for users.
 *
 * Lifecycle (G2-T05, lazy creation):
 * 1. User calls GET /daily-checkins/today.
 * 2. Service computes the user's local date from their timezone.
 * 3. If assignments already exist for that user/date, return them unchanged.
 * 4. Otherwise, create one assignment per latest-APPROVED template version.
 *
 * Versioning: each assignment pins the exact template version assigned at that
 * moment. If admin publishes a newer template version later the same day, the
 * existing assignment keeps pointing to the original version — historical
 * consistency is preserved.
 *
 * Self-imposed contract: the service never touches the assignment column after
 * creation except for status transitions (ANSWERED / SKIPPED), which are owned
 * by future tasks (G2-T06 for answer submission).
 *
 * Security:
 * - The userId always comes from the JWT principal (CurrentUserService).
 * - The client's timezone is not trusted — the user's stored timezone from the
 *   users table is used. (Future task G2-T07+ may expose a profile update
 *   endpoint to change the stored timezone.)
 */
@Service
public class DailyQuestionAssignmentService {

    private final DailyQuestionAssignmentRepository assignmentRepository;
    private final DailyQuestionTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;
    private final SeedGuard seedGuard;

    public DailyQuestionAssignmentService(
            DailyQuestionAssignmentRepository assignmentRepository,
            DailyQuestionTemplateRepository templateRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            Clock clock,
            SeedGuard seedGuard) {
        this.assignmentRepository = assignmentRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.clock = clock;
        this.seedGuard = seedGuard;
    }

    /**
     * Returns today's assignments for the current user, creating them if needed.
     *
     * Idempotent: calling this method multiple times for the same user on the same
     * local date returns the same set of assignments and does not create duplicates.
     */
    @Transactional
    public List<AssignmentResponse> getOrCreateTodayAssignments() {
        UUID userId = currentUserService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        String timezone = user.getTimezone();
        LocalDate localDate = todayInTimezone(timezone);

        List<DailyQuestionAssignment> existing =
                assignmentRepository.findByUserIdAndAssignedForDateOrderByTemplateCodeAsc(
                        userId, localDate);

        if (!existing.isEmpty()) {
            return existing.stream()
                    .map(AssignmentResponse::from)
                    .toList();
        }

        List<DailyQuestionTemplate> latestApproved = templateRepository.findLatestApproved();
        if (latestApproved.isEmpty()) {
            return List.of();
        }

        List<DailyQuestionAssignment> created = new ArrayList<>(latestApproved.size());
        for (DailyQuestionTemplate template : latestApproved) {
            DailyQuestionAssignment assignment = DailyQuestionAssignment.create(
                    userId, template, localDate, timezone);
            created.add(assignmentRepository.save(assignment));
        }

        created.sort(Comparator.comparing(DailyQuestionAssignment::getTemplateCode));
        return created.stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    /**
     * SEED-ONLY entry point (G2-T09). Creates an assignment for a past or
     * current local date. Used by the deterministic dev seed in
     * {@code com.mindbridge.devseed.DevSeedService}. Not for production
     * code paths — production code must use
     * {@link #getOrCreateTodayAssignments()} which derives the userId from
     * the JWT principal.
     *
     * <p>This method is public only because Java has no package-friend
     * mechanism across packages; the {@code *ForSeed} suffix and explicit
     * javadoc mark its purpose. The
     * {@code @ConditionalOnProperty} gate on {@code DevSeedRunner} ensures
     * the seed code path is opt-in.
     */
    public DailyQuestionAssignment createAssignmentForSeed(UUID userId,
                                                          DailyQuestionTemplate template,
                                                          LocalDate date,
                                                          String timezone) {
        seedGuard.requireSeedAllowed();
        return getOrCreateAssignmentForSeed(userId, template, date, timezone);
    }

    /**
     * Idempotent entry point for seed code. Returns the existing assignment if
     * one already exists for (user, templateVersion, date); otherwise creates and
     * saves a new one.  The {@code save()} call is kept inside this method so
     * the caller does not need to know whether a create or lookup happened.
     */
    private DailyQuestionAssignment getOrCreateAssignmentForSeed(UUID userId,
                                                                 DailyQuestionTemplate template,
                                                                 LocalDate date,
                                                                 String timezone) {
        List<DailyQuestionAssignment> existing = assignmentRepository
                .findByUserIdAndAssignedForDateOrderByTemplateCodeAsc(userId, date);
        for (DailyQuestionAssignment a : existing) {
            if (a.getTemplateVersion().getId().equals(template.getId())) {
                return a;
            }
        }
        DailyQuestionAssignment assignment = DailyQuestionAssignment.create(
                userId, template, date, timezone);
        return assignmentRepository.save(assignment);
    }

    /**
     * Computes today's local date in the given IANA timezone.
     * Falls back to UTC if the timezone is invalid (defensive — server should
     * never store a malformed timezone because the column accepts any VARCHAR(50)).
     */
    private LocalDate todayInTimezone(String timezone) {
        try {
            ZoneId zone = ZoneId.of(timezone);
            return LocalDate.now(clock.withZone(zone));
        } catch (Exception e) {
            return LocalDate.now(clock.withZone(ZoneId.of("UTC")));
        }
    }

    /**
     * Internal helper for tests / future scheduler tasks. Returns the local date
     * for the given user at the given instant.
     */
    LocalDate localDateForUser(User user, Instant now) {
        try {
            ZoneId zone = ZoneId.of(user.getTimezone());
            return now.atZone(zone).toLocalDate();
        } catch (Exception e) {
            return now.atZone(ZoneId.of("UTC")).toLocalDate();
        }
    }

    /**
     * Internal helper exposed for the template-status check (unit tests verify
     * that only APPROVED templates are assigned).
     */
    boolean isAssignable(DailyQuestionTemplate template) {
        return template.getStatus() == TemplateStatus.APPROVED;
    }
}
