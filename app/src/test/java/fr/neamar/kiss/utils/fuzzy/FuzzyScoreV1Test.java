package fr.neamar.kiss.utils.fuzzy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import fr.neamar.kiss.normalizer.StringNormalizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class FuzzyScoreV1Test {
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
                Arguments.of("no match", "some string", max_leading_letter_penalty + 10 * unmatched_letter_penalty),
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
        return createFuzzyScore(query)
                .match(testString).score;
    }

    private FuzzyScore createFuzzyScore(int[] query) {
        return new FuzzyScoreV1(query, false)
                .setFullWordBonus(full_word_bonus)
                .setAdjacencyBonus(adjacency_bonus)
                .setSeparatorBonus(separator_bonus)
                .setCamelBonus(camel_bonus)
                .setLeadingLetterPenalty(leading_letter_penalty)
                .setMaxLeadingLetterPenalty(max_leading_letter_penalty)
                .setUnmatchedLetterPenalty(unmatched_letter_penalty);
    }
    @Test
    public void testReusedMatchInfoScore() {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult("Bob", false);
        StringNormalizer.Result testStringNormalized1 = StringNormalizer.normalizeWithResult("Bob", false);
        StringNormalizer.Result testStringNormalized2 = StringNormalizer.normalizeWithResult("Alice", false);

        // Test full match standalone
        assertThat(doFuzzy(queryNormalized.codePoints, testStringNormalized1.codePoints), equalTo(separator_bonus + adjacency_bonus + adjacency_bonus + full_word_bonus));
        // Test no match standalone: this must result in appropriate penalty
        assertThat(doFuzzy(queryNormalized.codePoints, testStringNormalized2.codePoints), equalTo(unmatched_letter_penalty * 5));

        // create fuzzy score that is reused as in KISS providers
        FuzzyScore fuzzyScore = createFuzzyScore(queryNormalized.codePoints);

        // Test full match
        MatchInfo match1  = fuzzyScore.match(testStringNormalized1.codePoints);
        assertThat(match1.score, equalTo(separator_bonus + adjacency_bonus + adjacency_bonus + full_word_bonus));
        // Test no match: this must result in appropriate penalty, independent of previous match
        MatchInfo match2  = fuzzyScore.match(testStringNormalized2.codePoints);
        assertThat(match2.score, equalTo(unmatched_letter_penalty * 5));
    }
}
