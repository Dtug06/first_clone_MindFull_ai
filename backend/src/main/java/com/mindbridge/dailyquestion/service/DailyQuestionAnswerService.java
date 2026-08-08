package com.mindbridge.dailyquestion.service;

import com.mindbridge.behavior.domain.BehavioralEventType;
import com.mindbridge.behavior.domain.SourceType;
import com.mindbridge.behavior.service.BehavioralEventService;
import com.mindbridge.behavior.feature.profile.service.OnDemandAggregationTrigger;
import com.mindbridge.common.exception.AccessDeniedException;
import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.common.service.CurrentUserService;
import com.mindbridge.dailyquestion.domain.AnswerType;
import com.mindbridge.dailyquestion.domain.AssignmentStatus;
import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import com.mindbridge.dailyquestion.domain.DailyQuestionOption;
import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.domain.QuestionType;
import com.mindbridge.dailyquestion.dto.AnswerResponse;
import com.mindbridge.dailyquestion.dto.CheckinHistoryResponse;
import com.mindbridge.dailyquestion.dto.SubmitAnswerRequest;
import com.mindbridge.dailyquestion.exception.AnswerBusinessException;
import com.mindbridge.dailyquestion.repository.DailyQuestionAnswerRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionAssignmentRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionOptionRepository;
import com.mindbridge.devseed.SeedGuard;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Handles submission and history of daily question answers (G2-T06).
 *
 * Validation pipeline (in order):
 * 1. Assignment exists — 404 CHECKIN_ASSIGNMENT_NOT_FOUND if not.
 * 2. Ownership — assignment.userId == current principal — 403 ACCESS_DENIED.
 * 3. No existing answer — 409 CHECKIN_ANSWER_DUPLICATE if already answered.
 *    DB UNIQUE on assignment_id is the last-line safety net.
 * 4. Exactly-one value — request has exactly one of numeric/text/option set,
 *    matching answerType — 400 VALIDATION_ERROR.
 * 5. answerType ↔ template.questionType mapping — 400 if mismatched.
 * 6. OPTION ownership — optionValue must belong to the template's options — 400.
 * 7. NUMERIC range — within [scale_min, scale_max] if template has bounds — 400.
 * 8. TEXT length — max 5000 chars — 400.
 *
 * The service is @Transactional but the body is short and contains no
 * external calls (no LLM, no scheduler), so transaction duration is bounded.
 */
@Service
public class DailyQuestionAnswerService {

    private static final Logger log = LoggerFactory.getLogger(DailyQuestionAnswerService.class);

    /** Maximum length of a free-text answer. Conservative MVP cap to keep JSONB small. */
    static final int MAX_TEXT_LENGTH = 5000;

    /** Default window for history endpoint — last N days including today. */
    static final int DEFAULT_HISTORY_DAYS = 7;

    private final DailyQuestionAnswerRepository answerRepository;
    private final DailyQuestionAssignmentRepository assignmentRepository;
    private final DailyQuestionOptionRepository optionRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;
    private final BehavioralEventService behavioralEventService;
    private final SeedGuard seedGuard;
    private final OnDemandAggregationTrigger onDemandAggregationTrigger;

    public DailyQuestionAnswerService(
            DailyQuestionAnswerRepository answerRepository,
            DailyQuestionAssignmentRepository assignmentRepository,
            DailyQuestionOptionRepository optionRepository,
            CurrentUserService currentUserService,
            Clock clock,
            BehavioralEventService behavioralEventService,
            SeedGuard seedGuard,
            OnDemandAggregationTrigger onDemandAggregationTrigger) {
        this.answerRepository = answerRepository;
        this.assignmentRepository = assignmentRepository;
        this.optionRepository = optionRepository;
        this.currentUserService = currentUserService;
        this.clock = clock;
        this.behavioralEventService = behavioralEventService;
        this.seedGuard = seedGuard;
        this.onDemandAggregationTrigger = onDemandAggregationTrigger;
    }

    // --- Submit ---

    /**
     * Submits an answer for the given assignment. The assignment id is taken
     * from the URL path; the user id is taken from the JWT principal.
     *
     * @throws ResourceNotFoundException  if the assignment does not exist (404)
     * @throws AccessDeniedException       if the assignment belongs to another user (403)
     * @throws AnswerBusinessException     if validation fails or duplicate (400 / 409)
     */
    @Transactional
    public AnswerResponse submit(UUID assignmentId, SubmitAnswerRequest request) {
        UUID userId = currentUserService.getCurrentUserId();

        DailyQuestionAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyQuestionAssignment", assignmentId.toString()));

        if (!assignment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Assignment does not belong to current user");
        }

        if (assignment.getStatus() == AssignmentStatus.ANSWERED
                || answerRepository.existsByAssignmentId(assignmentId)) {
            throw new AnswerBusinessException(ErrorCode.CHECKIN_ANSWER_DUPLICATE,
                    "Answer already submitted for this assignment today");
        }

        DailyQuestionTemplate template = assignment.getTemplateVersion();
        AnswerType answerType = request.answerType();
        validateTypeMatchesTemplate(answerType, template.getQuestionType());
        validateExactlyOneValue(request);

        DailyQuestionAnswer answer = switch (answerType) {
            case NUMERIC -> {
                BigDecimal v = requireNumericValue(request);
                validateNumericRange(v, template);
                yield DailyQuestionAnswer.createNumeric(assignment, v);
            }
            case TEXT -> {
                String v = requireTextValue(request);
                if (v.length() > MAX_TEXT_LENGTH) {
                    throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                            "textValue exceeds maximum length of " + MAX_TEXT_LENGTH + " characters");
                }
                yield DailyQuestionAnswer.createText(assignment, v);
            }
            case OPTION -> {
                String v = requireOptionValue(request);
                validateOptionBelongsToTemplate(v, template);
                yield DailyQuestionAnswer.createOption(assignment, v);
            }
        };

        DailyQuestionAnswer saved;
        try {
            saved = answerRepository.saveAndFlush(answer);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE (assignment_id) tripped — concurrent submission
            throw new AnswerBusinessException(ErrorCode.CHECKIN_ANSWER_DUPLICATE,
                    "Answer already submitted for this assignment today");
        }

        assignment.markAnswered();
        assignmentRepository.save(assignment);

        // G2-T07: emit DAILY_CHECKIN_COMPLETED event. Properties NEVER include
        // raw answer values — only metadata (see G2-T07 plan §2.3).
        // source_id = assignment.id (not answer.id) per plan §2.6: assignment
        // is the stable anchor for daily-check-in, answer is the action.
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("assignment_id", assignment.getId().toString());
        props.put("answer_type", answerType.name());
        props.put("template_code", assignment.getTemplateCode());
        behavioralEventService.record(
                userId,
                BehavioralEventType.DAILY_CHECKIN_COMPLETED,
                SourceType.DAILY_QUESTION_ANSWER,
                assignment.getId(),
                props);

        registerAggregationAfterCommit(userId, assignment.getAssignedForDate());

        return AnswerResponse.from(saved);
    }

    /** G4-T14: aggregate only after the answer transaction commits successfully. */
    private void registerAggregationAfterCommit(UUID userId, LocalDate assignedForDate) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("G4 on-demand aggregation hook was not registered because transaction synchronization is inactive: userId={} date={}",
                    userId, assignedForDate);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    onDemandAggregationTrigger.triggerForUserAndDate(userId, assignedForDate);
                } catch (Exception e) {
                    // The facade is fail-soft; this final guard protects the saved answer
                    // even if a future implementation accidentally violates that contract.
                    log.warn("G4 after-commit aggregation callback failed: userId={} date={}",
                            userId, assignedForDate, e);
                }
            }
        });
    }

    /**
     * SEED-ONLY entry point (G2-T09). Submits an answer for the given
     * assignment on behalf of the given user id. Used by the deterministic
     * dev seed in {@code com.mindbridge.devseed.DevSeedService}. Not for
     * production code paths — production code must use
     * {@link #submit(UUID, SubmitAnswerRequest)} which derives the userId
     * from the JWT principal via {@code CurrentUserService}.
     *
     * <p>Public only because Java has no package-friend mechanism across
     * packages; the {@code *ForSeed} suffix and explicit javadoc mark its
     * purpose. The {@code @ConditionalOnProperty} gate on
     * {@code DevSeedRunner} ensures the seed code path is opt-in.
     */
    public DailyQuestionAnswer submitAnswerForSeed(UUID userId,
                                                   UUID assignmentId,
                                                   AnswerType answerType,
                                                   BigDecimal numericValue,
                                                   String textValue,
                                                   String optionValue) {
        seedGuard.requireSeedAllowed();
        DailyQuestionAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DailyQuestionAssignment", assignmentId.toString()));

        if (!assignment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Assignment does not belong to seed user");
        }

        if (assignment.getStatus() == AssignmentStatus.ANSWERED
                || answerRepository.existsByAssignmentId(assignmentId)) {
            throw new AnswerBusinessException(ErrorCode.CHECKIN_ANSWER_DUPLICATE,
                    "Answer already submitted for this assignment");
        }

        DailyQuestionTemplate template = assignment.getTemplateVersion();

        // Synthesize a minimal SubmitAnswerRequest so the existing validation
        // helpers (type matching, exactly-one-value, range, option ownership,
        // text length) run unchanged.
        SubmitAnswerRequest synthetic = new SubmitAnswerRequest(
                answerType, numericValue, textValue, optionValue);

        validateTypeMatchesTemplate(answerType, template.getQuestionType());
        validateExactlyOneValue(synthetic);

        DailyQuestionAnswer answer = switch (answerType) {
            case NUMERIC -> DailyQuestionAnswer.createNumeric(assignment, numericValue);
            case TEXT -> DailyQuestionAnswer.createText(assignment, textValue);
            case OPTION -> DailyQuestionAnswer.createOption(assignment, optionValue);
        };

        DailyQuestionAnswer saved;
        try {
            saved = answerRepository.saveAndFlush(answer);
        } catch (DataIntegrityViolationException e) {
            throw new AnswerBusinessException(ErrorCode.CHECKIN_ANSWER_DUPLICATE,
                    "Answer already submitted for this assignment");
        }

        assignment.markAnswered();
        assignmentRepository.save(assignment);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("assignment_id", assignment.getId().toString());
        props.put("answer_type", answerType.name());
        props.put("template_code", assignment.getTemplateCode());
        behavioralEventService.record(
                userId,
                BehavioralEventType.DAILY_CHECKIN_COMPLETED,
                SourceType.DAILY_QUESTION_ANSWER,
                assignment.getId(),
                props);

        return saved;
    }

    // --- History ---

    /**
     * Returns the user's check-in history grouped by local date (in the user's
     * stored timezone), descending. Window is the last 7 days inclusive of today.
     */
    @Transactional
    public List<CheckinHistoryResponse> getRecentHistory() {
        UUID userId = currentUserService.getCurrentUserId();

        ZoneId utc = ZoneId.of("UTC");
        Instant now = clock.instant();
        Instant from = now.atZone(utc).toLocalDate()
                .minusDays(DEFAULT_HISTORY_DAYS - 1L)
                .atStartOfDay(utc)
                .toInstant();
        Instant to = now;

        List<DailyQuestionAnswer> answers = answerRepository
                .findByUserIdAndAnsweredAtBetweenOrderByAnsweredAtDesc(userId, from, to);

        // Group by answered_at date in UTC (no timezone column on answers — see §2.4
        // follow-up: if user changes TZ mid-window, history grouping uses UTC date.
        // Acceptable for MVP because answers store answered_at in UTC and the
        // assignment's assignedForDate + timezone are still preserved on the
        // assignment row itself.)
        Map<LocalDate, List<DailyQuestionAnswer>> byDate = answers.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAnsweredAt().atZone(utc).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<CheckinHistoryResponse> result = new ArrayList<>(byDate.size());
        for (Map.Entry<LocalDate, List<DailyQuestionAnswer>> entry : byDate.entrySet()) {
            List<AnswerResponse> items = entry.getValue().stream()
                    .sorted(Comparator.comparing(DailyQuestionAnswer::getAnsweredAt))
                    .map(AnswerResponse::from)
                    .toList();
            result.add(new CheckinHistoryResponse(entry.getKey(), "UTC", items));
        }
        return result;
    }

    /**
     * Returns the current authenticated user id. Used by callers (e.g. the
     * IdempotencyService replay path) that need userId without invoking the
     * full business logic.
     */
    public UUID getCurrentUserId() {
        return currentUserService.getCurrentUserId();
    }

    // --- Validation helpers ---

    private void validateTypeMatchesTemplate(AnswerType answerType, QuestionType questionType) {
        boolean ok = switch (questionType) {
            case SCALE, NUMBER -> answerType == AnswerType.NUMERIC;
            case SINGLE_CHOICE -> answerType == AnswerType.OPTION;
            case TEXT -> answerType == AnswerType.TEXT;
        };
        if (!ok) {
            throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                    "answerType " + answerType + " does not match question type " + questionType);
        }
    }

    private void validateExactlyOneValue(SubmitAnswerRequest request) {
        boolean numeric = request.numericValue() != null;
        boolean text = request.textValue() != null && !request.textValue().isBlank();
        boolean option = request.optionValue() != null && !request.optionValue().isBlank();

        int count = (numeric ? 1 : 0) + (text ? 1 : 0) + (option ? 1 : 0);
        if (count != 1) {
            throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                    "Exactly one of numericValue/textValue/optionValue must be set (got " + count + ")");
        }

        boolean typeOk = switch (request.answerType()) {
            case NUMERIC -> numeric && !text && !option;
            case TEXT -> text && !numeric && !option;
            case OPTION -> option && !numeric && !text;
        };
        if (!typeOk) {
            throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                    "Value type does not match answerType " + request.answerType());
        }
    }

    private BigDecimal requireNumericValue(SubmitAnswerRequest request) {
        if (request.numericValue() == null) {
            throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                    "numericValue is required for NUMERIC answer");
        }
        return request.numericValue();
    }

    private String requireTextValue(SubmitAnswerRequest request) {
        if (request.textValue() == null || request.textValue().isBlank()) {
            throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                    "textValue is required for TEXT answer");
        }
        return request.textValue();
    }

    private String requireOptionValue(SubmitAnswerRequest request) {
        if (request.optionValue() == null || request.optionValue().isBlank()) {
            throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                    "optionValue is required for OPTION answer");
        }
        return request.optionValue();
    }

    private void validateNumericRange(BigDecimal value, DailyQuestionTemplate template) {
        BigDecimal min = template.getScaleMin();
        BigDecimal max = template.getScaleMax();
        if (min != null && value.compareTo(min) < 0) {
            throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                    "numericValue " + value + " is below scale_min " + min);
        }
        if (max != null && value.compareTo(max) > 0) {
            throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                    "numericValue " + value + " exceeds scale_max " + max);
        }
    }

    private void validateOptionBelongsToTemplate(String optionValue, DailyQuestionTemplate template) {
        List<DailyQuestionOption> options = optionRepository.findByTemplateIdOrderByOrderIndexAsc(
                template.getId());
        Set<String> allowed = options.stream()
                .map(DailyQuestionOption::getOptionValue)
                .collect(Collectors.toSet());
        if (!allowed.contains(optionValue)) {
            throw new AnswerBusinessException(ErrorCode.VALIDATION_ERROR,
                    "optionValue '" + optionValue + "' does not belong to this template's options");
        }
    }
}
