package com.mindbridge.devseed;

import com.mindbridge.dailyquestion.domain.AnswerType;
import com.mindbridge.dailyquestion.domain.QuestionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic generator of per-user-per-day answer values for the G2-T09 dev seed.
 *
 * <p>Each call to {@link #plan(DemoUsers.Group, int)} returns the value to record
 * for one (user-group, day-index) pair, where {@code dayIndex=0} is the oldest
 * day in the 30-day window and {@code dayIndex=29} is the newest.
 *
 * <p>Patterns (Phase 1 plan §3.1 + §3.2):
 * <ul>
 *   <li>STRESS_TRENDING_UP   — stress climbs linearly from 1 → 5 over 30 days.</li>
 *   <li>STRESS_TRENDING_DOWN — stress falls linearly from 5 → 1 over 30 days.</li>
 *   <li>STABLE_LOW_STRESS    — stress stable in {1, 2}.</li>
 *   <li>STABLE_HIGH_STRESS   — stress stable in {4, 5}.</li>
 *   <li>RECOVERY_PATTERN     — stress high 4-5 for 14 days, then drops to 1-2.</li>
 *   <li>SPORADIC             — same trajectory as STABLE_LOW_STRESS, but ~30%
 *       of days are skipped (the caller checks {@link #shouldAnswer} before
 *       writing an answer row).</li>
 * </ul>
 *
 * <h2>Determinism</h2>
 * <p>All values are computed from {@code (group, dayIndex)} with no randomness
 * source — the same inputs always produce the same outputs. The seed caller
 * should iterate {@code dayIndex} from 0..29 and ask for the value of each
 * template code per day.
 */
public final class DemoCheckinPlan {

    /** Total window length in days. Phase 1 §3.1 says 7-30, we use 30 to give G4 trend room. */
    public static final int WINDOW_DAYS = 30;

    /** MoOd option values for SINGLE_CHOICE 1..5 (matches V6 seed). */
    private static final List<String> MOOD_OPTIONS = List.of("1", "2", "3", "4", "5");

    /** OPEN free-text pool — neutral sentences, never crisis language. */
    private static final List<String> OPEN_POOL = List.of(
            "Today felt manageable overall.",
            "I had a normal day with the usual routine.",
            "Nothing special to report today.",
            "It was a quiet day, mostly catching up on small tasks.",
            "I spent some time on a hobby after work.",
            "I noticed the weather was nice and took a short walk.",
            "I caught up with a friend over a quick call.",
            "I am looking forward to the weekend."
    );

    private DemoCheckinPlan() {
    }

    /**
     * Returns whether the given group should answer the check-in on day index
     * {@code dayIndex}. SPORADIC users skip ~30% of days; all other groups
     * answer every day.
     */
    public static boolean shouldAnswer(DemoUsers.Group group, int dayIndex) {
        if (group != DemoUsers.Group.SPORADIC) {
            return true;
        }
        // Deterministic skip pattern: skip on dayIndex % 10 == 3, 6, 9.
        int mod = dayIndex % 10;
        return mod != 3 && mod != 6 && mod != 9;
    }

    /**
     * Returns a list of {@code (templateCode, answer)} entries for the given
     * group on the given day index. The order is: STRESS, MOOD, SLEEP, ENERGY,
     * OPEN (alphabetic by template code as required by the existing
     * {@code findByUserIdAndAssignedForDateOrderByTemplateCodeAsc} query).
     */
    public static List<Entry> plan(DemoUsers.Group group, int dayIndex) {
        if (dayIndex < 0 || dayIndex >= WINDOW_DAYS) {
            throw new IllegalArgumentException("dayIndex out of range: " + dayIndex);
        }
        Map<String, Answer> byCode = new LinkedHashMap<>();
        byCode.put("STRESS", stressValue(group, dayIndex));
        byCode.put("MOOD", moodValue(group, dayIndex));
        byCode.put("SLEEP", sleepValue(group, dayIndex));
        byCode.put("ENERGY", energyValue(group, dayIndex));
        byCode.put("OPEN", openValue(dayIndex));

        List<Entry> result = new ArrayList<>(byCode.size());
        for (Map.Entry<String, Answer> e : byCode.entrySet()) {
            result.add(new Entry(e.getKey(), e.getValue()));
        }
        return result;
    }

    // --- per-template generators ---

    private static Answer stressValue(DemoUsers.Group group, int dayIndex) {
        BigDecimal v = switch (group) {
            case STRESS_TRENDING_UP -> interpolate(dayIndex, 1, 5);
            case STRESS_TRENDING_DOWN -> interpolate(dayIndex, 5, 1);
            case STABLE_LOW_STRESS -> choose(dayIndex, 1, 2);
            case STABLE_HIGH_STRESS -> choose(dayIndex, 4, 5);
            case RECOVERY_PATTERN -> dayIndex < 14 ? choose(dayIndex, 4, 5) : choose(dayIndex, 1, 2);
            case SPORADIC -> choose(dayIndex, 1, 2);
        };
        // Clamp into [1, 5] — rounding of interpolation at the 30-day boundaries
        // can produce 0.97 or 5.03, which would fail the SCALE range check.
        double clamped = Math.max(1.0, Math.min(5.0, v.doubleValue()));
        return new Answer(AnswerType.NUMERIC, QuestionType.SCALE,
                BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP), null, null);
    }

    private static Answer moodValue(DemoUsers.Group group, int dayIndex) {
        // Mood is the inverse of stress pattern, mapped to option value 1..5.
        int stressLike = switch (group) {
            case STRESS_TRENDING_UP -> lerpInt(dayIndex, 1, 5);
            case STRESS_TRENDING_DOWN -> lerpInt(dayIndex, 5, 1);
            case STABLE_LOW_STRESS -> 1;
            case STABLE_HIGH_STRESS -> 2;
            case RECOVERY_PATTERN -> dayIndex < 14 ? 2 : 4;
            case SPORADIC -> 3;
        };
        // Clamp into [1, 5] defensively — rounding can produce 0 or 6 at edges.
        int clamped = Math.max(1, Math.min(5, stressLike));
        int optionIndex = 5 - clamped; // 1 -> "5" (great), 5 -> "1" (bad)
        String optionValue = MOOD_OPTIONS.get(optionIndex);
        return new Answer(AnswerType.OPTION, QuestionType.SINGLE_CHOICE, null, null, optionValue);
    }

    private static Answer sleepValue(DemoUsers.Group group, int dayIndex) {
        BigDecimal hours = switch (group) {
            case STRESS_TRENDING_UP -> BigDecimal.valueOf(8).subtract(interpolate(dayIndex, 0, 2));
            case STRESS_TRENDING_DOWN -> BigDecimal.valueOf(5).add(interpolate(dayIndex, 0, 2));
            case STABLE_LOW_STRESS -> BigDecimal.valueOf(8);
            case STABLE_HIGH_STRESS -> BigDecimal.valueOf(5).add(choose(dayIndex, 0, 1));
            case RECOVERY_PATTERN -> dayIndex < 14
                    ? BigDecimal.valueOf(5).add(choose(dayIndex, 0, 1))
                    : BigDecimal.valueOf(7);
            case SPORADIC -> BigDecimal.valueOf(7);
        };
        return new Answer(AnswerType.NUMERIC, QuestionType.NUMBER, hours.setScale(1, RoundingMode.HALF_UP), null, null);
    }

    private static Answer energyValue(DemoUsers.Group group, int dayIndex) {
        BigDecimal v = switch (group) {
            case STRESS_TRENDING_UP -> BigDecimal.valueOf(4).subtract(interpolate(dayIndex, 0, 3));
            case STRESS_TRENDING_DOWN -> BigDecimal.valueOf(2).add(interpolate(dayIndex, 0, 3));
            case STABLE_LOW_STRESS -> BigDecimal.valueOf(4);
            case STABLE_HIGH_STRESS -> choose(dayIndex, 1, 2);
            case RECOVERY_PATTERN -> dayIndex < 14 ? choose(dayIndex, 1, 2) : BigDecimal.valueOf(3);
            case SPORADIC -> BigDecimal.valueOf(3);
        };
        return new Answer(AnswerType.NUMERIC, QuestionType.SCALE, v.setScale(0, RoundingMode.HALF_UP), null, null);
    }

    private static Answer openValue(int dayIndex) {
        return new Answer(AnswerType.TEXT, QuestionType.TEXT, null,
                OPEN_POOL.get(dayIndex % OPEN_POOL.size()), null);
    }

    // --- helpers ---

    /** Linear interpolation from {@code from} at dayIndex=0 to {@code to} at dayIndex=29. */
    private static BigDecimal interpolate(int dayIndex, int from, int to) {
        double t = dayIndex / (double) (WINDOW_DAYS - 1);
        double v = from + (to - from) * t;
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static int lerpInt(int dayIndex, int from, int to) {
        return (int) Math.round(interpolate(dayIndex, from, to).doubleValue());
    }

    /** Pick between two values based on dayIndex parity — deterministic alternation. */
    private static BigDecimal choose(int dayIndex, int a, int b) {
        return BigDecimal.valueOf((dayIndex % 2 == 0) ? a : b);
    }

    /**
     * A single answer ready to be passed to
     * {@code DailyQuestionAnswerService.submitAnswerForSeed}. Exactly one of
     * {@code numericValue / textValue / optionValue} is set; the type matches
     * the template.
     */
    public record Answer(
            AnswerType answerType,
            QuestionType questionType,
            BigDecimal numericValue,
            String textValue,
            String optionValue
    ) {
    }

    /** A (template-code, answer) pair in template-code order. */
    public record Entry(String templateCode, Answer answer) {
    }
}