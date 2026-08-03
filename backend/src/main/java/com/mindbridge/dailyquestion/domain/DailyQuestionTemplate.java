package com.mindbridge.dailyquestion.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for the {@code daily_question_templates} table.
 *
 * Versioning rule (G2-T04):
 * - Each (code, version) pair is unique.
 * - Admin update retires the current APPROVED version and inserts a new row
 *   with incremented version — never modifies existing rows.
 * - Template versions assigned to users are never mutated.
 * - No setters — immutable after construction.
 */
@Entity
@Table(name = "daily_question_templates")
public class DailyQuestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Inclusive lower bound for NUMERIC answers. NULL = no lower bound.
     * Added in G2-T06 for answer validation against SCALE/NUMBER ranges.
     */
    @Column(name = "scale_min")
    private BigDecimal scaleMin;

    /**
     * Inclusive upper bound for NUMERIC answers. NULL = no upper bound.
     */
    @Column(name = "scale_max")
    private BigDecimal scaleMax;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DailyQuestionOption> options = new ArrayList<>();

    protected DailyQuestionTemplate() {
    }

    private DailyQuestionTemplate(String code, Integer version, QuestionType questionType,
                                  String prompt, TemplateStatus status, Instant createdAt,
                                  Instant updatedAt, BigDecimal scaleMin, BigDecimal scaleMax) {
        this.code = code;
        this.version = version;
        this.questionType = questionType;
        this.prompt = prompt;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = TemplateStatus.DRAFT;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Factory: creates a new DRAFT template.
     */
    public static DailyQuestionTemplate create(String code, Integer version,
                                                QuestionType questionType, String prompt) {
        Instant now = Instant.now();
        return new DailyQuestionTemplate(code, version, questionType, prompt,
                                          TemplateStatus.DRAFT, now, now, null, null);
    }

    /**
     * Factory with scale range. Used by tests for SCALE/NUMBER templates.
     * scaleMin/scaleMax may be null (= no bound).
     */
    public static DailyQuestionTemplate create(String code, Integer version,
                                                QuestionType questionType, String prompt,
                                                BigDecimal scaleMin, BigDecimal scaleMax) {
        Instant now = Instant.now();
        return new DailyQuestionTemplate(code, version, questionType, prompt,
                                          TemplateStatus.DRAFT, now, now, scaleMin, scaleMax);
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Integer getVersion() {
        return version;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public String getPrompt() {
        return prompt;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public BigDecimal getScaleMin() {
        return scaleMin;
    }

    public BigDecimal getScaleMax() {
        return scaleMax;
    }

    public List<DailyQuestionOption> getOptions() {
        return options;
    }

    /** Bidirectional add — maintains parent side of relationship. */
    public void addOption(DailyQuestionOption option) {
        options.add(option);
        option.setTemplate(this);
    }

    /**
     * Controlled status transition: marks this template version as RETIRED.
     * This is the only mutable field transition allowed on entity rows —
     * used when a newer version is published to supersede this one.
     */
    public void retire() {
        this.status = TemplateStatus.RETIRED;
    }

    /**
     * Sets the status on a newly created template entity.
     * Only used by service layer after creation when the status differs from DRAFT.
     */
    public void setStatus(TemplateStatus status) {
        this.status = status;
    }

    /**
     * Sets the scale range for SCALE/NUMBER templates.
     * Used by V10 migration backfill and by tests; production code goes
     * through the admin template update flow which preserves content.
     */
    public void setScaleRange(BigDecimal scaleMin, BigDecimal scaleMax) {
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
    }
}
