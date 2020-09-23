package fr.neamar.kiss.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import fr.neamar.kiss.normalizer.StringNormalizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class FuzzyScoreTest {
    private static final int full_word_bonus = 1000000;
    private static final int adjacency_bonus  = 100000;
    private static final int separator_bonus  = 10000;
    private static final int camel_bonus      = 1000;
    private static final int leading_letter_penalty     = -100;
    private static final int max_leading_letter_penalty = -300;
    private static final int unmatched_letter_penalty   = -1;

    @ParameterizedTest
    @MethodSource("testProvider")
    public void testOperations(String query, String testString, int result) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);
        StringNormalizer.Result testStringNormalized = StringNormalizer.normalizeWithResult(testString, false);

        assertThat(doFuzzy(queryNormalized.codePoints, testStringNormalized.codePoints), equalTo(result));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testProvider() {
        return Stream.of(
                Arguments.of("no match", "some string", 0),
                Arguments.of("yt", "YouTube", separator_bonus + camel_bonus + 5 * unmatched_letter_penalty),
                Arguments.of("js", "js", full_word_bonus + adjacency_bonus + separator_bonus),

                // Test full match start of word
                Arguments.of("js", "js end", full_word_bonus + adjacency_bonus + separator_bonus + 4 * unmatched_letter_penalty),
                // Test full match end of word
                Arguments.of("js", "start js", full_word_bonus + adjacency_bonus + separator_bonus + 6 * unmatched_letter_penalty + max_leading_letter_penalty),

                Arguments.of("js", "John Smith", 2 * separator_bonus + 8 * unmatched_letter_penalty),
                Arguments.of("jsmith", "John Smith", 2 * separator_bonus + 4 * unmatched_letter_penalty + 4 * adjacency_bonus + full_word_bonus)
        );
    }

    private Integer doFuzzy(int[] query, int[] testString) {
        return new FuzzyScore(query, false)
                .setFullWordBonus(full_word_bonus)
                .setAdjacencyBonus(adjacency_bonus)
                .setSeparatorBonus(separator_bonus)
                .setCamelBonus(camel_bonus)
                .setLeadingLetterPenalty(leading_letter_penalty)
                .setMaxLeadingLetterPenalty(max_leading_letter_penalty)
                .setUnmatchedLetterPenalty(unmatched_letter_penalty)
                .match(testString).score;
    }
}
