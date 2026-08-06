package com.mindbridge.dailyquestion.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.domain.TemplateStatus;
import com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.ApplicationArguments;

class TestProfileDailyQuestionBootstrapTest {

    private final DailyQuestionTemplateRepository repository =
            Mockito.mock(DailyQuestionTemplateRepository.class);
    private final TestProfileDailyQuestionBootstrap bootstrap =
            new TestProfileDailyQuestionBootstrap(repository);

    @Test
    void createsFiveApprovedTemplatesWhenCatalogIsEmpty() {
        when(repository.count()).thenReturn(0L);

        bootstrap.run(Mockito.mock(ApplicationArguments.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<DailyQuestionTemplate>> captor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());

        List<DailyQuestionTemplate> templates = new java.util.ArrayList<>();
        captor.getValue().forEach(templates::add);
        assertThat(templates).hasSize(5);
        assertThat(templates).allMatch(
                template -> template.getStatus() == TemplateStatus.APPROVED);
        assertThat(templates).extracting(DailyQuestionTemplate::getCode)
                .containsExactly("STRESS", "MOOD", "SLEEP", "ENERGY", "OPEN");
        DailyQuestionTemplate mood = templates.stream()
                .filter(template -> "MOOD".equals(template.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(mood.getOptions()).hasSize(5);
    }

    @Test
    void preservesAnExistingCatalog() {
        when(repository.count()).thenReturn(1L);

        bootstrap.run(Mockito.mock(ApplicationArguments.class));

        verify(repository, never()).saveAll(Mockito.any());
    }
}
