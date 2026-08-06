package com.mindbridge.consent.mapper;

import com.mindbridge.consent.domain.ConsentEvent;
import com.mindbridge.consent.domain.enums.ConsentAction;
import com.mindbridge.consent.domain.enums.ConsentType;
import com.mindbridge.consent.dto.ConsentEventResponse;
import com.mindbridge.consent.dto.CurrentConsentResponse;
import org.springframework.stereotype.Component;

/**
 * Maps ConsentEvent entity to API DTOs. Pure mapping — no business logic.
 */
@Component
public class ConsentEventMapper {

    public ConsentEventResponse toResponse(ConsentEvent event) {
        return new ConsentEventResponse(
                event.getId(),
                event.getConsentType(),
                event.getAction(),
                event.getPolicyVersion(),
                event.getOccurredAt()
        );
    }

    public CurrentConsentResponse toCurrentResponse(ConsentType type,
                                                     boolean granted,
                                                     String policyVersion,
                                                     java.time.Instant updatedAt) {
        return new CurrentConsentResponse(type, granted, policyVersion, updatedAt);
    }
}