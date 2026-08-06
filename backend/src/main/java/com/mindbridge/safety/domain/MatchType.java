package com.mindbridge.safety.domain;

/**
 * How a {@link SafetyKeywordRule} pattern is matched against the normalized
 * user content.
 *
 * <ul>
 *   <li>{@link #KEYWORD} — the {@code pattern} field is treated as a literal
 *       substring; matching is case-insensitive and done against the
 *       normalized text. Suitable for short, fixed phrases.</li>
 *   <li>{@link #REGEX} — the {@code pattern} field is compiled as a Java
 *       regex. Use this when a rule needs word boundaries, alternations,
 *       or any structural pattern that simple substring cannot express.</li>
 * </ul>
 *
 * <p>The match type is stored in {@code safety_keyword_rules.match_type}
 * with a CHECK constraint that limits the value to this enum's names
 * (case-sensitive).
 */
public enum MatchType {

    /** Literal substring match (case-insensitive, against normalized text). */
    KEYWORD,

    /** Java regex match (case-insensitive flag, against normalized text). */
    REGEX
}
