package fr.neamar.kiss.dataprovider.simpleprovider;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CalculatorProviderTest {
	private static final Pattern pattern = new CalculatorProvider().p;

	@ParameterizedTest
	@MethodSource("expressionProvider")
	public void testOperations(String expression, String operation) {
		Matcher matcher = pattern.matcher(expression.trim());
		matcher.find();
		assertThat(matcher.group(), is(operation));
	}

	private static Stream<Arguments> expressionProvider() {
		return Stream.of(
				Arguments.of("(1+1)", "(1+1)"),
				Arguments.of("(1 + 1)", "(1+1)"),

				Arguments.of("afsdfsds(1+1)dfsdfsd", "(1+1)"),
				Arguments.of("aadf8-100fs", "8-100"),

				Arguments.of("+1-1", "+1-1"),
				Arguments.of("-1-1", "-1-1"),
				Arguments.of("1*1", "1*1"),
				Arguments.of("1/1", "1/1"),

				Arguments.of("(1)/1", "(1)/1"),
				Arguments.of("( 1 )/1", "(1)/1"),
				Arguments.of("(1+1)/1", "(1+1)/1"),
				Arguments.of("(1*1)/1", "(1*1)/1"),

				Arguments.of("(1+1)/2.", "(1+1)/2."),
				Arguments.of("89.*(1+1)/2.", "89.*(1+1)/2.")
		);
	}
}
