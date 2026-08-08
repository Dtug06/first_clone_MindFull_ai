package com.mindbridge.chat.personalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileResponseMapper;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileService;
import com.mindbridge.consent.service.ConsentGuard;
import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.domain.QuestionType;
import com.mindbridge.dailyquestion.repository.DailyQuestionAnswerRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonalizationContextServiceTest {

    @Mock ConsentGuard consentGuard;
    @Mock UserRepository userRepository;
    @Mock DailyQuestionAnswerRepository answerRepository;
    @Mock UserBehaviorProfileService profileService;

    private final UUID userId = UUID.randomUUID();
    private final LocalDate today = LocalDate.of(2026, 8, 6);
    private PersonalizationContextService service;

    @BeforeEach
    void setUp() {
        service = new PersonalizationContextService(
                consentGuard,
                userRepository,
                answerRepository,
                profileService,
                new UserBehaviorProfileResponseMapper(new ObjectMapper()),
                Clock.fixed(Instant.parse("2026-08-06T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void noPersonalizationConsentReturnsEmptyWithoutReadingUserData() {
        when(consentGuard.hasPersonalizationConsent(userId)).thenReturn(false);

        PersonalizationContext result = service.load(userId);

        assertThat(result.available()).isFalse();
        verify(userRepository, never()).findById(userId);
        verify(answerRepository, never())
                .findWithAssignmentByUserIdAndAssignedForDate(userId, today);
    }

    @Test
    void includesNameAndTypedDailyAnswersButExcludesFreeText() {
        User user = User.register("minh@example.test", "hash", "  Minh\nNguyen  ");
        user.setTimezone("UTC");
        DailyQuestionAnswer stress = numericAnswer("STRESS", new BigDecimal("4"));
        DailyQuestionAnswer mood = optionAnswer("MOOD", "GOOD");
        DailyQuestionAnswer note = textAnswer("OPEN_NOTE", "private free text");

        when(consentGuard.hasPersonalizationConsent(userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(answerRepository.findWithAssignmentByUserIdAndAssignedForDate(userId, today))
                .thenReturn(List.of(stress, mood, note));
        when(profileService.findLatestForUser(userId)).thenReturn(Optional.empty());

        PersonalizationContext result = service.load(userId);

        assertThat(result.displayName()).isEqualTo("Minh Nguyen");
        assertThat(result.contextDate()).isEqualTo(today);
        assertThat(result.dailyObservations())
                .extracting(PersonalizationContext.DailyObservation::code)
                .containsExactly("STRESS", "MOOD");
        assertThat(result.toString()).doesNotContain("private free text", "OPEN_NOTE");
    }

    private DailyQuestionAssignment assignment(String code, QuestionType type) {
        DailyQuestionTemplate template = DailyQuestionTemplate.create(code, 1, type, code);
        return DailyQuestionAssignment.create(userId, template, today, "UTC");
    }

    private DailyQuestionAnswer numericAnswer(String code, BigDecimal value) {
        return DailyQuestionAnswer.createNumeric(assignment(code, QuestionType.SCALE), value);
    }

    private DailyQuestionAnswer optionAnswer(String code, String value) {
        return DailyQuestionAnswer.createOption(assignment(code, QuestionType.SINGLE_CHOICE), value);
    }

    private DailyQuestionAnswer textAnswer(String code, String value) {
        return DailyQuestionAnswer.createText(assignment(code, QuestionType.TEXT), value);
    }
}
