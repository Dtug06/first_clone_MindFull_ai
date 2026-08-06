package com.mindbridge.safety.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "expert_reviews")
public class ExpertReview {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "safety_event_id", nullable = false, updatable = false)
    private UUID safetyEventId;

    @Column(name = "reviewer_id", nullable = false, updatable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    private ExpertReviewDecision decision;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ExpertReview() {
    }

    public static ExpertReview create(UUID id, UUID safetyEventId, UUID reviewerId,
                                      ExpertReviewDecision decision, String note) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(safetyEventId, "safetyEventId must not be null");
        Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        Objects.requireNonNull(decision, "decision must not be null");

        ExpertReview review = new ExpertReview();
        review.id = id;
        review.safetyEventId = safetyEventId;
        review.reviewerId = reviewerId;
        review.decision = decision;
        review.note = note;
        return review;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getSafetyEventId() { return safetyEventId; }
    public UUID getReviewerId() { return reviewerId; }
    public ExpertReviewDecision getDecision() { return decision; }
    public String getNote() { return note; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpertReview other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}