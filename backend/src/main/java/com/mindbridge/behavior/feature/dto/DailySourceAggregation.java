package com.mindbridge.behavior.feature.dto;

import com.mindbridge.analysis.result.domain.ChatAnalysisResult;
import com.mindbridge.dailyquestion.domain.AnswerType;
import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * G4-T03: Immutable snapshot of raw inputs for one (user, local_date, timezone).
 *
 * <p>This DTO carries only what the daily calculator (G4-T04) needs to produce
 * the 8 catalog feature scores. It deliberately does NOT compute any score
 * itself - that responsibility lives in DailyFeatureCalculatorService
 * (G4-T04) so that all magic-number / threshold / weight decisions stay in
 * one place and can be revised together.
 *
 * <p>Missing data is represented as null (NOT 0 or empty string). FEATURE_DICTIONARY
 * Section 4.2 mandates this: "NEVER convert null to 0".
 *
 * <p>The DTO is a Java record because every field is computed at
 * construction time and never mutated. The nested records follow the same rule.
 */
public record DailySourceAggregation(
        UUID userId,
        String timezone,
        LocalDate localDate,

        /** UTC instant corresponding to localDate 00:00 in timezone. */
        OffsetDateTime windowStartUtc,

        /** UTC instant corresponding to (localDate + 1) 00:00 in timezone (exclusive). */
        OffsetDateTime windowEndUtc,

        /** Explicit answers (Daily Answer source) - one element per answered assignment on local_date. */
        List<ExplicitAnswer> explicitAnswers,

        /** ACTIVE chat_analysis_results in [windowStartUtc, windowEndUtc). Rerun-aware. */
        List<EffectiveChatAnalysis> effectiveChatAnalyses,

        /** Behavioral event counts for engagement features. */
        BehavioralEventCounts behavioralCounts,

        /** Whether CBT runtime is shipped + whether this (user, day) has CBT activity. */
        CbtAvailability cbtAvailability,

        /** Empty when cbtAvailability != COMPUTABLE (MVP returns empty counts). */
        CbtAggregation cbtActivity) {

    /**
     * Explicit-answer view: one answered daily_question_answers row joined
     * to its daily_question_assignments row.
     */
    public record ExplicitAnswer(
            UUID assignmentId,
            String templateCode,
            String questionType,
            AnswerType answerType,
            BigDecimal numericValue,
            String textValue,
            String optionValue,
            String timezone,
            LocalDate assignedForDate,
            Instant answeredAt) {

        public static ExplicitAnswer of(DailyQuestionAnswer answer, DailyQuestionAssignment assignment) {
            return new ExplicitAnswer(
                    assignment.getId(),
                    assignment.getTemplateCode(),
                    assignment.getTemplateVersion() == null
                            ? null
                            : assignment.getTemplateVersion().getQuestionType() == null
                                    ? null
                                    : assignment.getTemplateVersion().getQuestionType().name(),
                    answer.getAnswerType(),
                    answer.getNumericValue(),
                    answer.getTextValue(),
                    answer.getOptionValue(),
                    assignment.getTimezone(),
                    assignment.getAssignedForDate(),
                    answer.getAnsweredAt());
        }
    }

    /**
     * Effective (ACTIVE-only) chat-analysis view for one ACTIVE row.
     * JSONB/array columns (signals, evidence_spans) are intentionally NOT
     * carried here - G4-T04 will parse them if the anxiety_signal calculator needs them.
     */
    public record EffectiveChatAnalysis(
            UUID analysisResultId,
            UUID conversationMessageId,
            Instant createdAt,
            String topic,
            String emotion,
            String intent,
            Short modelRiskLevel,
            BigDecimal confidence) {

        public static EffectiveChatAnalysis of(ChatAnalysisResult row) {
            return new EffectiveChatAnalysis(
                    row.getId(),
                    row.getConversationMessageId(),
                    row.getCreatedAt().toInstant(),
                    row.getTopic(),
                    row.getEmotion(),
                    row.getIntent(),
                    row.getModelRiskLevel(),
                    row.getConfidence());
        }
    }

    /**
     * Aggregate counts of behavioral events for one (user, day window).
     * <pre>
     *   message_count              = COUNT(CHAT_MESSAGE_SENT)
     *   active_chat_session_count  = COUNT DISTINCT source_id of CHAT_SESSION_STARTED
     *   checkin_completed_count    = COUNT(DAILY_CHECKIN_COMPLETED)
     *   checkin_skipped_count      = COUNT(DAILY_CHECKIN_SKIPPED)
     *   checkin_assigned_count     = COUNT(daily_question_assignments for (user, localDate))
     * </pre>
     */
    public record BehavioralEventCounts(
            long chatMessageCount,
            long activeChatSessionCount,
            long checkinCompletedCount,
            long checkinSkippedCount,
            long checkinAssignedCount) {

        public static BehavioralEventCounts empty() {
            return new BehavioralEventCounts(0L, 0L, 0L, 0L, 0L);
        }
    }

    /**
     * CBT aggregation. Empty when CBT is not shipped (MVP) or when the
     * user has no CBT activity for the day.
     */
    public record CbtAggregation(
            long assignmentsCount,
            long startedCount,
            long completedCount,
            long skippedCount) {

        public static CbtAggregation empty() {
            return new CbtAggregation(0L, 0L, 0L, 0L);
        }
    }
}