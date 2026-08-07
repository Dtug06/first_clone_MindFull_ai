package com.mindbridge.auth.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the {@code users} table.
 *
 * Security rule: passwordHash is never exposed outside this class.
 * The UserMapper converts to UserResponse (DTO) which omits it entirely.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // PostgreSQL CITEXT keeps both uniqueness and lookup case-insensitive.
    // Declaring the real database type also lets Hibernate `validate` compare
    // this entity with the Flyway-owned schema correctly.
    @Column(unique = true, nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /**
     * IANA timezone id (e.g. "Asia/Ho_Chi_Minh"), used to compute local_date for
     * daily question assignments. Added in G2-T05.
     */
    @Column(nullable = false, length = 50)
    private String timezone = "UTC";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Factory method for creating a new user during registration.
     * Sets role to USER and status to ACTIVE — the only valid initial state.
     */
    public static User register(String email, String passwordHash, String displayName) {
        User user = new User();
        user.email = email;
        user.displayName = displayName;
        user.passwordHash = passwordHash;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    // Package-private setters for internal use.
    void setEmail(String email) {
        this.email = email;
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    void setRole(UserRole role) {
        this.role = role;
    }

    void setStatus(UserStatus status) {
        this.status = status;
    }

    /**
     * Updates the user's timezone. Called by future profile-update endpoints.
     * For G2-T05 the timezone is read from the DB to compute local_date for
     * daily question assignments.
     */
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public enum UserRole {
        USER, EXPERT, ADMIN
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}
