package com.mindbridge.behavior.domain;

import com.mindbridge.auth.domain.entity.User;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity for {@code behavioral_events}.
 *
 * Per docs/02_DATABASE_MVP.md §4.7 + G2-T07 plan.
 *
 * The event is write-only and immutable after insert. There are no setter
 * methods and no {@code update} path — once a behavioral event is recorded,
 * it stays. This is an audit trail, not a mutable log.
 *
 * Note: user_id has an FK ON DELETE CASCADE because when a user is deleted
 * (admin flow) their behavioral history must also disappear (GDPR-friendly).
 * source_id has NO FK because the source row may be deleted independently
 * while the event must survive for analysis.
 */
@Entity
@Table(name = "behavioral_events")
public class BehavioralEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private BehavioralEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private SourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    /**
     * JSON-shaped metadata. Stored as TEXT in H2 (test) and JSONB in PG (prod).
     * The DB column type is VARCHAR(4000) in H2; in PG it is JSONB. Hibernate
     * treats both as String at the Java level. The application contract for
     * the JSON content is enforced by:
     *   - caller's contract (e.g. CHAT_MESSAGE_SENT only sets message_length /
     *     was_redacted / role — never raw content)
     *   - schema_version per event_type
     *
     * MUST NOT contain raw user message content, raw answer content, raw
     * option labels, or PII. See G2-T07 plan §2.3.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    private String properties;

    @Column(name = "schema_version", nullable = false)
    private Short schemaVersion;

    protected BehavioralEvent() {
    }

    private BehavioralEvent(User user, BehavioralEventType eventType, SourceType sourceType,
                            UUID sourceId, Instant occurredAt, LocalDate localDate,
                            String timezone, String properties, Short schemaVersion) {
        this.user = user;
        this.eventType = eventType;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.occurredAt = occurredAt;
        this.localDate = localDate;
        this.timezone = timezone;
        this.properties = properties;
        this.schemaVersion = schemaVersion;
    }

    /**
     * Factory: builds a behavioral event ready to persist. The caller is
     * responsible for serializing {@code properties} to a JSON string before
     * passing it in (we keep this layer dependency-free — Jackson is the
     * caller's concern).
     *
     * @param user          the user who triggered the action (must be a managed entity)
     * @param eventType     the event type
     * @param sourceType    which business table the event references
     * @param sourceId      UUID of the source row (informational, no FK)
     * @param occurredAt    UTC timestamp at the moment the action happened
     * @param localDate     user-local date for {@code occurredAt}
     * @param timezone      IANA TZ used to compute {@code localDate}
     * @param properties    JSON string (nullable); metadata only, no raw content
     * @param schemaVersion shape version of {@code properties}; bump on JSON change
     */
    public static BehavioralEvent create(User user, BehavioralEventType eventType,
                                          SourceType sourceType, UUID sourceId,
                                          Instant occurredAt, LocalDate localDate,
                                          String timezone, String properties,
                                          Short schemaVersion) {
        return new BehavioralEvent(user, eventType, sourceType, sourceId, occurredAt,
                localDate, timezone, properties, schemaVersion);
    }

    @PrePersist
    void onCreate() {
        if (this.occurredAt == null) {
            this.occurredAt = Instant.now();
        }
        if (this.schemaVersion == null) {
            this.schemaVersion = 1;
        }
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BehavioralEventType getEventType() {
        return eventType;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getProperties() {
        return properties;
    }

    public Short getSchemaVersion() {
        return schemaVersion;
    }
}
