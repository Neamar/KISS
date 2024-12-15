package fr.neamar.kiss.utils.fuzzy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import fr.neamar.kiss.normalizer.StringNormalizer;

class FuzzyScoreV2Test {
    private static final int full_word_bonus = 0;//1000000;
    private static final int adjacency_bonus = 100000;
    private static final int separator_bonus = 10000;
    private static final int camel_bonus = 1000;
    private static final int first_letter_bonus = 100;
    private static final int leading_letter_penalty = -100;
    private static final int max_leading_letter_penalty = -300;
    private static final int unmatched_letter_penalty = -1;

    @ParameterizedTest
    @MethodSource("testProvider")
    public void testOperations(String query, String testString, int score, boolean match) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);
        StringNormalizer.Result testStringNormalized = StringNormalizer.normalizeWithResult(testString, false);

        MatchInfo result = doFuzzy(queryNormalized.codePoints, testStringNormalized.codePoints);
        assertThat(result.match, equalTo(match));
        assertThat(result.score, equalTo(score));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testProvider() {
        return Stream.of(
                Arguments.of("no match", "some string", max_leading_letter_penalty + 10 * unmatched_letter_penalty, false),
                Arguments.of("yt", "YouTube", separator_bonus + camel_bonus + 5 * unmatched_letter_penalty, true),
                Arguments.of("js", "js", full_word_bonus + adjacency_bonus + separator_bonus, true),

                // Test full match start of word
                Arguments.of("js", "js end", full_word_bonus + adjacency_bonus + separator_bonus + 4 * unmatched_letter_penalty, true),
                // Test full match end of word
                Arguments.of("js", "start js", full_word_bonus + adjacency_bonus + separator_bonus + 6 * unmatched_letter_penalty + max_leading_letter_penalty, true),

                Arguments.of("js", "John Smith", 2 * separator_bonus + 8 * unmatched_letter_penalty, true),
                Arguments.of("jsmith", "John Smith", 2 * separator_bonus + 4 * unmatched_letter_penalty + 4 * adjacency_bonus + full_word_bonus, true),

                Arguments.of("second", "first second third word", separator_bonus + 15 * unmatched_letter_penalty + 5 * adjacency_bonus + full_word_bonus + 3 * leading_letter_penalty, true),
                Arguments.of("econd", "first second third word", 16 * unmatched_letter_penalty + 4 * adjacency_bonus + max_leading_letter_penalty, true),
                Arguments.of("third", "first second third word", separator_bonus + 17 * unmatched_letter_penalty + 4 * adjacency_bonus + full_word_bonus + max_leading_letter_penalty, true),
                Arguments.of("word", "first second third word", separator_bonus + 19 * unmatched_letter_penalty + 3 * adjacency_bonus + full_word_bonus + max_leading_letter_penalty, true),
                Arguments.of("first second third word", "firss", separator_bonus + 3 * adjacency_bonus + full_word_bonus, false)
        );
    }

    private MatchInfo doFuzzy(int[] query, int[] testString) {
        return createFuzzyScore(query)
                .match(testString);
    }

    private FuzzyScore createFuzzyScore(int[] query) {
        return new FuzzyScoreV2(query, true)
                .setFullWordBonus(full_word_bonus)
                .setAdjacencyBonus(adjacency_bonus)
                .setSeparatorBonus(separator_bonus)
                .setCamelBonus(camel_bonus)
                .setFirstLetterBonus(first_letter_bonus)
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
        assertThat(doFuzzy(queryNormalized.codePoints, testStringNormalized1.codePoints).score, equalTo(separator_bonus + adjacency_bonus + adjacency_bonus + full_word_bonus));
        // Test no match standalone: this must result in appropriate penalty
        assertThat(doFuzzy(queryNormalized.codePoints, testStringNormalized2.codePoints).score, equalTo(unmatched_letter_penalty * 5));

        // create fuzzy score that is reused as in KISS providers
        FuzzyScore fuzzyScore = createFuzzyScore(queryNormalized.codePoints);

        // Test full match
        MatchInfo match1 = fuzzyScore.match(testStringNormalized1.codePoints);
        assertThat(match1.score, equalTo(separator_bonus + adjacency_bonus + adjacency_bonus + full_word_bonus));
        // Test no match: this must result in appropriate penalty, independent of previous match
        MatchInfo match2 = fuzzyScore.match(testStringNormalized2.codePoints);
        assertThat(match2.score, equalTo(unmatched_letter_penalty * 5));
    }
}
