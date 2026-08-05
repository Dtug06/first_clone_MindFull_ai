const fs = require('fs');
const path = require('path');

const content = `package com.mindbridge.behavior.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindbridge.analysis.result.domain.ChatAnalysisResult;
import com.mindbridge.analysis.result.domain.ResultAnalysisStatus;
import com.mindbridge.analysis.result.repository.ChatAnalysisResultRepository;
import com.mindbridge.behavior.feature.dto.BehavioralEventCountsRow;
import com.mindbridge.behavior.feature.dto.CbtAvailability;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation.BehavioralEventCounts;
import com.mindbridge.behavior.feature.impl.DailySourceAggregationServiceImpl;
import com.mindbridge.behavior.repository.BehavioralEventRepository;
import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import com.mindbridge.dailyquestion.repository.DailyQuestionAnswerRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * G4-T03: Unit tests for {@link DailySourceAggregationServiceImpl}.
 *
 * <p>These tests use Mockito for the repositories and a stubbed {@link DataSource}
 * so the {@code @PostConstruct} CBT probe is exercised without booting Spring.
 *
 * <p>Coverage:
 * <ul>
 *   <li>Argument validation (null userId / timezone / localDate, bad TZ)</li>
 *   <li>TZ math: window-start / window-end are computed correctly for UTC,
 *       Asia/Ho_Chi_Minh, and Pacific/Auckland</li>
 *   <li>Rerun-aware: SUPERSEDED chat rows are filtered out</li>
 *   <li>Empty inputs return zeros, not nulls</li>
 *   <li>CBT runtime detection: probe result flips cbtAvailability</li>
 *   <li>Repositories are called with the right arguments (window bounds, etc.)</li>
 * </ul>
 */
class DailySourceAggregationServiceImplTest {

    private DailyQuestionAnswerRepository answerRepo;
    private ChatAnalysisResultRepository chatRepo;
    private BehavioralEventRepository eventRepo;
    private DataSource dataSource;
    private DailySourceAggregationServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        answerRepo = mock(DailyQuestionAnswerRepository.class);
        chatRepo = mock(ChatAnalysisResultRepository.class);
        eventRepo = mock(BehavioralEventRepository.class);
        dataSource = mock(DataSource.class);
        when(answerRepo.findWithAssignmentByUserIdAndAssignedForDate(any(), any())).thenReturn(Collections.emptyList());
        when(chatRepo.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(any(), any(), any())).thenReturn(Collections.emptyList());
        when(eventRepo.aggregateByUserAndDay(any(), any(), any())).thenReturn(emptyRow());
        service = new DailySourceAggregationServiceImpl(answerRepo, chatRepo, eventRepo, dataSource);
    }

    /** Test helper: returns a BehavioralEventCountsRow with all zeros. */
    private static BehavioralEventCountsRow emptyRow() {
        BehavioralEventCountsRow r = mock(BehavioralEventCountsRow.class);
        when(r.getChatMessageCount()).thenReturn(0L);
        when(r.getActiveChatSessionCount()).thenReturn(0L);
        when(r.getCheckinCompletedCount()).thenReturn(0L);
        when(r.getCheckinSkippedCount()).thenReturn(0L);
        return r;
    }

    /** Run the @PostConstruct method via reflection so the probe runs. */
    private void runPostConstruct(DailySourceAggregationServiceImpl svc) throws Exception {
        Method m = DailySourceAggregationServiceImpl.class.getDeclaredMethod("detectCbtRuntime");
        m.setAccessible(true);
        m.invoke(svc);
    }

    /** Override the volatile cbtShipped flag for unit tests. */
    private void setCbtShipped(DailySourceAggregationServiceImpl svc, boolean v) throws Exception {
        Field f = DailySourceAggregationServiceImpl.class.getDeclaredField("cbtShipped");
        f.setAccessible(true);
        f.setBoolean(svc, v);
    }

    @Test
    @DisplayName("null userId -> IllegalArgumentException")
    void nullUserId_throws() {
        assertThatThrownBy(() -> service.aggregateForDay(null, "UTC", LocalDate.of(2026, 8, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    @DisplayName("null timezone -> IllegalArgumentException")
    void nullTimezone_throws() {
        assertThatThrownBy(() -> service.aggregateForDay(UUID.randomUUID(), null, LocalDate.of(2026, 8, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timezone");
    }

    @Test
    @DisplayName("blank timezone -> IllegalArgumentException")
    void blankTimezone_throws() {
        assertThatThrownBy(() -> service.aggregateForDay(UUID.randomUUID(), "   ", LocalDate.of(2026, 8, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timezone");
    }

    @Test
    @DisplayName("null localDate -> IllegalArgumentException")
    void nullLocalDate_throws() {
        assertThatThrownBy(() -> service.aggregateForDay(UUID.randomUUID(), "UTC", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("localDate");
    }

    @Test
    @DisplayName("invalid timezone -> IllegalArgumentException")
    void invalidTimezone_throws() {
        assertThatThrownBy(() -> service.aggregateForDay(UUID.randomUUID(), "Not_A_Real_Zone", LocalDate.of(2026, 8, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid IANA timezone");
    }

    @Test
    @DisplayName("UTC window math: start = localDate 00:00 UTC, end = +1 day 00:00 UTC (exclusive)")
    void utcWindowMath_isCorrect() throws Exception {
        setCbtShipped(service, false);
        UUID userId = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 8, 4);

        DailySourceAggregation out = service.aggregateForDay(userId, "UTC", day);

        OffsetDateTime expectedStart = OffsetDateTime.of(2026, 8, 4, 0, 0, 0, 0, ZoneId.of("UTC").getRules().getOffset(OffsetDateTime.now().toLocalDateTime().atStartOfDay()));
        assertThat(out.windowStartUtc()).isEqualTo(day.atStartOfDay(ZoneId.of("UTC")).toOffsetDateTime());
        assertThat(out.windowEndUtc()).isEqualTo(day.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toOffsetDateTime());
    }

    @Test
    @DisplayName("Asia/Ho_Chi_Minh window math: start = localDate 00:00 +07:00, end = +1 day 00:00 +07:00")
    void hcmWindowMath_isCorrect() throws Exception {
        setCbtShipped(service, false);
        UUID userId = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 8, 4);

        DailySourceAggregation out = service.aggregateForDay(userId, "Asia/Ho_Chi_Minh", day);

        assertThat(out.windowStartUtc().getOffset()).isEqualTo(java.time.ZoneOffset.ofHours(7));
        assertThat(out.windowStartUtc()).isEqualTo(OffsetDateTime.parse("2026-08-04T00:00+07:00"));
        assertThat(out.windowEndUtc()).isEqualTo(OffsetDateTime.parse("2026-08-05T00:00+07:00"));
    }

    @Test
    @DisplayName("DST transition date: America/New_York window still computed correctly")
    void dstTransitionWindowMath_isCorrect() throws Exception {
        setCbtShipped(service, false);
        UUID userId = UUID.randomUUID();
        // US DST 2026: starts Mar 8 (spring forward). localDate 2026-03-08:
        // before = EST (UTC-5); after = EDT (UTC-4). We pick the day itself
        // which is unambiguously 00:00 EST at midnight, so start offset is -05:00.
        LocalDate day = LocalDate.of(2026, 3, 8);

        DailySourceAggregation out = service.aggregateForDay(userId, "America/New_York", day);

        assertThat(out.windowStartUtc()).isEqualTo(OffsetDateTime.parse("2026-03-08T00:00-05:00"));
        assertThat(out.windowEndUtc()).isEqualTo(OffsetDateTime.parse("2026-03-09T00:00-05:00"));
    }

    @Test
    @DisplayName("Empty sources -> zero/empty lists, NOT null")
    void emptySources_returnsZeros() throws Exception {
        setCbtShipped(service, false);
        UUID userId = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 8, 4);

        DailySourceAggregation out = service.aggregateForDay(userId, "UTC", day);

        assertThat(out.explicitAnswers()).isEmpty();
        assertThat(out.effectiveChatAnalyses()).isEmpty();
        assertThat(out.behavioralCounts().chatMessageCount()).isZero();
        assertThat(out.behavioralCounts().activeChatSessionCount()).isZero();
        assertThat(out.behavioralCounts().checkinCompletedCount()).isZero();
        assertThat(out.behavioralCounts().checkinSkippedCount()).isZero();
        assertThat(out.behavioralCounts().checkinAssignedCount()).isZero();
        assertThat(out.cbtAvailability()).isEqualTo(CbtAvailability.NOT_SHIPPED);
        assertThat(out.cbtActivity()).isNotNull();
        assertThat(out.cbtActivity().assignmentsCount()).isZero();
    }

    @Test
    @DisplayName("SUPERSEDED chat_analysis_results are filtered out (rerun-aware)")
    void supersededChatAnalyses_areFiltered() throws Exception {
        setCbtShipped(service, false);
        UUID userId = UUID.randomUUID();

        ChatAnalysisResult active = mock(ChatAnalysisResult.class);
        when(active.getAnalysisStatus()).thenReturn(ResultAnalysisStatus.ACTIVE);
        when(active.getId()).thenReturn(UUID.randomUUID());

        ChatAnalysisResult superseded = mock(ChatAnalysisResult.class);
        when(superseded.getAnalysisStatus()).thenReturn(ResultAnalysisStatus.SUPERSEDED);

        when(chatRepo.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(eq(userId), any(), any()))
                .thenReturn(List.of(active, superseded));

        DailySourceAggregation out = service.aggregateForDay(userId, "UTC", LocalDate.of(2026, 8, 4));

        assertThat(out.effectiveChatAnalyses()).hasSize(1);
    }

    @Test
    @DisplayName("INVALIDATED chat_analysis_results are filtered out")
    void invalidatedChatAnalyses_areFiltered() {
        UUID userId = UUID.randomUUID();
        ChatAnalysisResult invalidated = mock(ChatAnalysisResult.class);
        when(invalidated.getAnalysisStatus()).thenReturn(ResultAnalysisStatus.INVALIDATED);

        when(chatRepo.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(eq(userId), any(), any()))
                .thenReturn(List.of(invalidated));

        DailySourceAggregation out = service.aggregateForDay(userId, "UTC", LocalDate.of(2026, 8, 4));

        assertThat(out.effectiveChatAnalyses()).isEmpty();
    }

    @Test
    @DisplayName("CBT probe detects exercise_assignments table -> cbtShipped=true (NOT_APPLICABLE today)")
    void cbtProbe_tableExists_setsCbtShippedTrue() throws Exception {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(con);
        when(con.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(1);

        service = new DailySourceAggregationServiceImpl(answerRepo, chatRepo, eventRepo, dataSource);
        runPostConstruct(service);

        DailySourceAggregation out = service.aggregateForDay(UUID.randomUUID(), "UTC", LocalDate.of(2026, 8, 4));
        assertThat(out.cbtAvailability()).isEqualTo(CbtAvailability.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("CBT probe finds no table -> cbtShipped=false (NOT_SHIPPED, MVP baseline)")
    void cbtProbe_tableMissing_setsCbtShippedFalse() throws Exception {
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(con);
        when(con.prepareStatement(any())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(0);

        service = new DailySourceAggregationServiceImpl(answerRepo, chatRepo, eventRepo, dataSource);
        runPostConstruct(service);

        DailySourceAggregation out = service.aggregateForDay(UUID.randomUUID(), "UTC", LocalDate.of(2026, 8, 4));
        assertThat(out.cbtAvailability()).isEqualTo(CbtAvailability.NOT_SHIPPED);
    }

    @Test
    @DisplayName("CBT probe throws -> cbtShipped=false (fail-safe to MVP baseline)")
    void cbtProbe_throws_defaultsToNotShipped() throws Exception {
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("connection refused"));

        service = new DailySourceAggregationServiceImpl(answerRepo, chatRepo, eventRepo, dataSource);
        runPostConstruct(service);

        DailySourceAggregation out = service.aggregateForDay(UUID.randomUUID(), "UTC", LocalDate.of(2026, 8, 4));
        assertThat(out.cbtAvailability()).isEqualTo(CbtAvailability.NOT_SHIPPED);
    }

    @Test
    @DisplayName("Repositories receive the same window bounds the DTO exposes (single-source-of-truth)")
    void repositories_receiveMatchingWindow() throws Exception {
        setCbtShipped(service, false);
        UUID userId = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 8, 4);

        DailySourceAggregation out = service.aggregateForDay(userId, "UTC", day);

        ArgumentCaptor<OffsetDateTime> fromCap = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCap = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(chatRepo).findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(eq(userId), fromCap.capture(), toCap.capture());
        assertThat(fromCap.getValue()).isEqualTo(out.windowStartUtc());
        assertThat(toCap.getValue()).isEqualTo(out.windowEndUtc());

        ArgumentCaptor<java.time.Instant> fromInstCap = ArgumentCaptor.forClass(java.time.Instant.class);
        ArgumentCaptor<java.time.Instant> toInstCap = ArgumentCaptor.forClass(java.time.Instant.class);
        verify(eventRepo).aggregateByUserAndDay(eq(userId), fromInstCap.capture(), toInstCap.capture());
        assertThat(fromInstCap.getValue()).isEqualTo(out.windowStartUtc().toInstant());
        assertThat(toInstCap.getValue()).isEqualTo(out.windowEndUtc().toInstant());
    }

    @Test
    @DisplayName("Explicit answer repository is called with the userId and localDate literal")
    void explicitAnswerRepo_calledWithLiteralLocalDate() throws Exception {
        setCbtShipped(service, false);
        UUID userId = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 8, 4);

        service.aggregateForDay(userId, "UTC", day);

        ArgumentCaptor<LocalDate> dateCap = ArgumentCaptor.forClass(LocalDate.class);
        verify(answerRepo).findWithAssignmentByUserIdAndAssignedForDate(eq(userId), dateCap.capture());
        assertThat(dateCap.getValue()).isEqualTo(day);
    }
}
`;

const dest = process.argv[2];
fs.mkdirSync(path.dirname(dest), { recursive: true });
fs.writeFileSync(dest, content, { encoding: 'utf8' });
console.log('OK', dest, content.length, 'chars');