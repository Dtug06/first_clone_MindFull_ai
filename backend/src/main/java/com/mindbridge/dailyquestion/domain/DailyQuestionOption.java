package com.mindbridge.dailyquestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * JPA entity for the {@code daily_question_options} table.
 *
 * Options belong to exactly one template version.
 * When a template version is retired its options are cascade-deleted.
 * No setters — immutable after construction.
 */
@Entity
@Table(name = "daily_question_options")
public class DailyQuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private DailyQuestionTemplate template;

    @Column(name = "option_value", nullable = false, length = 50)
    private String optionValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    protected DailyQuestionOption() {
    }

    private DailyQuestionOption(DailyQuestionTemplate template, String optionValue,
                                String label, Integer orderIndex) {
        this.template = template;
        this.optionValue = optionValue;
        this.label = label;
        this.orderIndex = orderIndex;
    }

    /** Factory — caller is responsible for adding to template's option list. */
    public static DailyQuestionOption create(DailyQuestionTemplate template,
                                             String optionValue, String label, Integer orderIndex) {
        return new DailyQuestionOption(template, optionValue, label, orderIndex);
    }

    // --- Setters for JPA (needed for bidirectional consistency before addOption) ---

    void setTemplate(DailyQuestionTemplate template) {
        this.template = template;
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public DailyQuestionTemplate getTemplate() {
        return template;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public String getLabel() {
        return label;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }
}
