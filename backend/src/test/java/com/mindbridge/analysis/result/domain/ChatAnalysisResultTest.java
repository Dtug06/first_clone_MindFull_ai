package com.mindbridge.analysis.result.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.Emotion;
import com.mindbridge.analysis.provider.EvidenceSpan;
import com.mindbridge.analysis.provider.Intent;
import com.mindbridge.analysis.provider.Signal;
import com.mindbridge.analysis.provider.Topic;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ChatAnalysisResult")
class ChatAnalysisResultTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID RUN_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Nested
    class FactoryValidation {

        @Test
        @DisplayName("null id throws NullPointerException")
        void create_nullId_throws() {
            ChatAnalysisOutput output = okOutput();
            assertThatThrownBy(() -> ChatAnalysisResult.create(null, RUN_ID, MESSAGE_ID, USER_ID, output, NOW))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("null runId throws NullPointerException")
        void create_nullRunId_throws() {
            ChatAnalysisOutput output = okOutput();
            assertThatThrownBy(() -> ChatAnalysisResult.create(ID, null, MESSAGE_ID, USER_ID, output, NOW))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("runId");
        }

        @Test
        @DisplayName("null messageId throws NullPointerException")
        void create_nullMessageId_throws() {
            ChatAnalysisOutput output = okOutput();
            assertThatThrownBy(() -> ChatAnalysisResult.create(ID, RUN_ID, null, USER_ID, output, NOW))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("messageId");
        }

        @Test
        @DisplayName("null userId throws NullPointerException")
        void create_nullUserId_throws() {
            ChatAnalysisOutput output = okOutput();
            assertThatThrownBy(() -> ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, null, output, NOW))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("null output throws NullPointerException")
        void create_nullOutput_throws() {
            assertThatThrownBy(() -> ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, null, NOW))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("output");
        }

        @Test
        @DisplayName("null createdAt throws NullPointerException")
        void create_nullCreatedAt_throws() {
            ChatAnalysisOutput output = okOutput();
            assertThatThrownBy(() -> ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, output, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("createdAt");
        }
    }

    @Nested
    class FactoryOutputMapping {

        @Test
        @DisplayName("maps singular topic/emotion/intent to DB column strings")
        void create_mapsSingularFields() {
            ChatAnalysisOutput output = new ChatAnalysisOutput(
                    Topic.RELATIONSHIP, Emotion.SAD, Intent.VENT,
                    List.of(), 2, 0.85,
                    List.of(), 10L, null,
                    com.mindbridge.analysis.provider.AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, output, NOW);

            assertThat(row.getTopic()).isEqualTo("RELATIONSHIP");
            assertThat(row.getEmotion()).isEqualTo("SAD");
            assertThat(row.getIntent()).isEqualTo("VENT");
        }

        @Test
        @DisplayName("maps signals List to JSONB string array")
        void create_mapsSignalsToArray() {
            ChatAnalysisOutput output = new ChatAnalysisOutput(
                    Topic.WORK_STRESS, Emotion.ANXIOUS, Intent.VENT,
                    List.of(Signal.BURNOUT, Signal.FATIGUE), 2, 0.7,
                    List.of(), 8L, null,
                    com.mindbridge.analysis.provider.AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, output, NOW);

            String[] signals = row.getSignals();
            assertThat(signals).containsExactly("BURNOUT", "FATIGUE");
        }

        @Test
        @DisplayName("maps empty signals to empty JSONB array")
        void create_emptySignals_yieldsEmptyArray() {
            ChatAnalysisOutput output = okOutput();
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, output, NOW);

            assertThat(row.getSignals()).isEmpty();
            assertThat(row.getSignalsAsList()).isEmpty();
        }

        @Test
        @DisplayName("maps evidenceSpans to JSON string array")
        void create_mapsEvidenceSpans() {
            EvidenceSpan span1 = new EvidenceSpan(0, 5, "a".repeat(64));
            EvidenceSpan span2 = new EvidenceSpan(10, 20, "b".repeat(64));
            ChatAnalysisOutput output = new ChatAnalysisOutput(
                    Topic.HEALTH, Emotion.DISTRESS, Intent.SUPPORT,
                    List.of(), 3, 0.6,
                    List.of(span1, span2), 20L, null,
                    com.mindbridge.analysis.provider.AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, output, NOW);

            String[] spans = row.getEvidenceSpans();
            assertThat(spans).hasSize(2);
            assertThat(spans[0]).contains("\"start\":0");
            assertThat(spans[1]).contains("\"start\":10");
        }

        @Test
        @DisplayName("maps modelRiskLevel and confidence correctly")
        void create_mapsRiskAndConfidence() {
            ChatAnalysisOutput output = new ChatAnalysisOutput(
                    Topic.SLEEP, Emotion.OVERWHELMED, Intent.INFO,
                    List.of(Signal.SLEEP_DISRUPTION), 4, 0.99,
                    List.of(), 5L, null,
                    com.mindbridge.analysis.provider.AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, output, NOW);

            assertThat(row.getModelRiskLevel()).isEqualTo((short) 4);
            assertThat(row.getConfidence()).isEqualByComparingTo("0.99");
        }

        @Test
        @DisplayName("new row starts with ACTIVE status and null supersedesId")
        void create_newRow_isActiveWithNoSupersedes() {
            ChatAnalysisOutput output = okOutput();
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, output, NOW);

            assertThat(row.getAnalysisStatus()).isEqualTo(ResultAnalysisStatus.ACTIVE);
            assertThat(row.getSupersedesId()).isNull();
        }

        @Test
        @DisplayName("new row copies all id fields from parameters")
        void create_copiesAllIdentifiers() {
            ChatAnalysisOutput output = okOutput();
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, output, NOW);

            assertThat(row.getId()).isEqualTo(ID);
            assertThat(row.getAnalysisRunId()).isEqualTo(RUN_ID);
            assertThat(row.getConversationMessageId()).isEqualTo(MESSAGE_ID);
            assertThat(row.getUserId()).isEqualTo(USER_ID);
            assertThat(row.getCreatedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    class StateTransitions {

        @Test
        @DisplayName("markSuperseded: ACTIVE - SUPERSEDED, sets supersedesId")
        void markSuperseded_fromActive_succeeds() {
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, okOutput(), NOW);
            UUID newId = UUID.randomUUID();

            row.markSuperseded(newId);

            assertThat(row.getAnalysisStatus()).isEqualTo(ResultAnalysisStatus.SUPERSEDED);
            assertThat(row.getSupersedesId()).isEqualTo(newId);
        }

        @Test
        @DisplayName("markSuperseded from non-ACTIVE throws IllegalStateException")
        void markSuperseded_fromNonActive_throws() {
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, okOutput(), NOW);
            row.markSuperseded(UUID.randomUUID());

            assertThatThrownBy(() -> row.markSuperseded(UUID.randomUUID()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SUPERSEDED");
        }

        @Test
        @DisplayName("markInvalidated: ACTIVE - INVALIDATED")
        void markInvalidated_fromActive_succeeds() {
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, okOutput(), NOW);

            row.markInvalidated();

            assertThat(row.getAnalysisStatus()).isEqualTo(ResultAnalysisStatus.INVALIDATED);
        }

        @Test
        @DisplayName("markInvalidated from non-ACTIVE throws IllegalStateException")
        void markInvalidated_fromNonActive_throws() {
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, okOutput(), NOW);
            row.markInvalidated();

            assertThatThrownBy(() -> row.markInvalidated())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("INVALIDATED");
        }

        @Test
        @DisplayName("isAuthoritative: ACTIVE is true, others are false")
        void isAuthoritative_statusBased() {
            ChatAnalysisResult row = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, okOutput(), NOW);
            assertThat(row.getAnalysisStatus().isAuthoritative()).isTrue();

            row.markInvalidated();
            assertThat(row.getAnalysisStatus().isAuthoritative()).isFalse();
        }
    }

    @Nested
    class NoSetters {

        @Test
        @DisplayName("no public setter methods exist on the entity")
        void noPublicSetters() {
            Method[] methods = ChatAnalysisResult.class.getDeclaredMethods();

            for (Method m : methods) {
                if (m.getName().startsWith("set")) {
                    assertThat(m)
                            .matches(meth -> !java.lang.reflect.Modifier.isPublic(meth.getModifiers()),
                                    "setter " + m.getName() + " must not be public");
                }
            }
        }
    }

    @Nested
    class RiskBoundaryGuard {

        @Test
        @DisplayName("entity has model_risk_level but NOT final_risk_level or rule_risk_level")
        void noFinalOrRuleRiskLevelFields() {
            Field[] fields = ChatAnalysisResult.class.getDeclaredFields();
            java.util.Set<String> fieldNames = new java.util.HashSet<>();
            for (Field f : fields) {
                fieldNames.add(f.getName());
            }

            assertThat(fieldNames).contains("modelRiskLevel");
            assertThat(fieldNames).doesNotContain("finalRiskLevel", "final_risk_level",
                    "ruleRiskLevel", "rule_risk_level");
        }
    }

    @Nested
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals by id only")
        void equals_byId() {
            ChatAnalysisResult r1 = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, okOutput(), NOW);
            ChatAnalysisResult r2 = ChatAnalysisResult.create(ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), okOutput(), NOW);

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("hashCode consistent with equals")
        void hashCode_consistent() {
            ChatAnalysisResult r1 = ChatAnalysisResult.create(ID, RUN_ID, MESSAGE_ID, USER_ID, okOutput(), NOW);
            ChatAnalysisResult r2 = ChatAnalysisResult.create(ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), okOutput(), NOW);

            assertThat(r1).hasSameHashCodeAs(r2);
        }
    }

    private static ChatAnalysisOutput okOutput() {
        return new ChatAnalysisOutput(
                Topic.WORK_STRESS, Emotion.NEUTRAL, Intent.VENT,
                List.of(), 1, 0.72,
                List.of(), 15L, null,
                com.mindbridge.analysis.provider.AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
    }
}