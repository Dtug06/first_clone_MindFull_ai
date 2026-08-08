package com.mindbridge.devseed;

import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the seven-day trend dev seed.
 *
 * <p>Bound from {@code mindbridge.dev-seed.seven-day-trend.*} keys in
 * {@code application.yml}.
 *
 * <p>This seed is ENTIRELY separate from the G2-T09 {@code DevSeedService}.
 * It does NOT use the G2-T09 seed mechanism, user IDs, email domain, or
 * reset logic. It targets one dedicated test user and does NOT touch any
 * G2-T09 demo users.
 *
 * @param enabled       master switch - when {@code false} (default), the seed
 *                      bean is not loaded. Must be set explicitly in local config.
 * @param userEmail    email of the seed user. If missing, the seed fails
 *                      with a safe error. Format: any existing user email.
 * @param targetDate   last local date of the 7-day window (day 7). Defaults
 *                      to the current local date in Asia/Ho_Chi_Minh if unset.
 *                      Format: YYYY-MM-DD.
 */
@ConfigurationProperties(prefix = "mindbridge.dev-seed.seven-day-trend")
public record SevenDayTrendSeedProperties(
        boolean enabled,
        String userEmail,
        LocalDate targetDate
) {
    public SevenDayTrendSeedProperties {
        if (targetDate == null) {
            targetDate = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }
}