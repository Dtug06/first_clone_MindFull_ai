package com.mindbridge.dailyquestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the {@code daily_question_answers} table.
 *
 * Per G2-T06 §2.3 (plan option A) — immutable once persisted:
 * - UNIQUE (assignment_id) at the DB layer prevents a second answer row.
 * - The service catches the constraint violation and surfaces 409 Conflict
 *   if the user tries to submit again.
 *
 * Value storage (per docs/02_DATABASE_MVP.md §4.6):
 * - numeric_value: NUMERIC, used when answerType = NUMERIC (SCALE / NUMBER)
 * - text_value:    TEXT,    used when answerType = TEXT
 * - option_value:  VARCHAR(50), used when answerType = OPTION (SINGLE_CHOICE)
 *                  Stores the option's value string (e.g. "1", "2"), NOT a FK
 *                  to daily_question_options.id — keeps answers decoupled from
 *                  option PKs across template versions.
 *
 * The DB-level CHECK constraint enforces exactly-one-value-per-answer_type
 * as a safety net. The service layer validates the same rule with friendlier
 * error messages.
 *
 * user_id is denormalized from the assignment for two reasons:
 * 1. Avoids a JOIN when listing user history.
 * 2. Defense-in-depth ownership check: even if the assignment FK were wrong,
 *    user_id on the answer row still matches the principal.
 */
@Entity
@Table(name = "daily_question_answers")
public class DailyQuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private DailyQuestionAssignment assignment;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false, length = 20)
    private AnswerType answerType;

    @Column(name = "numeric_value")
    private BigDecimal numericValue;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @Column(name = "option_value", length = 50)
    private String optionValue;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    protected DailyQuestionAnswer() {
    }

    private DailyQuestionAnswer(DailyQuestionAssignment assignment, UUID userId,
                                AnswerType answerType, BigDecimal numericValue,
                                String textValue, String optionValue,
                                Instant answeredAt) {
        this.assignment = assignment;
        this.userId = userId;
        this.answerType = answerType;
        this.numericValue = numericValue;
        this.textValue = textValue;
        this.optionValue = optionValue;
        this.answeredAt = answeredAt;
    }

    @PrePersist
    void onCreate() {
        if (this.answeredAt == null) {
            this.answeredAt = Instant.now();
        }
    }

    /**
     * Factory: creates a NUMERIC answer (SCALE / NUMBER questions).
     */
    public static DailyQuestionAnswer createNumeric(DailyQuestionAssignment assignment,
                                                    BigDecimal numericValue) {
        return new DailyQuestionAnswer(assignment, assignment.getUserId(),
                AnswerType.NUMERIC, numericValue, null, null, Instant.now());
    }

    /**
     * Factory: creates a TEXT answer.
     */
    public static DailyQuestionAnswer createText(DailyQuestionAssignment assignment,
                                                 String textValue) {
        return new DailyQuestionAnswer(assignment, assignment.getUserId(),
                AnswerType.TEXT, null, textValue, null, Instant.now());
    }

    /**
     * Factory: creates an OPTION answer (SINGLE_CHOICE questions).
     */
    public static DailyQuestionAnswer createOption(DailyQuestionAssignment assignment,
                                                   String optionValue) {
        return new DailyQuestionAnswer(assignment, assignment.getUserId(),
                AnswerType.OPTION, null, null, optionValue, Instant.now());
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public DailyQuestionAssignment getAssignment() {
        return assignment;
    }

    public UUID getUserId() {
        return userId;
    }

    public AnswerType getAnswerType() {
        return answerType;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}