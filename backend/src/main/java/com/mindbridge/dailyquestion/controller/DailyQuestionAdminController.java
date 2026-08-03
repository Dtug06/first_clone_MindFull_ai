package com.mindbridge.dailyquestion.controller;

import com.mindbridge.common.exception.AccessDeniedException;
import com.mindbridge.common.service.CurrentUserService;
import com.mindbridge.dailyquestion.dto.CreateTemplateRequest;
import com.mindbridge.dailyquestion.dto.TemplateResponse;
import com.mindbridge.dailyquestion.dto.UpdateTemplateRequest;
import com.mindbridge.dailyquestion.service.DailyQuestionTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST controller for the Daily Question Template Catalog.
 *
 * All endpoints require ADMIN role — enforced via CurrentUserService.getCurrentUserRole().
 *
 * Versioning: POST creates version 1; PUT creates a new version and retires the
 * current APPROVED version. Existing rows are never mutated except for retirement.
 */
@RestController
@RequestMapping("/admin/daily-questions")
public class DailyQuestionAdminController {

    private final DailyQuestionTemplateService templateService;
    private final CurrentUserService currentUserService;

    public DailyQuestionAdminController(
            DailyQuestionTemplateService templateService,
            CurrentUserService currentUserService) {
        this.templateService = templateService;
        this.currentUserService = currentUserService;
    }

    /**
     * GET /admin/daily-questions — list all template versions.
     */
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> listAll() {
        requireAdmin();
        return ResponseEntity.ok(templateService.listAll());
    }

    /**
     * GET /admin/daily-questions/{code} — get the latest version of a template.
     */
    @GetMapping("/{code}")
    public ResponseEntity<TemplateResponse> getByCode(@PathVariable String code) {
        requireAdmin();
        return ResponseEntity.ok(templateService.getLatestByCode(code));
    }

    /**
     * POST /admin/daily-questions — create a new template (version 1, DRAFT).
     */
    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request) {
        requireAdmin();
        TemplateResponse response = templateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /admin/daily-questions/{code} — update by creating a new version.
     *
     * The current latest APPROVED version is retired; a new version row is created
     * with the updated content.
     */
    @PutMapping("/{code}")
    public ResponseEntity<TemplateResponse> update(
            @PathVariable String code,
            @Valid @RequestBody UpdateTemplateRequest request) {
        requireAdmin();
        return ResponseEntity.ok(templateService.updateByCode(code, request));
    }

    private void requireAdmin() {
        String role = currentUserService.getCurrentUserRole();
        if (!"ADMIN".equals(role)) {
            throw new AccessDeniedException("Admin role required for this operation");
        }
    }
}
