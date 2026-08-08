package com.mindbridge.behavior.feature.impl;

import com.mindbridge.analysis.result.domain.ChatAnalysisResult;
import com.mindbridge.analysis.result.domain.ResultAnalysisStatus;
import com.mindbridge.analysis.result.repository.ChatAnalysisResultRepository;
import com.mindbridge.behavior.feature.DailySourceAggregationService;
import com.mindbridge.behavior.feature.dto.BehavioralEventCountsRow;
import com.mindbridge.behavior.feature.dto.CbtAvailability;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation.BehavioralEventCounts;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation.CbtAggregation;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation.EffectiveChatAnalysis;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation.ExplicitAnswer;
import com.mindbridge.behavior.repository.BehavioralEventRepository;
import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import com.mindbridge.dailyquestion.repository.DailyQuestionAnswerRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionAssignmentRepository;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * G4-T03: Default implementation of {@link DailySourceAggregationService}.
 *
 * Read-only. The method runs inside a single
 * @Transactional(readOnly = true) so all four source-table reads see a
 * consistent snapshot. Late-arriving answers are correctly attributed by
 * joining through the assignment (see Q1 policy).
 *
 * CBT runtime availability is detected ONCE at service start-up via a SQL
 * probe against information_schema.tables. When G5 ships and creates the
 * exercise_assignments table, the probe flips to true automatically without
 * any code change (Q2 policy).
 */
@Service
public class DailySourceAggregationServiceImpl implements DailySourceAggregationService {

    private static final Logger log = LoggerFactory.getLogger(DailySourceAggregationServiceImpl.class);

    private static final String CBT_TABLE_NAME = "exercise_assignments";

    private final DailyQuestionAnswerRepository answerRepository;
    private final DailyQuestionAssignmentRepository assignmentRepository;
    private final ChatAnalysisResultRepository chatAnalysisRepository;
    private final BehavioralEventRepository behavioralEventRepository;
    private final DataSource dataSource;

    private volatile boolean cbtShipped = true;

    public DailySourceAggregationServiceImpl(
            DailyQuestionAnswerRepository answerRepository,
            DailyQuestionAssignmentRepository assignmentRepository,
            ChatAnalysisResultRepository chatAnalysisRepository,
            BehavioralEventRepository behavioralEventRepository,
            DataSource dataSource) {
        this.answerRepository = answerRepository;
        this.assignmentRepository = assignmentRepository;
        this.chatAnalysisRepository = chatAnalysisRepository;
        this.behavioralEventRepository = behavioralEventRepository;
        this.dataSource = dataSource;
    }

    @PostConstruct
    void detectCbtRuntime() {
        boolean shipped = probeCbtTableExists();
        this.cbtShipped = shipped;
        if (shipped) {
            log.info("G4-T03: CBT runtime detected (table={} present).", CBT_TABLE_NAME);
        } else {
            log.info("G4-T03: CBT runtime NOT detected (table={} absent). cbtAvailability=NOT_SHIPPED.", CBT_TABLE_NAME);
        }
    }

    private boolean probeCbtTableExists() {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, CBT_TABLE_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (Exception e) {
            log.warn("G4-T03: CBT runtime probe failed, assuming NOT_SHIPPED. cause={}", e.toString());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DailySourceAggregation aggregateForDay(UUID userId, String timezone, LocalDate localDate) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("timezone must not be null or blank");
        }
        if (localDate == null) {
            throw new IllegalArgumentException("localDate must not be null");
        }

        final ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid IANA timezone: " + timezone, e);
        }

        OffsetDateTime windowStartUtc = localDate.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime windowEndUtc = localDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
        Instant fromUtc = windowStartUtc.toInstant();
        Instant toUtc = windowEndUtc.toInstant();

        List<ExplicitAnswer> explicitAnswers = aggregateExplicitAnswers(userId, localDate);
        List<EffectiveChatAnalysis> effectiveChatAnalyses = aggregateChatAnalyses(userId, windowStartUtc, windowEndUtc);
        BehavioralEventCounts behavioralCounts = aggregateBehavioralEvents(
                userId, localDate, fromUtc, toUtc);
        CbtAvailability cbtAvailability = resolveCbtAvailability();
        CbtAggregation cbtActivity = (cbtAvailability == CbtAvailability.COMPUTABLE)
                ? aggregateCbtActivity()
                : CbtAggregation.empty();

        if (log.isDebugEnabled()) {
            log.debug("G4-T03 aggregate: userId={} localDate={} tz={} answers={} chatAnalyses={} msgCount={} cbt={}",
                    userId, localDate, timezone,
                    explicitAnswers.size(), effectiveChatAnalyses.size(),
                    behavioralCounts.chatMessageCount(),
                    cbtAvailability);
        }

        return new DailySourceAggregation(
                userId, timezone, localDate,
                windowStartUtc, windowEndUtc,
                explicitAnswers, effectiveChatAnalyses,
                behavioralCounts, cbtAvailability, cbtActivity);
    }

    private List<ExplicitAnswer> aggregateExplicitAnswers(UUID userId, LocalDate localDate) {
        List<DailyQuestionAnswer> rows =
                answerRepository.findWithAssignmentByUserIdAndAssignedForDate(userId, localDate);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(a -> ExplicitAnswer.of(a, a.getAssignment()))
                .toList();
    }

    private List<EffectiveChatAnalysis> aggregateChatAnalyses(UUID userId, OffsetDateTime fromUtc, OffsetDateTime toUtc) {
        List<ChatAnalysisResult> rows = chatAnalysisRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, fromUtc, toUtc);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .filter(r -> r.getAnalysisStatus() != null && r.getAnalysisStatus().isAuthoritative())
                .map(EffectiveChatAnalysis::of)
                .toList();
    }

    private BehavioralEventCounts aggregateBehavioralEvents(
            UUID userId, LocalDate localDate, Instant fromUtc, Instant toUtc) {
        BehavioralEventCountsRow row = behavioralEventRepository.aggregateByUserAndDay(userId, fromUtc, toUtc);
        long chatMsg = (row == null) ? 0L : row.getChatMessageCount();
        long sessions = (row == null) ? 0L : row.getActiveChatSessionCount();
        long completed = (row == null) ? 0L : row.getCheckinCompletedCount();
        long skipped = (row == null) ? 0L : row.getCheckinSkippedCount();
        long assigned = assignmentRepository.countByUserIdAndAssignedForDate(userId, localDate);
        return new BehavioralEventCounts(chatMsg, sessions, completed, skipped, assigned);
    }

    private CbtAvailability resolveCbtAvailability() {
        if (!cbtShipped) {
            return CbtAvailability.NOT_SHIPPED;
        }
        return CbtAvailability.NOT_APPLICABLE;
    }

    private CbtAggregation aggregateCbtActivity() {
        return CbtAggregation.empty();
    }
}
