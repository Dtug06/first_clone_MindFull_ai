package com.mindbridge.dailyquestion.service;

import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.dailyquestion.domain.DailyQuestionOption;
import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.domain.QuestionType;
import com.mindbridge.dailyquestion.domain.TemplateStatus;
import com.mindbridge.dailyquestion.dto.CreateTemplateRequest;
import com.mindbridge.dailyquestion.dto.TemplateResponse;
import com.mindbridge.dailyquestion.dto.UpdateTemplateRequest;
import com.mindbridge.dailyquestion.exception.TemplateBusinessException;
import com.mindbridge.dailyquestion.repository.DailyQuestionOptionRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Manages the daily question template catalog.
 *
 * Versioning rule (G2-T04):
 * - Templates are versioned per (code, version).
 * - Admin update ALWAYS creates a new version row — never mutates an existing row.
 * - If the current latest version is APPROVED (implying it may have assignments),
 *   it is retired before the new version is created.
 * - DRAFT versions without assignments can be superseded without retiring.
 *
 * Security: all methods in this service require ADMIN role. The controller
 * enforces the ADMIN check before calling service methods.
 */
@Service
public class DailyQuestionTemplateService {

    private final DailyQuestionTemplateRepository templateRepository;
    private final DailyQuestionOptionRepository optionRepository;

    public DailyQuestionTemplateService(
            DailyQuestionTemplateRepository templateRepository,
            DailyQuestionOptionRepository optionRepository) {
        this.templateRepository = templateRepository;
        this.optionRepository = optionRepository;
    }

    // --- Read operations ---

    /**
     * Returns all template versions (all versions), ordered by code then version desc.
     * Admin listing endpoint.
     */
    public List<TemplateResponse> listAll() {
        return templateRepository.findAllByOrderByCodeAscVersionDesc().stream()
                .map(TemplateResponse::from)
                .toList();
    }

    /**
     * Returns the latest version of a template by code.
     *
     * @throws ResourceNotFoundException if no template with this code exists
     */
    public TemplateResponse getLatestByCode(String code) {
        DailyQuestionTemplate template = templateRepository.findTopByCodeOrderByVersionDesc(code)
                .orElseThrow(() -> new ResourceNotFoundException("DailyQuestionTemplate", code));
        return TemplateResponse.from(template);
    }

    // --- Write operations ---

    /**
     * Creates a new template (version 1, DRAFT by default).
     * Options are persisted via JPA cascade on the second save when options exist.
     *
     * @throws TemplateBusinessException if (code, version=1) already exists
     */
    @Transactional
    public TemplateResponse create(CreateTemplateRequest request) {
        if (templateRepository.existsByCodeAndVersion(request.code(), 1)) {
            throw new TemplateBusinessException(
                    "Template with code '" + request.code() + "' and version 1 already exists");
        }

        DailyQuestionTemplate template = DailyQuestionTemplate.create(
                request.code(),
                1,
                request.questionType(),
                request.prompt()
        );
        template = templateRepository.save(template);

        attachOptions(template, request.questionType(), request.options());

        // Only re-save if options were added (cascades to option table).
        // For types without options (SCALE, TEXT, NUMBER), this is a no-op.
        if (request.questionType() == QuestionType.SINGLE_CHOICE
                && request.options() != null && !request.options().isEmpty()) {
            template = templateRepository.save(template);
        }

        return TemplateResponse.from(template);
    }

    /**
     * Updates a template. Two cases:
     * 1. Current is APPROVED → retire it, create new version row (never mutate assigned content).
     * 2. Current is DRAFT → update the single DRAFT row in-place with new content.
     *
     * @throws ResourceNotFoundException if the code has no existing template
     */
    @Transactional
    public TemplateResponse updateByCode(String code, UpdateTemplateRequest request) {
        DailyQuestionTemplate current = templateRepository.findTopByCodeOrderByVersionDesc(code)
                .orElseThrow(() -> new ResourceNotFoundException("DailyQuestionTemplate", code));

        if (current.getStatus() == TemplateStatus.APPROVED) {
            // APPROVED → retire current (may have assignments), create new version
            current.retire();
            templateRepository.save(current);

            int nextVersion = current.getVersion() + 1;
            DailyQuestionTemplate next = DailyQuestionTemplate.create(
                    code, nextVersion, request.questionType(), request.prompt());
            next.setStatus(request.status());
            next = templateRepository.save(next);
            attachOptions(next, request.questionType(), request.options());
            if (request.questionType() == QuestionType.SINGLE_CHOICE
                    && request.options() != null && !request.options().isEmpty()) {
                next = templateRepository.save(next);
            }
            return TemplateResponse.from(next);
        } else {
            // DRAFT → retire current (no assignments exist), create new version
            current.retire();
            templateRepository.save(current);

            int nextVersion = current.getVersion() + 1;
            DailyQuestionTemplate updated = DailyQuestionTemplate.create(
                    code, nextVersion, request.questionType(), request.prompt());
            updated.setStatus(request.status());
            updated = templateRepository.save(updated);
            attachOptions(updated, request.questionType(), request.options());
            if (request.questionType() == QuestionType.SINGLE_CHOICE
                    && request.options() != null && !request.options().isEmpty()) {
                updated = templateRepository.save(updated);
            }
            return TemplateResponse.from(updated);
        }
    }

    /**
     * Helper: attaches options to a SINGLE_CHOICE template.
     * No options are created for other question types.
     */
    private void attachOptions(DailyQuestionTemplate template,
                               QuestionType type,
                               List<CreateTemplateRequest.OptionRequest> optionRequests) {
        if (type != QuestionType.SINGLE_CHOICE || optionRequests == null || optionRequests.isEmpty()) {
            return;
        }

        for (CreateTemplateRequest.OptionRequest req : optionRequests) {
            DailyQuestionOption option = DailyQuestionOption.create(
                    template, req.optionValue(), req.label(), req.orderIndex());
            template.addOption(option);
        }
    }
}
