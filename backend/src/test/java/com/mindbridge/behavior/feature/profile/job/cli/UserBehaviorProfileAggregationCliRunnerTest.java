package com.mindbridge.behavior.feature.profile.job.cli;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.mindbridge.behavior.feature.profile.job.UserBehaviorProfileAggregationJobService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserBehaviorProfileAggregationCliRunnerTest {

    @Mock UserBehaviorProfileAggregationJobService service;

    @Test
    void runsOneUserForDate() {
        UUID userId = UUID.randomUUID();
        var runner = new UserBehaviorProfileAggregationCliRunner(
                service, "USER:" + userId + ":2026-08-06");

        runner.run();

        verify(service).aggregateOneUser(userId, LocalDate.of(2026, 8, 6));
    }

    @Test
    void runsAllUsersForDate() {
        var runner = new UserBehaviorProfileAggregationCliRunner(
                service, "ALL:2026-08-06");

        runner.run();

        verify(service).aggregateAllForDate(LocalDate.of(2026, 8, 6));
    }

    @Test
    void rejectsInvalidTarget() {
        var runner = new UserBehaviorProfileAggregationCliRunner(service, "bad");

        assertThatThrownBy(runner::run)
                .isInstanceOf(IllegalArgumentException.class);
    }
}
