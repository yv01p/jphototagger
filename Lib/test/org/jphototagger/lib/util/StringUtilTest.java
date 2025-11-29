package org.jphototagger.lib.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for StringUtil.
 * Migrated from JUnit 4 to JUnit 5 with AssertJ assertions.
 */
class StringUtilTest {

    @Nested
    @DisplayName("wrapWords")
    class WrapWords {

        @Test
        @DisplayName("returns empty list for empty string")
        void emptyString() {
            List<String> result = StringUtil.wrapWords("", 1);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("single character within limit")
        void singleCharacter() {
            List<String> result = StringUtil.wrapWords("a", 1);
            assertThat(result).containsExactly("a");
        }

        @Test
        @DisplayName("splits long words")
        void splitsLongWords() {
            List<String> result = StringUtil.wrapWords("aa", 1);
            assertThat(result).containsExactly("a", "a");
        }

        @Test
        @DisplayName("splits on spaces")
        void splitsOnSpaces() {
            List<String> result = StringUtil.wrapWords("a a", 1);
            assertThat(result).containsExactly("a", "a");
        }

        @Test
        @DisplayName("splits on spaces with multiple words")
        void splitsOnSpacesMultipleWords() {
            List<String> result = StringUtil.wrapWords("aa a", 1);
            assertThat(result).containsExactly("a", "a", "a");
        }

        @Test
        @DisplayName("wraps German text correctly")
        void wrapsGermanText() {
            String text = "Dies ist ein längerer Text mit 43 Zeichen.";
            List<String> result = StringUtil.wrapWords(text, 25);
            assertThat(result).containsExactly(
                    "Dies ist ein längerer",
                    "Text mit 43 Zeichen.");
        }

        @Test
        @DisplayName("wraps single long word")
        void wrapsSingleLongWord() {
            String text = "DiesisteinlängererTextmit36Zeichen.";
            List<String> result = StringUtil.wrapWords(text, 25);
            assertThat(result).containsExactly(
                    "DiesisteinlängererTextmit",
                    "36Zeichen.");
        }
    }

    @Nested
    @DisplayName("getNTimesRepeated")
    class GetNTimesRepeated {

        @Test
        @DisplayName("returns empty for empty string")
        void emptyString() {
            assertThat(StringUtil.getNTimesRepeated("", 0)).isEmpty();
            assertThat(StringUtil.getNTimesRepeated("", 100)).isEmpty();
        }

        @Test
        @DisplayName("returns empty for zero repetitions")
        void zeroRepetitions() {
            assertThat(StringUtil.getNTimesRepeated(".", 0)).isEmpty();
            assertThat(StringUtil.getNTimesRepeated("abc", 0)).isEmpty();
        }

        @Test
        @DisplayName("repeats single character")
        void repeatsSingleChar() {
            assertThat(StringUtil.getNTimesRepeated(".", 1)).isEqualTo(".");
            assertThat(StringUtil.getNTimesRepeated(".", 3)).isEqualTo("...");
        }

        @Test
        @DisplayName("repeats multi-character string")
        void repeatsMultiChar() {
            assertThat(StringUtil.getNTimesRepeated("abc", 1)).isEqualTo("abc");
            assertThat(StringUtil.getNTimesRepeated("abc", 3)).isEqualTo("abcabcabc");
        }
    }

    @Nested
    @DisplayName("getSubstringCount")
    class GetSubstringCount {

        @Test
        @DisplayName("returns 0 for empty string")
        void emptyString() {
            assertThat(StringUtil.getSubstringCount("", "")).isZero();
            assertThat(StringUtil.getSubstringCount("", "bla")).isZero();
        }

        @Test
        @DisplayName("counts single occurrence")
        void singleOccurrence() {
            assertThat(StringUtil.getSubstringCount("bla", "bla")).isEqualTo(1);
        }

        @Test
        @DisplayName("counts multiple occurrences")
        void multipleOccurrences() {
            assertThat(StringUtil.getSubstringCount("bla bla", "bla")).isEqualTo(2);
            assertThat(StringUtil.getSubstringCount("blablabla", "bla")).isEqualTo(3);
        }

        @Test
        @DisplayName("counts multiple occurrences with multi-word substring")
        void multipleOccurrencesMultiWord() {
            String substringRegex = "Multiple words here ";
            String string = substringRegex + "abc" + substringRegex + substringRegex + " " + substringRegex;
            int count = StringUtil.getSubstringCount(string, substringRegex);
            assertThat(count).isEqualTo(4);
        }

        @Test
        @DisplayName("returns 0 when substring not found")
        void substringNotFound() {
            assertThat(StringUtil.getSubstringCount("blubb", "bla")).isZero();
        }
    }

    @Nested
    @DisplayName("removeLast")
    class RemoveLast {

        @Test
        @DisplayName("handles empty strings")
        void emptyStrings() {
            assertThat(StringUtil.removeLast("", "")).isEmpty();
            assertThat(StringUtil.removeLast("", "bla")).isEmpty();
            assertThat(StringUtil.removeLast("bla", "")).isEqualTo("bla");
        }

        @Test
        @DisplayName("removes entire string when equal")
        void removesEntireString() {
            assertThat(StringUtil.removeLast("bla", "bla")).isEmpty();
        }

        @Test
        @DisplayName("removes last occurrence")
        void removesLastOccurrence() {
            assertThat(StringUtil.removeLast("bla bla bla", "bla bla")).isEqualTo("bla ");
        }

        @Test
        @DisplayName("returns original when substring not found")
        void substringNotFound() {
            assertThat(StringUtil.removeLast("xyz", "bla bla")).isEqualTo("xyz");
        }

        @Test
        @DisplayName("removes from middle of string")
        void removesFromMiddle() {
            assertThat(StringUtil.removeLast("xyz bla xyz", "bla")).isEqualTo("xyz  xyz");
        }
    }
}
