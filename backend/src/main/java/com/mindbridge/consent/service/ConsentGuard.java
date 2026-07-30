package com.mindbridge.consent.service;

import com.mindbridge.consent.domain.enums.ConsentAction;
import com.mindbridge.consent.domain.enums.ConsentType;
import com.mindbridge.consent.exception.ConsentRequiredException;
import com.mindbridge.consent.repository.ConsentEventRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper that AI / Recommendation / Matching modules MUST call before processing
 * user data.
 *
 * Usage (from a future G3 module):
 *   consentGuard.requireChatAnalysisConsent(userId);
 *
 * Throws ConsentRequiredException (HTTP 409) if the user has not granted the
 * consent or has revoked it.
 */
@Service
public class ConsentGuard {

    private final ConsentEventRepository repository;

    public ConsentGuard(ConsentEventRepository repository) {
        this.repository = repository;
    }

    public void requireChatAnalysisConsent(UUID userId) {
        require(userId, ConsentType.CHAT_ANALYSIS, "Chat analysis consent is required");
    }

    public void requirePersonalizationConsent(UUID userId) {
        require(userId, ConsentType.PERSONALIZATION, "Personalization consent is required");
    }

    public void requireExpertSharingConsent(UUID userId) {
        require(userId, ConsentType.EXPERT_SHARING, "Expert sharing consent is required");
    }

    public boolean hasChatAnalysisConsent(UUID userId) {
        return has(userId, ConsentType.CHAT_ANALYSIS);
    }

    public boolean hasPersonalizationConsent(UUID userId) {
        return has(userId, ConsentType.PERSONALIZATION);
    }

    public boolean hasExpertSharingConsent(UUID userId) {
        return has(userId, ConsentType.EXPERT_SHARING);
    }

    @Transactional(readOnly = true)
    private boolean has(UUID userId, ConsentType type) {
        var events = repository.findLatestByUserAndType(userId, type);
        if (events.isEmpty()) {
            return false;
        }
        return events.get(0).getAction() == ConsentAction.GRANTED;
    }

    @Transactional(readOnly = true)
    private void require(UUID userId, ConsentType type, String message) {
        if (!has(userId, type)) {
            throw new ConsentRequiredException(message);
        }
    }
}