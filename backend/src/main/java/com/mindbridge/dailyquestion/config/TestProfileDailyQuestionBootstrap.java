package com.mindbridge.dailyquestion.config;

import com.mindbridge.dailyquestion.domain.DailyQuestionOption;
import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.domain.QuestionType;
import com.mindbridge.dailyquestion.domain.TemplateStatus;
import com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Bootstraps the approved MVP daily-question catalog for the isolated H2 test
 * runtime used by manual frontend/backend smoke testing.
 *
 * <p>PostgreSQL environments remain owned by Flyway V6. This component can
 * never run outside the {@code test} profile and never changes a non-empty
 * catalog.
 */
@Component
@Profile("test")
@ConditionalOnProperty(
        prefix = "mindbridge.daily-question",
        name = "bootstrap-approved-templates",
        havingValue = "true")
public class TestProfileDailyQuestionBootstrap implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(TestProfileDailyQuestionBootstrap.class);

    private final DailyQuestionTemplateRepository templateRepository;

    public TestProfileDailyQuestionBootstrap(
            DailyQuestionTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (templateRepository.count() > 0) {
                log.info("Daily-question H2 bootstrap skipped: catalog is not empty");
                return;
            }

            DailyQuestionTemplate stress = numericTemplate(
                    "STRESS", QuestionType.SCALE,
                    "Hôm nay bạn cảm thấy mức căng thẳng của mình như thế nào?",
                    "1", "5");
            DailyQuestionTemplate mood = template(
                    "MOOD", QuestionType.SINGLE_CHOICE,
                    "Tâm trạng hôm nay của bạn như thế nào?");
            addMoodOptions(mood);
            DailyQuestionTemplate sleep = numericTemplate(
                    "SLEEP", QuestionType.NUMBER,
                    "Bạn ngủ bao nhiêu giờ đêm qua?", "0", "24");
            DailyQuestionTemplate energy = numericTemplate(
                    "ENERGY", QuestionType.SCALE,
                    "Mức năng lượng của bạn hôm nay như thế nào?", "1", "5");
            DailyQuestionTemplate open = template(
                    "OPEN", QuestionType.TEXT,
                    "Có điều gì bạn muốn chia sẻ hôm nay không?");

            templateRepository.saveAll(List.of(stress, mood, sleep, energy, open));
            log.info("Daily-question H2 bootstrap created 5 approved MVP templates");
        } catch (DataAccessException ex) {
            // Some focused test contexts create their schema later with @Sql.
            // Skipping there keeps this smoke-test helper isolated and harmless.
            log.debug("Daily-question H2 bootstrap skipped: catalog table is unavailable");
        }
    }

    private static DailyQuestionTemplate template(
            String code, QuestionType type, String prompt) {
        DailyQuestionTemplate template = DailyQuestionTemplate.create(code, 1, type, prompt);
        template.setStatus(TemplateStatus.APPROVED);
        return template;
    }

    private static DailyQuestionTemplate numericTemplate(
            String code,
            QuestionType type,
            String prompt,
            String min,
            String max) {
        DailyQuestionTemplate template = DailyQuestionTemplate.create(
                code, 1, type, prompt, new BigDecimal(min), new BigDecimal(max));
        template.setStatus(TemplateStatus.APPROVED);
        return template;
    }

    private static void addMoodOptions(DailyQuestionTemplate mood) {
        mood.addOption(DailyQuestionOption.create(mood, "1", "Rất tệ", 1));
        mood.addOption(DailyQuestionOption.create(mood, "2", "Tệ", 2));
        mood.addOption(DailyQuestionOption.create(mood, "3", "Bình thường", 3));
        mood.addOption(DailyQuestionOption.create(mood, "4", "Tốt", 4));
        mood.addOption(DailyQuestionOption.create(mood, "5", "Rất tốt", 5));
    }
}
