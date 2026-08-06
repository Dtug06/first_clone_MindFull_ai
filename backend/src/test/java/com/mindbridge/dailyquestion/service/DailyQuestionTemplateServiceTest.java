package com.mindbridge.dailyquestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.dailyquestion.domain.QuestionType;
import com.mindbridge.dailyquestion.domain.TemplateStatus;
import com.mindbridge.dailyquestion.dto.CreateTemplateRequest;
import com.mindbridge.dailyquestion.dto.TemplateResponse;
import com.mindbridge.dailyquestion.dto.UpdateTemplateRequest;
import com.mindbridge.dailyquestion.exception.TemplateBusinessException;
import com.mindbridge.dailyquestion.repository.DailyQuestionOptionRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for DailyQuestionTemplateService using H2 in-memory database.
 * Schema is created via @Sql BEFORE the test class (once, before all tests).
 * H2's MVCC isolation ensures concurrent test isolation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "classpath:schema-daily-question.sql")
@DisplayName("DailyQuestionTemplateService")
class DailyQuestionTemplateServiceTest {

    @Autowired
    private DailyQuestionTemplateRepository templateRepository;

    @Autowired
    private DailyQuestionOptionRepository optionRepository;

    private DailyQuestionTemplateService service;

    @Autowired
    void setService(DailyQuestionOptionRepository optionRepo) {
        // Inject after Spring context is ready so schema exists
        this.service = new DailyQuestionTemplateService(templateRepository, optionRepo);
    }

    // --- create ---

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates a SCALE template with version 1 and DRAFT status")
        void createScaleTemplate() {
            var request = new CreateTemplateRequest(
                    "STRESS", QuestionType.SCALE, "How stressed are you?", null);

            TemplateResponse result = service.create(request);

            assertThat(result.code()).isEqualTo("STRESS");
            assertThat(result.version()).isEqualTo(1);
            assertThat(result.questionType()).isEqualTo(QuestionType.SCALE);
            assertThat(result.status()).isEqualTo(TemplateStatus.DRAFT);
            assertThat(result.options()).isEmpty();
        }

        @Test
        @DisplayName("creates a SINGLE_CHOICE template with options")
        void createSingleChoiceTemplate() {
            List<CreateTemplateRequest.OptionRequest> options = List.of(
                    new CreateTemplateRequest.OptionRequest("1", "Low", 1),
                    new CreateTemplateRequest.OptionRequest("2", "Medium", 2),
                    new CreateTemplateRequest.OptionRequest("3", "High", 3)
            );
            var request = new CreateTemplateRequest(
                    "MOOD", QuestionType.SINGLE_CHOICE, "How is your mood?", options);

            TemplateResponse result = service.create(request);

            assertThat(result.questionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
            assertThat(result.options()).hasSize(3);
            assertThat(result.options().get(0).label()).isEqualTo("Low");
            assertThat(result.options().get(1).label()).isEqualTo("Medium");
            assertThat(result.options().get(2).label()).isEqualTo("High");
        }

        @Test
        @DisplayName("throws TemplateBusinessException when code+version 1 already exists")
        void duplicateCreate() {
            var request = new CreateTemplateRequest(
                    "STRESS", QuestionType.SCALE, "How stressed?", null);
            service.create(request);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(TemplateBusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    // --- getLatestByCode ---

    @Nested
    @DisplayName("getLatestByCode")
    class GetLatestByCode {

        @Test
        @DisplayName("returns the latest version of a template")
        void getLatest() {
            service.create(new CreateTemplateRequest(
                    "MOOD", QuestionType.TEXT, "Old prompt?", null));
            service.updateByCode("MOOD", new UpdateTemplateRequest(
                    QuestionType.TEXT, "New prompt", TemplateStatus.APPROVED, null));

            TemplateResponse result = service.getLatestByCode("MOOD");

            assertThat(result.version()).isEqualTo(2);
            assertThat(result.prompt()).isEqualTo("New prompt");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown code")
        void notFound() {
            assertThatThrownBy(() -> service.getLatestByCode("UNKNOWN"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // --- listAll ---

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("returns all versions ordered by code, version desc")
        void listAll() {
            service.create(new CreateTemplateRequest(
                    "MOOD", QuestionType.TEXT, "p1", null));
            service.updateByCode("MOOD", new UpdateTemplateRequest(
                    QuestionType.TEXT, "p2", TemplateStatus.APPROVED, null));
            service.create(new CreateTemplateRequest(
                    "STRESS", QuestionType.SCALE, "p3", null));

            List<TemplateResponse> results = service.listAll();

            assertThat(results).hasSize(3);
            assertThat(results.get(0).code()).isEqualTo("MOOD");
            assertThat(results.get(0).version()).isEqualTo(2);
            assertThat(results.get(1).code()).isEqualTo("MOOD");
            assertThat(results.get(1).version()).isEqualTo(1);
            assertThat(results.get(2).code()).isEqualTo("STRESS");
        }
    }

    // --- updateByCode versioning ---

    @Nested
    @DisplayName("updateByCode versioning")
    class UpdateVersioning {

        @Test
        @DisplayName("updating APPROVED template retires old version and creates new")
        void approvedRetiresOld() {
            // create() → v1 DRAFT
            service.create(new CreateTemplateRequest(
                    "MOOD", QuestionType.SCALE, "Old prompt", null));
            // updateByCode: current v1 DRAFT → retire v1, create v2 APPROVED
            TemplateResponse v2 = service.updateByCode("MOOD",
                    new UpdateTemplateRequest(QuestionType.SCALE, "Old prompt", TemplateStatus.APPROVED, null));
            assertThat(v2.version()).isEqualTo(2);
            assertThat(v2.status()).isEqualTo(TemplateStatus.APPROVED);

            // updateByCode: current v2 APPROVED → retire v2, create v3 APPROVED
            TemplateResponse v3 = service.updateByCode("MOOD",
                    new UpdateTemplateRequest(QuestionType.SCALE, "New prompt", TemplateStatus.APPROVED, null));

            assertThat(v3.version()).isEqualTo(3);
            assertThat(v3.status()).isEqualTo(TemplateStatus.APPROVED);
            assertThat(v3.prompt()).isEqualTo("New prompt");

            // v1 and v2 retired; v3 is latest
            List<TemplateResponse> all = service.listAll();
            assertThat(all).hasSize(3);
            assertThat(all.get(0).version()).isEqualTo(3);
            assertThat(all.stream()
                    .filter(r -> r.version() == 1)
                    .findFirst()
                    .map(r -> r.status()))
                    .contains(TemplateStatus.RETIRED);
            assertThat(all.stream()
                    .filter(r -> r.version() == 2)
                    .findFirst()
                    .map(r -> r.status()))
                    .contains(TemplateStatus.RETIRED);
        }

        @Test
        @DisplayName("updating DRAFT template creates new version, old DRAFT is retired")
        void draftUpdatesInPlace() {
            service.create(new CreateTemplateRequest(
                    "MOOD", QuestionType.TEXT, "Draft prompt", null));

            // Update DRAFT → DRAFT: old v1 retired, new v2 DRAFT with new content
            TemplateResponse v2 = service.updateByCode("MOOD",
                    new UpdateTemplateRequest(QuestionType.TEXT, "New draft prompt", TemplateStatus.DRAFT, null));

            // New version v2 created (version incremented)
            assertThat(v2.version()).isEqualTo(2);
            assertThat(v2.prompt()).isEqualTo("New draft prompt");
            assertThat(v2.status()).isEqualTo(TemplateStatus.DRAFT);
            List<TemplateResponse> all = service.listAll();
            assertThat(all).hasSize(2);
            // v1 is retired, v2 is DRAFT
            assertThat(all.stream()
                    .filter(r -> r.status() == TemplateStatus.RETIRED)
                    .findFirst()
                    .map(r -> r.version()))
                    .contains(1);
        }

        @Test
        @DisplayName("update increments version number correctly")
        void versionIncrement() {
            service.create(new CreateTemplateRequest(
                    "SLEEP", QuestionType.NUMBER, "How many hours?", null));
            service.updateByCode("SLEEP", new UpdateTemplateRequest(
                    QuestionType.NUMBER, "New", TemplateStatus.APPROVED, null));
            service.updateByCode("SLEEP", new UpdateTemplateRequest(
                    QuestionType.NUMBER, "Newer", TemplateStatus.APPROVED, null));

            TemplateResponse latest = service.getLatestByCode("SLEEP");
            assertThat(latest.version()).isEqualTo(3);
        }

        @Test
        @DisplayName("update replaces options for SINGLE_CHOICE type")
        void updateReplacesOptions() {
            List<CreateTemplateRequest.OptionRequest> opts = List.of(
                    new CreateTemplateRequest.OptionRequest("a", "Alpha", 1),
                    new CreateTemplateRequest.OptionRequest("b", "Beta", 2)
            );
            service.create(new CreateTemplateRequest(
                    "RATING", QuestionType.SINGLE_CHOICE, "Rate today", opts));
            service.updateByCode("RATING", new UpdateTemplateRequest(
                    QuestionType.SINGLE_CHOICE, "Rate today v2",
                    TemplateStatus.APPROVED,
                    List.of(new CreateTemplateRequest.OptionRequest("a", "Alpha Updated", 1)))
            );

            TemplateResponse latest = service.getLatestByCode("RATING");
            assertThat(latest.options()).hasSize(1);
            assertThat(latest.options().get(0).label()).isEqualTo("Alpha Updated");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown code")
        void notFound() {
            assertThatThrownBy(() -> service.updateByCode("UNKNOWN",
                    new UpdateTemplateRequest(QuestionType.TEXT, "p", TemplateStatus.DRAFT, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // --- Seed verification ---

    @Nested
    @DisplayName("Seed verification")
    class SeedVerification {

        @Test
        @DisplayName("MOOD template options are ordered by order_index ascending")
        void moodOptionsOrdered() {
            // Create MOOD template and options matching the seed data
            service.create(new CreateTemplateRequest(
                    "MOOD", QuestionType.SINGLE_CHOICE, "Tâm trạng hôm nay?",
                    List.of(
                            new CreateTemplateRequest.OptionRequest("1", "Rất tệ", 1),
                            new CreateTemplateRequest.OptionRequest("2", "Tệ", 2),
                            new CreateTemplateRequest.OptionRequest("3", "Bình thường", 3),
                            new CreateTemplateRequest.OptionRequest("4", "Tốt", 4),
                            new CreateTemplateRequest.OptionRequest("5", "Rất tốt", 5)
                    )));

            templateRepository.findTopByCodeOrderByVersionDesc("MOOD")
                    .ifPresent(mood -> {
                        var opts = optionRepository.findByTemplateIdOrderByOrderIndexAsc(mood.getId());
                        assertThat(opts).hasSize(5);
                        assertThat(opts.get(0).getOptionValue()).isEqualTo("1");
                        assertThat(opts.get(0).getLabel()).isEqualTo("Rất tệ");
                        assertThat(opts.get(4).getOptionValue()).isEqualTo("5");
                        assertThat(opts.get(4).getLabel()).isEqualTo("Rất tốt");
                    });
        }

        @Test
        @DisplayName("5 MVP template codes exist after seed (STRESS, MOOD, SLEEP, ENERGY, OPEN)")
        void fiveMvpTemplatesExist() {
            // Insert all 5 MVP templates
            service.create(new CreateTemplateRequest("STRESS", QuestionType.SCALE, "Stress level?", null));
            service.create(new CreateTemplateRequest("MOOD", QuestionType.SINGLE_CHOICE, "Mood?", null));
            service.create(new CreateTemplateRequest("SLEEP", QuestionType.NUMBER, "Sleep hours?", null));
            service.create(new CreateTemplateRequest("ENERGY", QuestionType.SCALE, "Energy level?", null));
            service.create(new CreateTemplateRequest("OPEN", QuestionType.TEXT, "Share something?", null));

            var latest = templateRepository.findLatestVersions();
            assertThat(latest).hasSize(5);
            assertThat(latest.stream().map(r -> r.getCode())).containsExactlyInAnyOrder(
                    "STRESS", "MOOD", "SLEEP", "ENERGY", "OPEN");
        }
    }
}
