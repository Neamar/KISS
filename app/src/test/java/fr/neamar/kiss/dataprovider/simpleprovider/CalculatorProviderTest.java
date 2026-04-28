package fr.neamar.kiss.dataprovider.simpleprovider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CalculatorProviderTest {
	private static final CalculatorProvider provider = new CalculatorProvider();
	private static final Pattern pattern = provider.computableRegexp;

	@ParameterizedTest
	@MethodSource("expressionProvider")
	public void testOperations(String expression, String operation) {
		Matcher matcher = pattern.matcher(expression.replaceAll("\\s+", ""));
		matcher.find();
		assertThat(matcher.group(), is(operation));
	}

	private static Stream<Arguments> expressionProvider() {
		return Stream.of(
				Arguments.of("(1+1)", "(1+1)"),
				Arguments.of("(1 + 1)", "(1+1)"),

				Arguments.of("+1-1", "+1-1"),
				Arguments.of("-1-1", "-1-1"),
				Arguments.of("1*1", "1*1"),
				Arguments.of("1/1", "1/1"),
				Arguments.of("1**1", "1**1"),

				Arguments.of("(1)/1", "(1)/1"),
				Arguments.of("( 1 )/1", "(1)/1"),
				Arguments.of("(1+1)/1", "(1+1)/1"),
				Arguments.of("(1*1)/1", "(1*1)/1"),

				Arguments.of("(1+1)/2.", "(1+1)/2."),
				Arguments.of("89.*(1+1)/2.", "89.*(1+1)/2."),

				Arguments.of("8,009.*(1+1)/2.", "8,009.*(1+1)/2.")
		);
	}

	@ParameterizedTest
	@MethodSource("percentageProvider")
	public void testPercentage(String expression, String expected) {
		BigDecimal value = provider.compute(expression);
		assertThat(value, comparesEqualTo(new BigDecimal(expected)));
	}

	private static Stream<Arguments> percentageProvider() {
		return Stream.of(
				// Trailing +/- N% applied to the base
				Arguments.of("100+10%", "110"),
				Arguments.of("200-25%", "150"),
				Arguments.of("(50+50)+10%", "110"),
				Arguments.of("1500*2-10%", "2700"),
				Arguments.of("100+12.5%", "112.5"),
				Arguments.of("100+12,5%", "112.5"),
				Arguments.of("100-100%", "0"),
				Arguments.of("100+0%", "100"),

				// Pattern doesn't match: % keeps its modulo meaning
				Arguments.of("17%5", "2"),
				Arguments.of("100%5", "0")
		);
	}

	@ParameterizedTest
	@MethodSource("rejectedProvider")
	public void testRejected(String expression) {
		assertThat(provider.compute(expression), is(nullValue()));
	}

	private static Stream<Arguments> rejectedProvider() {
		return Stream.of(
				// Multiple %: trailing-% pattern doesn't match (base would contain '%'),
				// fall-through evaluates 100 mod 0 (from the inner 10%+5...) -> arithmetic error.
				Arguments.of("100+10%+5%"),
				// Pattern matches but base is malformed -> no result.
				Arguments.of("100++10%")
		);
	}
}
