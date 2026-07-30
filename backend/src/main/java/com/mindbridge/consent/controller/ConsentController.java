package com.mindbridge.consent.controller;

import com.mindbridge.consent.dto.ConsentEventRequest;
import com.mindbridge.consent.dto.ConsentEventResponse;
import com.mindbridge.consent.dto.CurrentConsentResponse;
import com.mindbridge.consent.service.ConsentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consent controller — append-only consent events for the current user.
 *
 * userId is always taken from the JWT principal (CurrentUserService).
 * Client-supplied userId is never trusted.
 */
@RestController
@RequestMapping("/consents")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    /**
     * POST /consents — record a new consent event (GRANTED or REVOKED).
     * Always appends — never updates existing rows.
     */
    @PostMapping
    public ResponseEntity<ConsentEventResponse> recordConsent(
            @Valid @RequestBody ConsentEventRequest request) {
        ConsentEventResponse response = consentService.recordConsent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /consents/current — current consent state per type for the current user.
     */
    @GetMapping("/current")
    public ResponseEntity<List<CurrentConsentResponse>> getCurrentConsentStates() {
        return ResponseEntity.ok(consentService.getCurrentConsentStates());
    }
}