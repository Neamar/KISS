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
    private static final int first_letter_bonus = 1000000;
    private static final int adjacency_bonus = 100000;
    private static final int separator_bonus = 10000;
    private static final int camel_bonus = 1000;
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
                Arguments.of("no match", "some string", 0, false),
                Arguments.of("yt", "YouTube", 100 + camel_bonus + first_letter_bonus + 5 * unmatched_letter_penalty, true),
                Arguments.of("js", "js", 100 + adjacency_bonus + first_letter_bonus, true),

                // Test full match start of word
                Arguments.of("js", "js end", 100 + adjacency_bonus + first_letter_bonus + 4 * unmatched_letter_penalty, true),
                // Test full match end of word
                Arguments.of("js", "start js", 100 + adjacency_bonus + separator_bonus + 6 * unmatched_letter_penalty + max_leading_letter_penalty, true),

                Arguments.of("js", "John Smith", 100 + separator_bonus + first_letter_bonus + 8 * unmatched_letter_penalty, true),
                Arguments.of("jsmith", "John Smith", 100 + 4 * adjacency_bonus + separator_bonus + first_letter_bonus + 4 * unmatched_letter_penalty, true),

                Arguments.of("second", "first second third word", 100 + 5 * adjacency_bonus + separator_bonus + 17 * unmatched_letter_penalty + 3 * leading_letter_penalty, true),
                Arguments.of("econd", "first second third word", 100 + 4 * adjacency_bonus + 18 * unmatched_letter_penalty + max_leading_letter_penalty, true),
                Arguments.of("third", "first second third word", 100 + 4 * adjacency_bonus + separator_bonus + 18 * unmatched_letter_penalty + max_leading_letter_penalty, true),
                Arguments.of("word", "first second third word", 100 + 3 * adjacency_bonus + separator_bonus + 19 * unmatched_letter_penalty + max_leading_letter_penalty, true),
                Arguments.of("first second third word", "firss", 0, false)
        );
    }

    private MatchInfo doFuzzy(int[] query, int[] testString) {
        return createFuzzyScore(query)
                .match(testString);
    }

    private FuzzyScore createFuzzyScore(int[] query) {
        return new FuzzyScoreV2(query, true)
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
        MatchInfo match = doFuzzy(queryNormalized.codePoints, testStringNormalized1.codePoints);
        assertThat(match.match, equalTo(true));
        assertThat(match.score, equalTo(100 + 2 * adjacency_bonus + first_letter_bonus));
        // Test no match standalone: this must result in appropriate penalty
        match = doFuzzy(queryNormalized.codePoints, testStringNormalized2.codePoints);
        assertThat(match.match, equalTo(false));
        assertThat(match.score, equalTo(0));

        // create fuzzy score that is reused as in KISS providers
        FuzzyScore fuzzyScore = createFuzzyScore(queryNormalized.codePoints);

        // Test full match
        match = fuzzyScore.match(testStringNormalized1.codePoints);
        assertThat(match.match, equalTo(true));
        assertThat(match.score, equalTo(100 + 2 * adjacency_bonus + first_letter_bonus));
        // Test no match: this must result in appropriate penalty, independent of previous match
        match = fuzzyScore.match(testStringNormalized2.codePoints);
        assertThat(match.match, equalTo(false));
        assertThat(match.score, equalTo(0));
    }
}
