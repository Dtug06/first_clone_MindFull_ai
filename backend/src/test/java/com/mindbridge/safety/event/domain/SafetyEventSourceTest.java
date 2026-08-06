package com.mindbridge.safety.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.safety.event.SafetyEventSourceType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link SafetyEventSource} entity   focuses on
 * factory validation and the polymorphic null-id branch.
 *
 * <p>JPA persistence behaviour is exercised separately by the
 * integration test.
 */
@DisplayName("SafetyEventSource")
class SafetyEventSourceTest {

    @Nested
    @DisplayName("Factory validation")
    class FactoryValidation {

        @Test
        @DisplayName("of() rejects null id")
        void nullId() {
            assertThatThrownBy(() -> SafetyEventSource.of(
                    null, UUID.randomUUID(),
                    SafetyEventSourceType.CHAT_ANALYSIS, UUID.randomUUID()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("of() rejects null safetyEventId")
        void nullSafetyEventId() {
            assertThatThrownBy(() -> SafetyEventSource.of(
                    UUID.randomUUID(), null,
                    SafetyEventSourceType.CHAT_ANALYSIS, UUID.randomUUID()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("of() rejects null sourceType")
        void nullSourceType() {
            assertThatThrownBy(() -> SafetyEventSource.of(
                    UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID()))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Polymorphic semantics")
    class PolymorphicSemantics {

        @Test
        @DisplayName("sourceId may be null for audit-only signals")
        void sourceIdNullable() {
            UUID eventId = UUID.randomUUID();
            SafetyEventSource s = SafetyEventSource.of(
                    UUID.randomUUID(), eventId,
                    SafetyEventSourceType.CHAT_ANALYSIS, null);
            assertThat(s.getSourceId()).isNull();
            assertThat(s.getSourceType()).isEqualTo(SafetyEventSourceType.CHAT_ANALYSIS);
            assertThat(s.getSafetyEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("All 4 source types accepted by the factory")
        void allSourceTypes() {
            UUID eventId = UUID.randomUUID();
            for (SafetyEventSourceType type : SafetyEventSourceType.values()) {
                SafetyEventSource s = SafetyEventSource.of(
                        UUID.randomUUID(), eventId, type, UUID.randomUUID());
                assertThat(s.getSourceType()).isEqualTo(type);
            }
        }
    }
}