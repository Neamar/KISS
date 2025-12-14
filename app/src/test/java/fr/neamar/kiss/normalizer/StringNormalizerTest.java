package fr.neamar.kiss.normalizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class StringNormalizerTest {


    @ParameterizedTest
    @MethodSource("testProvider")
    public void testNormalization(String query, String normalizedQuery) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);
        assertThat(queryNormalized, notNullValue());
        assertThat(queryNormalized.toString(), equalTo(normalizedQuery));
    }

    private static Stream<Arguments> testProvider() {
        return Stream.of(
                Arguments.of("è", "e"),
                Arguments.of("é", "e"),
                Arguments.of("ê", "e"),
                Arguments.of("ë", "e"),
                Arguments.of("û", "u"),
                Arguments.of("ù", "u"),
                Arguments.of("ï", "i"),
                Arguments.of("î", "i"),
                Arguments.of("à", "a"),
                Arguments.of("â", "a"),
                Arguments.of("ô", "o"),
                Arguments.of("ä", "a"),
                Arguments.of("ö", "o"),
                Arguments.of("ü", "u"),
                Arguments.of("Å", "A"),
                Arguments.of("Æ", "Æ"),
                Arguments.of("ﬁ", "fi")
        );
    }


}
