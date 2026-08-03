package com.mindbridge.safety.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TextNormalizer}. Pure JUnit 5 + AssertJ —
 * does NOT boot a Spring context.
 */
@DisplayName("TextNormalizer")
class TextNormalizerTest {

    private final TextNormalizer normalizer = new TextNormalizer();

    @Nested
    @DisplayName("Lowercase folding")
    class Lowercase {

        @Test
        @DisplayName("ASCII uppercase is folded")
        void ascii() {
            assertThat(normalizer.normalize("XIN CHAO"))
                    .isEqualTo("xin chao");
        }

        @Test
        @DisplayName("Vietnamese uppercase diacritics are folded (Đ → đ)")
        void vietnameseDiacritics() {
            // 'Đ' has no lowercase form in NFKC; Java folds it to 'đ'.
            assertThat(normalizer.normalize("ĐANG NGHĨ"))
                    .isEqualTo("đang nghĩ");
        }
    }

    @Nested
    @DisplayName("Punctuation stripping")
    class Punctuation {

        @Test
        @DisplayName("Common ASCII punctuation is stripped")
        void commonPunct() {
            assertThat(normalizer.normalize("Xin chào, bạn!"))
                    .isEqualTo("xin chào bạn");
        }

        @Test
        @DisplayName("Hyphen and apostrophe are kept")
        void hyphenAndApostrophe() {
            // Critical: rule pattern "tự-tử" must survive normalization.
            assertThat(normalizer.normalize("tự-tử"))
                    .isEqualTo("tự-tử");
            assertThat(normalizer.normalize("don't"))
                    .isEqualTo("don't");
        }

        @Test
        @DisplayName("Multiple punctuation marks are removed together")
        void multiple() {
            assertThat(normalizer.normalize("...xin...chao???"))
                    .isEqualTo("xin chao");
        }
    }

    @Nested
    @DisplayName("Whitespace handling")
    class Whitespace {

        @Test
        @DisplayName("Consecutive whitespace collapses to single space")
        void collapse() {
            assertThat(normalizer.normalize("a   b\t\tc\n\nd"))
                    .isEqualTo("a b c d");
        }

        @Test
        @DisplayName("Leading and trailing whitespace is trimmed")
        void trim() {
            assertThat(normalizer.normalize("   xin chào   "))
                    .isEqualTo("xin chào");
        }
    }

    @Nested
    @DisplayName("End-to-end normalization chain")
    class EndToEnd {

        @Test
        @DisplayName("Combined case, punct, whitespace is normalized")
        void combined() {
            assertThat(normalizer.normalize("Xin   chào,  TÔI  buồn!"))
                    .isEqualTo("xin chào tôi buồn");
        }

        @Test
        @DisplayName("Empty string returns empty string")
        void empty() {
            assertThat(normalizer.normalize("")).isEqualTo("");
        }

        @Test
        @DisplayName("null returns empty string")
        void nullInput() {
            assertThat(normalizer.normalize(null)).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Unicode preservation")
    class Unicode {

        @Test
        @DisplayName("Vietnamese diacritics are preserved (không remove)")
        void vietnameseDiacriticsPreserved() {
            assertThat(normalizer.normalize("buồn quá"))
                    .isEqualTo("buồn quá");
        }

        @Test
        @DisplayName("Emoji are preserved")
        void emojiPreserved() {
            // Emoji are not stripped — they survive NFKC normalization
            // and are not matched by the punctuation regex.
            String result = normalizer.normalize("vui 😊");
            assertThat(result).startsWith("vui");
            assertThat(result).contains("😊");
        }

        @Test
        @DisplayName("Cyrillic letters are preserved (no Latin-only folding)")
        void cyrillic() {
            assertThat(normalizer.normalize("Привет")).isEqualTo("привет");
        }
    }
}
