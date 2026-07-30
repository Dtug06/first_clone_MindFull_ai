package com.mindbridge.consent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.consent.domain.ConsentEvent;
import com.mindbridge.consent.domain.enums.ConsentAction;
import com.mindbridge.consent.domain.enums.ConsentType;
import com.mindbridge.consent.exception.ConsentRequiredException;
import com.mindbridge.consent.repository.ConsentEventRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * Tests for ConsentGuard.
 *
 * Creates a real user (FK constraint requires users.id to exist)
 * then verifies guard reads the latest event per type correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-consent.sql"
})
@DisplayName("ConsentGuard")
class ConsentGuardTest {

    @Autowired
    private ConsentEventRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ConsentGuard guard;
    private User testUser;

    @BeforeEach
    void setUp() {
        guard = new ConsentGuard(repository);
        testUser = User.register(
                "guard-test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
                passwordEncoder.encode("PassPass123!"),
                "Guard Test");
        userRepository.save(testUser);
    }

    @AfterEach
    void cleanup() {
        repository.deleteAll();
        userRepository.deleteAll();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("hasChatAnalysisConsent returns false when no event exists")
    void noEvent_returnsFalse() {
        assertThat(guard.hasChatAnalysisConsent(testUser.getId())).isFalse();
    }

    @Test
    @DisplayName("GRANTED → hasChatAnalysisConsent returns true")
    void granted_returnsTrue() {
        repository.save(ConsentEvent.record(
                testUser.getId(), ConsentType.CHAT_ANALYSIS, ConsentAction.GRANTED, "1.0", null));

        assertThat(guard.hasChatAnalysisConsent(testUser.getId())).isTrue();
    }

    @Test
    @DisplayName("REVOKED → hasChatAnalysisConsent returns false")
    void revoked_returnsFalse() {
        repository.save(ConsentEvent.record(
                testUser.getId(), ConsentType.CHAT_ANALYSIS, ConsentAction.REVOKED, "1.0", null));

        assertThat(guard.hasChatAnalysisConsent(testUser.getId())).isFalse();
    }

    @Test
    @DisplayName("GRANTED then REVOKED → hasChatAnalysisConsent returns false (latest wins)")
    void grantedThenRevoked_latestWins() {
        repository.save(ConsentEvent.record(
                testUser.getId(), ConsentType.CHAT_ANALYSIS, ConsentAction.GRANTED, "1.0", null));
        repository.save(ConsentEvent.record(
                testUser.getId(), ConsentType.CHAT_ANALYSIS, ConsentAction.REVOKED, "1.0", null));

        assertThat(guard.hasChatAnalysisConsent(testUser.getId())).isFalse();
    }

    @Test
    @DisplayName("requireChatAnalysisConsent throws when no event")
    void require_noEvent_throws() {
        assertThatThrownBy(() -> guard.requireChatAnalysisConsent(testUser.getId()))
                .isInstanceOf(ConsentRequiredException.class)
                .hasMessageContaining("Chat analysis");
    }

    @Test
    @DisplayName("requireChatAnalysisConsent throws when latest is REVOKED")
    void require_revoked_throws() {
        repository.save(ConsentEvent.record(
                testUser.getId(), ConsentType.CHAT_ANALYSIS, ConsentAction.REVOKED, "1.0", null));

        assertThatThrownBy(() -> guard.requireChatAnalysisConsent(testUser.getId()))
                .isInstanceOf(ConsentRequiredException.class);
    }

    @Test
    @DisplayName("requireChatAnalysisConsent passes when latest is GRANTED")
    void require_granted_passes() {
        repository.save(ConsentEvent.record(
                testUser.getId(), ConsentType.CHAT_ANALYSIS, ConsentAction.GRANTED, "1.0", null));

        guard.requireChatAnalysisConsent(testUser.getId()); // must not throw
    }

    @Test
    @DisplayName("Consent types are independent")
    void typesIndependent() {
        repository.save(ConsentEvent.record(
                testUser.getId(), ConsentType.CHAT_ANALYSIS, ConsentAction.GRANTED, "1.0", null));

        assertThat(guard.hasChatAnalysisConsent(testUser.getId())).isTrue();
        assertThat(guard.hasPersonalizationConsent(testUser.getId())).isFalse();
        assertThat(guard.hasExpertSharingConsent(testUser.getId())).isFalse();
    }
}