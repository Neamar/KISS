package fr.neamar.kiss.utils.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Iterator;

public class Calculator {
	public static BigDecimal calculateExpression(ArrayDeque<Tokenizer.Token> expression) throws ArithmeticException {
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

				case Tokenizer.Token.SUM_TOKEN:
					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.add(operand2));
					break;
				case Tokenizer.Token.SUBTRACT_TOKEN:
					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.subtract(operand2));
					break;
				case Tokenizer.Token.MULTIPLY_TOKEN:
					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.multiply(operand2));
					break;
				case Tokenizer.Token.DIVIDE_TOKEN:
					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.divide(operand2));
					break;
				case Tokenizer.Token.EXP_TOKEN:
					operand2 = stack.pop();
					operand1 = stack.pop();
					stack.push(operand1.pow(operand2.intValueExact()));
					break;
			}
		}

		return stack.pop();
	}
}
