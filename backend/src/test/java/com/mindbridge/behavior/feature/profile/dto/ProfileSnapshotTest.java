package com.mindbridge.behavior.feature.profile.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.behavior.feature.engagement.dto.TopicFrequency;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileSnapshotTest {

    UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    LocalDate windowEnd = LocalDate.of(2026, 8, 4);
    OffsetDateTime calculatedAt = OffsetDateTime.now();

    private ProfileSnapshot validSnapshot(DataQualityStatus status) {
        return new ProfileSnapshot(
                userId, windowEnd,
                null, null, null, null, null, null, null, null, null, null,
                0, 0,
                null, List.of(), List.of(),
                null, null,
                BigDecimal.ZERO, BigDecimal.ZERO,
                status,
                calculatedAt);
    }

    @Test
    @DisplayName("valid input constructs successfully")
    void valid() {
        ProfileSnapshot s = validSnapshot(DataQualityStatus.SUFFICIENT);
        assertThat(s.userId()).isEqualTo(userId);
        assertThat(s.dominantTopics7d()).isEmpty();
        assertThat(s.dominantTopics30d()).isEmpty();
        assertThat(s.dataQualityStatus()).isEqualTo(DataQualityStatus.SUFFICIENT);
    }

    @Test
    @DisplayName("null userId rejected")
    void nullUserIdRejected() {
        assertThatThrownBy(() -> new ProfileSnapshot(
                null, windowEnd,
                null, null, null, null, null, null, null, null, null, null,
                0, 0,
                null, List.of(), List.of(),
                null, null,
                BigDecimal.ZERO, BigDecimal.ZERO,
                DataQualityStatus.SUFFICIENT,
                calculatedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    @DisplayName("null dataQualityStatus rejected")
    void nullDataQualityStatusRejected() {
        assertThatThrownBy(() -> new ProfileSnapshot(
                userId, windowEnd,
                null, null, null, null, null, null, null, null, null, null,
                0, 0,
                null, List.of(), List.of(),
                null, null,
                BigDecimal.ZERO, BigDecimal.ZERO,
                null,
                calculatedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dataQualityStatus");
    }

    @Test
    @DisplayName("dataCoverage must be in [0, 1]")
    void dataCoverageOutOfRange() {
        assertThatThrownBy(() -> new ProfileSnapshot(
                userId, windowEnd,
                null, null, null, null, null, null, null, null, null, null,
                0, 0,
                null, List.of(), List.of(),
                null, null,
                new BigDecimal("1.5"), BigDecimal.ZERO,
                DataQualityStatus.SUFFICIENT,
                calculatedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dataCoverage");
    }

    @Test
    @DisplayName("engagementScore > 3 rejected")
    void engagementOutOfRange() {
        assertThatThrownBy(() -> new ProfileSnapshot(
                userId, windowEnd,
                null, null, null, null, null, null, null, null, null, null,
                4, 0,
                null, List.of(), List.of(),
                null, null,
                BigDecimal.ZERO, BigDecimal.ZERO,
                DataQualityStatus.SUFFICIENT,
                calculatedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("engagementScore7d");
    }

    @Test
    @DisplayName("riskLevel > 4 rejected")
    void riskLevelOutOfRange() {
        assertThatThrownBy(() -> new ProfileSnapshot(
                userId, windowEnd,
                null, null, null, null, null, null, null, null, null, null,
                0, 0,
                null, List.of(), List.of(),
                (short) 5, null,
                BigDecimal.ZERO, BigDecimal.ZERO,
                DataQualityStatus.SUFFICIENT,
                calculatedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riskLevel");
    }

    @Test
    @DisplayName("null dominant topics become empty lists (defensive copy)")
    void nullTopicsBecomeEmpty() {
        ProfileSnapshot s = new ProfileSnapshot(
                userId, windowEnd,
                null, null, null, null, null, null, null, null, null, null,
                0, 0,
                null, null, null,
                null, null,
                BigDecimal.ZERO, BigDecimal.ZERO,
                DataQualityStatus.LOW,
                calculatedAt);
        assertThat(s.dominantTopics7d()).isEmpty();
        assertThat(s.dominantTopics30d()).isEmpty();
    }

    @Test
    @DisplayName("dominant topics list is defensively copied (immutable)")
    void topicsAreDefensivelyCopied() {
        List<TopicFrequency> mutableList = new java.util.ArrayList<>();
        mutableList.add(new TopicFrequency("WORK_STRESS", 5L, 1.0));
        ProfileSnapshot s = new ProfileSnapshot(
                userId, windowEnd,
                null, null, null, null, null, null, null, null, null, null,
                0, 0,
                null, mutableList, mutableList,
                null, null,
                BigDecimal.ZERO, BigDecimal.ZERO,
                DataQualityStatus.SUFFICIENT,
                calculatedAt);
        mutableList.add(new TopicFrequency("SLEEP", 3L, 0.5));
        assertThat(s.dominantTopics7d()).hasSize(1);
        assertThat(s.dominantTopics30d()).hasSize(1);
    }

    @Test
    @DisplayName("all three DataQualityStatus values are accepted")
    void allDataQualityStatusValuesAccepted() {
        for (DataQualityStatus status : DataQualityStatus.values()) {
            ProfileSnapshot s = validSnapshot(status);
            assertThat(s.dataQualityStatus()).isEqualTo(status);
        }
    }
}