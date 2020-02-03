package fr.neamar.kiss.utils.calculator;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;

public class CalculatorTest {
	@ParameterizedTest
	@MethodSource("operationsProvider")
	public void testOperations(String operation, BigDecimal result) {
		assertThat(operate(operation).result, closeTo(result, new BigDecimal(0.000001)));
	}

	private static Stream<Arguments> operationsProvider() {
		return Stream.of(
				Arguments.of("+1", new BigDecimal(1)),
				Arguments.of("-1", new BigDecimal(-1)),
				Arguments.of("--1", new BigDecimal(1)),

				Arguments.of("--1.", new BigDecimal(1)),

				Arguments.of("+1+1", new BigDecimal(2)),
				Arguments.of("-1+1+1+1", new BigDecimal(2)),
				Arguments.of("2+0", new BigDecimal(2)),
				Arguments.of("0+2", new BigDecimal(2)),

				Arguments.of("0+2.", new BigDecimal(2)),

				Arguments.of("(1+1)", new BigDecimal(2)),
				Arguments.of("(1)+1", new BigDecimal(2)),
				Arguments.of("(--1)+1", new BigDecimal(2)),
				Arguments.of("--1+1", new BigDecimal(2)),
				Arguments.of("-(-1)+1", new BigDecimal(2)),
				Arguments.of("(--1)+1", new BigDecimal(2)),

				Arguments.of("(1+1.)", new BigDecimal(2)),

				Arguments.of("2*2+1", new BigDecimal(5)),
				Arguments.of("1+2*2", new BigDecimal(5)),
				Arguments.of("(1+2)*2", new BigDecimal(6)),

				Arguments.of("(1+2.)*2", new BigDecimal(6)),

				Arguments.of("2/2/2", new BigDecimal("0.5")),
				Arguments.of("2/1/1", new BigDecimal(2)),
				Arguments.of("2/(1/2)", new BigDecimal(4)),

				Arguments.of("2/2/2.", new BigDecimal("0.5")),
				Arguments.of("2/(1./2)", new BigDecimal(4)),

				Arguments.of("-2**2", new BigDecimal(4)),
				Arguments.of("(-2)**2", new BigDecimal(4)),
				Arguments.of("1+1**2", new BigDecimal(2)),
				Arguments.of("1+1^2", new BigDecimal(2)),

				Arguments.of("1+1^2.", new BigDecimal(2)),

				Arguments.of("(1/3)+(1/3)", new BigDecimal(2).divide(new BigDecimal(3), MathContext.DECIMAL32)),

				Arguments.of("(1/10)", new BigDecimal(1).divide(new BigDecimal(10))),

				Arguments.of("-1^2", new BigDecimal(1))

				);
	}

	private Result<BigDecimal> operate(String operation) {
		Result<ArrayDeque<Tokenizer.Token>> tokenized = Tokenizer.tokenize(operation);
		if(tokenized.syntacticalError) {
			return Result.syntacticalError();
		} else if(tokenized.arithmeticalError) {
			return Result.arithmeticalError();
		} else {
			Result<ArrayDeque<Tokenizer.Token>> posfixed = ShuntingYard.infixToPostfix(tokenized.result);

			if (posfixed.syntacticalError) {
				return Result.syntacticalError();
			} else if (posfixed.arithmeticalError) {
				return Result.arithmeticalError();
			} else {
				return Calculator.calculateExpression(posfixed.result);
			}
		}
	}
}
