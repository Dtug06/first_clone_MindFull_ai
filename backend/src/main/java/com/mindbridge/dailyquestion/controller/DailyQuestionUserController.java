package com.mindbridge.dailyquestion.controller;

import com.mindbridge.dailyquestion.dto.AnswerResponse;
import com.mindbridge.dailyquestion.dto.AssignmentResponse;
import com.mindbridge.dailyquestion.dto.CheckinHistoryResponse;
import com.mindbridge.dailyquestion.dto.SubmitAnswerRequest;
import com.mindbridge.dailyquestion.service.DailyQuestionAnswerService;
import com.mindbridge.dailyquestion.service.DailyQuestionAssignmentService;
import com.mindbridge.idempotency.service.IdempotencyService;
import com.mindbridge.idempotency.service.IdempotencyService.IdempotencyResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User-facing endpoints for daily question assignments and answers.
 *
 * All endpoints are authenticated (any authenticated user). The userId is
 * derived from the JWT principal inside the service — never accepted from the
 * request body or query string.
 *
 * G2-T08: POST /{assignmentId}/answer accepts an optional {@code Idempotency-Key}
 * header. Same key + same payload = same response (replay). Missing key = legacy
 * behavior (409 on duplicate answer, per G2-T06 §4.3).
 */
@RestController
@RequestMapping("/daily-checkins")
public class DailyQuestionUserController {

    /** Logical endpoint identifier used as the idempotency key group. */
    static final String ANSWER_ENDPOINT = "POST:/daily-checkins/{assignmentId}/answer";

    private final DailyQuestionAssignmentService assignmentService;
    private final DailyQuestionAnswerService answerService;
    private final IdempotencyService idempotencyService;

    public DailyQuestionUserController(
            DailyQuestionAssignmentService assignmentService,
            DailyQuestionAnswerService answerService,
            IdempotencyService idempotencyService) {
        this.assignmentService = assignmentService;
        this.answerService = answerService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * GET /daily-checkins/today
     * Returns today's assignments for the current user, creating them idempotently
     * if they don't yet exist.
     */
    @GetMapping("/today")
    public ResponseEntity<List<AssignmentResponse>> getToday() {
        return ResponseEntity.ok(assignmentService.getOrCreateTodayAssignments());
    }

    /**
     * POST /daily-checkins/{assignmentId}/answer
     * Submits an answer for the given assignment. The assignmentId comes from the
     * URL; the userId is resolved from the JWT principal inside the service.
     *
     * With {@code Idempotency-Key} header: same key returns the original 201
     * response even on subsequent calls (no 409). Without the header: legacy
     * behavior — 409 on duplicate (per G2-T06 §4.3).
     *
     * Returns 201 on success, 400 on validation error, 403 on ownership mismatch,
     * 404 on missing assignment, 409 on duplicate (no key).
     */
    @PostMapping("/{assignmentId}/answer")
    public ResponseEntity<AnswerResponse> submitAnswer(
            @PathVariable UUID assignmentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody SubmitAnswerRequest request) {

        UUID userId = answerService.getCurrentUserId();

        IdempotencyResult<AnswerResponse> result = idempotencyService.executeWithIdempotency(
                userId,
                ANSWER_ENDPOINT,
                idempotencyKey,
                AnswerResponse.class,
                () -> IdempotencyService.result(
                        answerService.submit(assignmentId, request),
                        HttpStatus.CREATED));

        return ResponseEntity.status(result.status()).body(result.body());
    }

    /**
     * GET /daily-checkins/history
     * Returns the user's recent check-in history (last 7 days, grouped by date,
     * descending). Date grouping uses UTC because answer rows do not store the
     * user's timezone at answer time — the assignment row still preserves it.
     */
    @GetMapping("/history")
    public ResponseEntity<List<CheckinHistoryResponse>> getHistory() {
        return ResponseEntity.ok(answerService.getRecentHistory());
    }
}
