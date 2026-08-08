package com.mindbridge.devseed;

import com.mindbridge.dailyquestion.domain.AnswerType;
import com.mindbridge.dailyquestion.domain.QuestionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Deterministic generator of per-day answer values for the seven-day trend seed.
 *
 * <p>Generates a non-clinical improving-trend pattern:
 * <ul>
 *   <li>Day 1: stress 4, mood 2, energy 2, sleep 5.0h - highest stress, low mood/energy</li>
 *   <li>Days 2-6: monotonic improvement</li>
 *   <li>Day 7: stress 2, mood 4, energy 4, sleep 7.5h - clearly lower stress, higher mood/energy</li>
 * </ul>
 *
 * <p>All values are in the approved [1-5] scale (SCALE templates) and
 * [0-24] range (SLEEP NUMBER template). Values remain in the non-clinical
 * test range and are explicitly NOT clinical thresholds.
 *
 * <h2>Determinism</h2>
 * All values are computed from {@code dayIndex (0..6)} with no randomness.
 * The same seed always produces the same values.
 */
public final class SevenDayTrendPlan {

    private SevenDayTrendPlan() {}

    // Mood option values for SINGLE_CHOICE 1..5 (matches V6 seed MOOD options)
    private static final List<String> MOOD_OPTIONS = List.of("1", "2", "3", "4", "5");

    /**
     * Returns the answer entries for the given day index (0 = oldest, 6 = target day).
     * Template order: STRESS, MOOD, SLEEP, ENERGY, OPEN (alphabetic, matching
     * the existing findByUserIdAndAssignedForDateOrderByTemplateCodeAsc query).
     */
    public static List<Answer> forDay(int dayIndex) {
        if (dayIndex < 0 || dayIndex > 6) {
            throw new IllegalArgumentException("dayIndex must be 0..6, got: " + dayIndex);
        }
        return List.of(
                stress(dayIndex),
                mood(dayIndex),
                sleep(dayIndex),
                energy(dayIndex),
                open(dayIndex)
        );
    }

    // STRESS: raw 4 -> 2 (stress decreasing = improving)
    private static Answer stress(int day) {
        int raw = 4 - day; // 4, 3, 3, 2, 2, 2, 2
        int clamped = Math.max(1, Math.min(5, raw));
        return new Answer(AnswerType.NUMERIC, QuestionType.SCALE,
                BigDecimal.valueOf(clamped), null, null);
    }

    // MOOD: option "2" -> "4" (mood improving)
    // mood option value: 5 = great, 1 = bad
    // Day 1: "2" (poor), Day 7: "4" (good)
    private static Answer mood(int day) {
        int value = 2 + day; // 2, 3, 3, 3, 4, 4, 4
        int clamped = Math.max(1, Math.min(5, value));
        int optionIndex = 5 - clamped; // 5 -> "5", 4 -> "4", ...
        String optionValue = MOOD_OPTIONS.get(optionIndex);
        return new Answer(AnswerType.OPTION, QuestionType.SINGLE_CHOICE, null, null, optionValue);
    }

    // SLEEP: 5.0h -> 7.5h (improving sleep duration)
    private static Answer sleep(int day) {
        double hours = 5.0 + (day * 0.5); // 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 7.5
        hours = Math.min(24.0, hours);
        BigDecimal h = BigDecimal.valueOf(hours).setScale(1, RoundingMode.HALF_UP);
        return new Answer(AnswerType.NUMERIC, QuestionType.NUMBER, h, null, null);
    }

    // ENERGY: raw 2 -> 4 (energy increasing)
    private static Answer energy(int day) {
        int raw = 2 + day; // 2, 2, 3, 3, 3, 4, 4
        int clamped = Math.max(1, Math.min(5, raw));
        return new Answer(AnswerType.NUMERIC, QuestionType.SCALE,
                BigDecimal.valueOf(clamped), null, null);
    }

    // OPEN: neutral placeholder text
    private static Answer open(int day) {
        String text = switch (day) {
            case 0 -> "Today felt a bit challenging.";
            case 1 -> "Managed to get through today.";
            case 2 -> "Things are getting a bit more manageable.";
            case 3 -> "Feeling a little better about today.";
            case 4 -> "Today was better than yesterday.";
            case 5 -> "I noticed some improvements today.";
            case 6 -> "Today felt quite good overall.";
            default -> "A regular day.";
        };
        return new Answer(AnswerType.TEXT, QuestionType.TEXT, null, text, null);
    }

    /**
     * A single answer entry.
     *
     * @param answerType   NUMERIC | TEXT | OPTION
     * @param questionType SCALE | NUMBER | SINGLE_CHOICE | TEXT
     * @param numericValue set for NUMERIC, null otherwise
     * @param textValue   set for TEXT, null otherwise
     * @param optionValue set for OPTION, null otherwise
     */
    public record Answer(
            AnswerType answerType,
            QuestionType questionType,
            BigDecimal numericValue,
            String textValue,
            String optionValue
    ) {}
}