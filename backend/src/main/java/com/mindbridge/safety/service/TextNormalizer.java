package com.mindbridge.safety.service;

import java.text.Normalizer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Stateless text normalizer used by the safety pre-filter before rule
 * matching.
 *
 * <p>v1 normalization scope (intentionally minimal — see
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} §6):
 * <ul>
 *   <li>Unicode NFKC normalization so visually-similar characters fold
 *       consistently.</li>
 *   <li>Lowercase.</li>
 *   <li>Strip a defined set of punctuation characters (keeps letters,
 *       digits, spaces, hyphens).</li>
 *   <li>Collapse consecutive whitespace into a single ASCII space.</li>
 * </ul>
 *
 * <p>Out of scope for v1 (deliberate, to keep the MVP small):
 * <ul>
 *   <li>Removing Vietnamese diacritics (would harm legitimate matches
 *       and is non-trivial to do correctly for all dialects).</li>
 *   <li>Teencode / leet / morphological variants — listed as future
 *       work in {@code docs/05_IMPLEMENTATION_STATUS.md} §26.</li>
 *   <li>Stemming / lemmatization — out of scope per
 *       {@code docs/01_ARCHITECTURE.md} §19.</li>
 * </ul>
 *
 * <p>This class is intentionally simple — no regex compilation per call,
 * no allocations beyond the trimmed string, no thread-local state.
 */
@Component
public class TextNormalizer {

    /**
     * Punctuation stripped before matching. The hyphen {@code -} and
     * the apostrophe are intentionally kept so patterns like
     * {@code "tự-tử"} or {@code "don't"} survive normalization. All
     * other ASCII punctuation is removed.
     */
    private static final Pattern STRIP_PUNCT = Pattern.compile(
            "[\\p{Punct}&&[^-']]");

    /** Collapses runs of any whitespace character into a single space. */
    private static final Pattern COLLAPSE_WS = Pattern.compile("\\s+");

    /** Trims leading/trailing whitespace. */
    private static final Pattern TRIM = Pattern.compile("^\\s+|\\s+$");

    /**
     * Apply the v1 normalization chain to the input.
     *
     * @param content raw user content (already redacted upstream).
     *                {@code null} returns {@code ""}.
     * @return the normalized form suitable for keyword and regex
     *         matching.
     */
    public String normalize(String content) {
        if (content == null) {
            return "";
        }
        String s = Normalizer.normalize(content, Normalizer.Form.NFKC);
        s = s.toLowerCase();
        s = STRIP_PUNCT.matcher(s).replaceAll(" ");
        s = COLLAPSE_WS.matcher(s).replaceAll(" ");
        s = TRIM.matcher(s).replaceAll("");
        return s;
    }
}
