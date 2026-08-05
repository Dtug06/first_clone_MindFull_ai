package com.mindbridge.behavior.feature.job.cli;

import com.mindbridge.behavior.feature.job.DailyFeatureAggregationService;
import com.mindbridge.behavior.feature.job.dto.JobRunSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mindbridge.feature-aggregation.run.enabled", havingValue = "true")
public class DailyFeatureAggregationCliRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DailyFeatureAggregationCliRunner.class);
    private final DailyFeatureAggregationService service;
    private final DailyFeatureAggregationCliProperties props;

    public DailyFeatureAggregationCliRunner(DailyFeatureAggregationService service,
            DailyFeatureAggregationCliProperties props) {
        this.service = service;
        this.props = props;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            log.warn("No CLI arguments provided. Use --target=ALL:YYYY-MM-DD or USER:<uuid>:YYYY-MM-DD:YYYY-MM-DD");
            return;
        }
        String raw = args[0].replaceFirst("^--target=", "");
        DailyFeatureAggregationCliTarget target = DailyFeatureAggregationCliTargetParser.parse(raw);
        if (!target.isValid()) {
            throw new IllegalArgumentException("Invalid CLI target: " + raw);
        }
        log.info("G4-T05 CLI run starting: kind={}", target.kind());
        JobRunSummary summary = switch (target.kind()) {
            case ALL_USERS_FOR_DATE -> service.aggregateAllForDate(target.dateFrom());
            case SINGLE_USER_DATE_RANGE -> service.aggregateSingleUserForDateRange(target.userId(), target.dateFrom(), target.dateTo());
        };
        log.info("G4-T05 CLI run finished: status={} attempted={} succeeded={} failed={}",
                summary.status(), summary.usersAttempted(), summary.usersSucceeded(), summary.usersFailed());
    }
}
