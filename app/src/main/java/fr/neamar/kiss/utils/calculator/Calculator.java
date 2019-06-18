package fr.neamar.kiss.utils.calculator;

import java.math.BigDecimal;
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
		Iterator<Tokenizer.Token> iterator = expression.descendingIterator();


		while (iterator.hasNext()) {
			Tokenizer.Token token = iterator.next();

			BigDecimal operand2 = null;
			BigDecimal operand1 = null;

			switch (token.type) {
				case Tokenizer.Token.NUMBER_TOKEN:
					stack.push(token.number);
					break;

				case Tokenizer.Token.UNARY_PLUS_TOKEN:
					if(errorInExpression(true, stack)) {
						return Result.syntacticalError();
					}

					//redundant: stack.push(stack.pop());
					break;
				case Tokenizer.Token.UNARY_MINUS_TOKEN:
					if(errorInExpression(true, stack)) {
						return Result.syntacticalError();
					}

					stack.push(stack.pop().negate());
					break;

				case Tokenizer.Token.SUM_TOKEN:
					if(errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.add(operand2));
					break;
				case Tokenizer.Token.SUBTRACT_TOKEN:
					if(errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.subtract(operand2));
					break;
				case Tokenizer.Token.MULTIPLY_TOKEN:
					if(errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.multiply(operand2));
					break;
				case Tokenizer.Token.DIVIDE_TOKEN:
					if(errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.divide(operand2));
					break;
				case Tokenizer.Token.EXP_TOKEN:
					if(errorInExpression(false, stack)) {
						return Result.syntacticalError();
					}

					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(new BigDecimal(StrictMath.pow(operand1.doubleValue(), operand2.doubleValue())));
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
}
