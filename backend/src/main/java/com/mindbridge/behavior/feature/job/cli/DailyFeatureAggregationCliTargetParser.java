package com.mindbridge.behavior.feature.job.cli;

import java.time.LocalDate;
import java.util.UUID;

public final class DailyFeatureAggregationCliTargetParser {
    private DailyFeatureAggregationCliTargetParser() {}

    public static DailyFeatureAggregationCliTarget parse(String raw) {
        if (raw == null || raw.isBlank()) {

            throw new IllegalArgumentException("CLI target must not be blank");
        }
        String[] parts = raw.trim().split(":");
        if (parts.length < 2) throw new IllegalArgumentException("Invalid CLI target format: " + raw);
        switch (parts[0].toUpperCase()) {
            case "ALL" -> {

                if (parts.length != 2) throw new IllegalArgumentException("ALL requires DATE");
                return DailyFeatureAggregationCliTarget.forAllUsers(parseDate(parts[1]));
            }
            case "USER" -> {

                if (parts.length != 4) throw new IllegalArgumentException("USER requires UUID:DATE:DATE");
                return DailyFeatureAggregationCliTarget.forUser(parseUuid(parts[1]), parseDate(parts[2]), parseDate(parts[3]));
            }
            default -> throw new IllegalArgumentException("Unknown target kind: " + parts[0]);
        }
    }

    private static LocalDate parseDate(String s) {
        try { return LocalDate.parse(s); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid date: " + s, e); }
    }

    private static UUID parseUuid(String s) {
        try { return UUID.fromString(s); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid UUID: " + s, e); }
    }
}
