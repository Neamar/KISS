package fr.neamar.kiss.utils.calculator;

import android.os.Build;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.Iterator;

public class Calculator {

	public static Result<BigDecimal> calculateExpression(ArrayDeque<Tokenizer.Token> expression) {
		try {
			return calculateExpressionThrowing(expression);
		} catch (ArithmeticException e) {
			return Result.arithmeticalError();
		}
	}

	private static Result<BigDecimal> calculateExpressionThrowing(ArrayDeque<Tokenizer.Token> expression)
			throws ArithmeticException {
		ArrayDeque<BigDecimal> stack = new ArrayDeque<>();

		for (Tokenizer.Token token : expression) {
			BigDecimal operand2 = null;
			BigDecimal operand1 = null;

			switch (token.type) {
				case Tokenizer.Token.NUMBER_TOKEN:
					stack.push(token.number);
					break;

				case Tokenizer.Token.UNARY_PLUS_TOKEN:
					if (errorInExpression(true, stack)) {
						return Result.syntacticalError();
					}

					//redundant: stack.push(stack.pop());
					break;
				case Tokenizer.Token.UNARY_MINUS_TOKEN:
					if (errorInExpression(true, stack)) {
						return Result.syntacticalError();
					}

					stack.push(stack.pop().negate());
					break;

				case Tokenizer.Token.SUM_TOKEN:
					if (errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.add(operand2));
					break;
				case Tokenizer.Token.SUBTRACT_TOKEN:
					if (errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.subtract(operand2));
					break;
				case Tokenizer.Token.MULTIPLY_TOKEN:
					if (errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.multiply(operand2));
					break;
				case Tokenizer.Token.DIVIDE_TOKEN:
					if (errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.divide(operand2, MathContext.DECIMAL32));
					break;
				case Tokenizer.Token.EXP_TOKEN:
					if (errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();

					double pow = StrictMath.pow(operand1.doubleValue(), operand2.doubleValue());

					if(!isFinite(pow)) {
						throw new ArithmeticException("Not finite result: "
								+ operand1.toString() + "^" + operand2.toString() + " = " + pow);
					}

					stack.push(new BigDecimal(pow));
					break;
			}
		}

		if(stack.size() != 1) {
			return Result.syntacticalError();
		}

		return Result.result(stack.pop());
	}

	private static boolean errorInExpression(boolean isUnary, final ArrayDeque<BigDecimal> stack) {
		boolean error = false;
		if(isUnary) {
			error = error || stack.size() < 1;
		} else {
			error = error || stack.size() < 2;
		}
		return error;
	}

	/**
	 * Returns {@code true} if the argument is a finite floating-point
	 * value; returns {@code false} otherwise (for NaN and infinity
	 * arguments).
	 *
	 * @param d the {@code double} value to be tested
	 * @return {@code true} if the argument is a finite
	 * floating-point value, {@code false} otherwise.
	 */
	public static boolean isFinite(double d) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
		    return Double.isFinite(d);
		} else {
			return Math.abs(d) <= Double.MAX_VALUE;
		}
	}
}
