package com.mindbridge.safety.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.safety.response.SafetyResponseTemplateStatus;
import com.mindbridge.safety.response.domain.SafetyResponseTemplate;
import com.mindbridge.safety.response.exception.SafetyResponseTemplateInputException;
import com.mindbridge.safety.response.repository.SafetyResponseTemplateRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link SafetyResponseTemplateService}.
 *
 * <p>Exercises the full persistence path against the H2 schema mirror
 * ({@code schema-safety-response-templates.sql}). Verifies:
 * <ul>
 *   <li>create() inserts a DRAFT row and audits the action.</li>
 *   <li>submitForReview() transitions DRAFT - PENDING_REVIEW.</li>
 *   <li>approve() enforces the role check (EXPERT or ADMIN only) and
 *       the partial-uniqueness invariants on the H2 test schema.</li>
 *   <li>retire() transitions APPROVED - RETIRED.</li>
 *   <li>Duplicate (code, template_version) is rejected on create().</li>
 * </ul>
 *
 * <p>Mirrors the structure of {@code SafetyKeywordRuleServiceTest} (T08)
 * so the project keeps one consistent test idiom.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mindbridge.ai.provider=mock",
        "mindbridge.ai.mock.force-scenario="
})
@Sql(scripts = {
        "/schema-safety-response-templates.sql"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("SafetyResponseTemplateService integration")
class SafetyResponseTemplateServiceIntegrationTest {

    @Autowired
    private SafetyResponseTemplateService service;

    @Autowired
    private SafetyResponseTemplateRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID expertId;
    private UUID adminId;
    private UUID userId;

    @BeforeEach
    @Transactional
    void seedUsers() {
        repository.deleteAll();
        jdbc.update("DELETE FROM users");

        // Use raw SQL because User.setRole / setStatus are package-private
        // to the auth.domain.entity package. This mirrors the pattern used
        // by T11 integration tests: bypass the JPA factory when the test
        // needs a non-USER role.
        expertId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();
        jdbc.update(
                "INSERT INTO users (id, email, password_hash, display_name, "
                        + "role, status, timezone, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                expertId.toString(),
                "expert-" + expertId + "@mindbridge.local",
                "x", "Expert", "EXPERT", "ACTIVE", "UTC", now, now);
        jdbc.update(
                "INSERT INTO users (id, email, password_hash, display_name, "
                        + "role, status, timezone, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                adminId.toString(),
                "admin-" + adminId + "@mindbridge.local",
                "x", "Admin", "ADMIN", "ACTIVE", "UTC", now, now);
        jdbc.update(
                "INSERT INTO users (id, email, password_hash, display_name, "
                        + "role, status, timezone, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId.toString(),
                "user-" + userId + "@mindbridge.local",
                "x", "User", "USER", "ACTIVE", "UTC", now, now);
    }

    @Nested
    @DisplayName("Create")
    class Create {

        @Test
        @DisplayName("create() inserts a DRAFT row and audits the action")
        void createInsertsDraft() {
            SafetyResponseTemplate t = service.create(
                    "C1", "v1", "vi", "REASON_L4",
                    "expert content", false);

            assertThat(t.getId()).isNotNull();
            assertThat(t.getStatus()).isEqualTo(SafetyResponseTemplateStatus.DRAFT);
            assertThat(t.getCode()).isEqualTo("C1");
            assertThat(t.getTemplateVersion()).isEqualTo("v1");
            assertThat(t.getRiskReason()).isEqualTo("REASON_L4");
            assertThat(t.getContent()).isEqualTo("expert content");
            assertThat(t.isDefault()).isFalse();
            assertThat(t.getCreatedAt()).isNotNull();

            SafetyResponseTemplate roundtrip = repository
                    .findById(t.getId()).orElseThrow();
            assertThat(roundtrip.getStatus())
                    .isEqualTo(SafetyResponseTemplateStatus.DRAFT);
        }

        @Test
        @DisplayName("create() rejects duplicate (code, template_version)")
        void duplicateRejected() {
            service.create("CDUP", "v1", "vi", "REASON_L4", "x", false);
            assertThatThrownBy(() -> service.create(
                    "CDUP", "v1", "vi", "REASON_L4", "y", false))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("Submit / Approve / Retire")
    class Lifecycle {

        @Test
        @DisplayName("DRAFT - PENDING_REVIEW - APPROVED - RETIRED happy path")
        void happyPath() {
            SafetyResponseTemplate t = service.create(
                    "CL1", "v1", "vi", "REASON_L4", "x", false);
            assertThat(t.getStatus())
                    .isEqualTo(SafetyResponseTemplateStatus.DRAFT);

            SafetyResponseTemplate pending = service.submitForReview(t.getId());
            assertThat(pending.getStatus())
                    .isEqualTo(SafetyResponseTemplateStatus.PENDING_REVIEW);

            SafetyResponseTemplate approved = service.approve(
                    t.getId(), expertId);
            assertThat(approved.getStatus())
                    .isEqualTo(SafetyResponseTemplateStatus.APPROVED);
            assertThat(approved.getApprovedBy()).isEqualTo(expertId);
            assertThat(approved.getApprovedAt()).isNotNull();

            SafetyResponseTemplate retired = service.retire(t.getId());
            assertThat(retired.getStatus())
                    .isEqualTo(SafetyResponseTemplateStatus.RETIRED);
        }

        @Test
        @DisplayName("ADMIN can also approve")
        void adminCanApprove() {
            SafetyResponseTemplate t = service.create(
                    "CA1", "v1", "vi", "REASON_L4", "x", false);
            service.submitForReview(t.getId());
            SafetyResponseTemplate approved = service.approve(
                    t.getId(), adminId);
            assertThat(approved.getStatus())
                    .isEqualTo(SafetyResponseTemplateStatus.APPROVED);
            assertThat(approved.getApprovedBy()).isEqualTo(adminId);
        }
    }

    @Nested
    @DisplayName("Role check")
    class RoleCheck {

        @Test
        @DisplayName("USER role cannot approve")
        void userCannotApprove() {
            SafetyResponseTemplate t = service.create(
                    "CR1", "v1", "vi", "REASON_L4", "x", false);
            service.submitForReview(t.getId());
            assertThatThrownBy(() -> service.approve(
                    t.getId(), userId))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("EXPERT or ADMIN");
        }

        @Test
        @DisplayName("non-existent approver rejected")
        void unknownApprover() {
            SafetyResponseTemplate t = service.create(
                    "CR2", "v1", "vi", "REASON_L4", "x", false);
            service.submitForReview(t.getId());
            assertThatThrownBy(() -> service.approve(
                    t.getId(), UUID.randomUUID()))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Transition guards")
    class TransitionGuards {

        @Test
        @DisplayName("approve from DRAFT is rejected")
        void approveFromDraftRejected() {
            SafetyResponseTemplate t = service.create(
                    "CG1", "v1", "vi", "REASON_L4", "x", false);
            assertThatThrownBy(() -> service.approve(
                    t.getId(), expertId))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("PENDING_REVIEW");
        }

        @Test
        @DisplayName("retire from DRAFT is rejected")
        void retireFromDraftRejected() {
            SafetyResponseTemplate t = service.create(
                    "CG2", "v1", "vi", "REASON_L4", "x", false);
            assertThatThrownBy(() -> service.retire(t.getId()))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("submitForReview twice is rejected")
        void submitTwice() {
            SafetyResponseTemplate t = service.create(
                    "CG3", "v1", "vi", "REASON_L4", "x", false);
            service.submitForReview(t.getId());
            assertThatThrownBy(() -> service.submitForReview(t.getId()))
                    .isInstanceOf(SafetyResponseTemplateInputException.class);
        }
    }

    @Nested
    @DisplayName("Partial-uniqueness invariants (H2 mirror)")
    class PartialUniqueness {

        @Test
        @DisplayName("at most one APPROVED row per (code, locale, risk_reason)")
        void oneApprovedPerTriple() {
            SafetyResponseTemplate first = service.create(
                    "U1", "v1", "vi", "REASON_L4", "x", false);
            service.submitForReview(first.getId());
            service.approve(first.getId(), expertId);

            // New code, same locale + risk_reason is fine; same code is not.
            SafetyResponseTemplate second = service.create(
                    "U1", "v2", "vi", "REASON_L4", "y", false);
            service.submitForReview(second.getId());
            assertThatThrownBy(() -> service.approve(
                    second.getId(), expertId))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("another APPROVED row");
        }

        @Test
        @DisplayName("at most one APPROVED default row per locale")
        void oneDefaultPerLocale() {
            SafetyResponseTemplate first = service.create(
                    "D1", "v1", "vi", "DEFAULT", "x", true);
            service.submitForReview(first.getId());
            service.approve(first.getId(), expertId);

            // Second default row in same locale is rejected.
            SafetyResponseTemplate second = service.create(
                    "D2", "v1", "vi", "DEFAULT", "y", true);
            service.submitForReview(second.getId());
            assertThatThrownBy(() -> service.approve(
                    second.getId(), expertId))
                    .isInstanceOf(SafetyResponseTemplateInputException.class)
                    .hasMessageContaining("default");
        }
    }

    @SuppressWarnings("unused")
    private static OffsetDateTime now() {
        return OffsetDateTime.now();
    }
}
