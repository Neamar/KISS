package fr.neamar.kiss.utils.calculator;

import java.util.ArrayDeque;

public class ShuntingYard {

	public static Result<ArrayDeque<Tokenizer.Token>> infixToPostfix(ArrayDeque<Tokenizer.Token> infix) {
		ArrayDeque<Tokenizer.Token> outQueue = new ArrayDeque<>();
		ArrayDeque<Tokenizer.Token> operatorStack = new ArrayDeque<>();

		for (Tokenizer.Token token : infix) {
			switch (token.type) {
				case Tokenizer.Token.NUMBER_TOKEN:
					outQueue.add(token);
					break;
				case Tokenizer.Token.UNARY_PLUS_TOKEN:
				case Tokenizer.Token.UNARY_MINUS_TOKEN:
				case Tokenizer.Token.SUM_TOKEN:
				case Tokenizer.Token.SUBTRACT_TOKEN:
				case Tokenizer.Token.MULTIPLY_TOKEN:
				case Tokenizer.Token.DIVIDE_TOKEN:
				case Tokenizer.Token.EXP_TOKEN:
					if (operatorStack.isEmpty()) {
						operatorStack.push(token);
					} else {
						while (!operatorStack.isEmpty()) {
							int prec1 = token.getPrecedence();
							int prec2 = operatorStack.peek().getPrecedence();

							if ((token.isLeftAssociative() && prec1 <= prec2) || (token.isRightAssociative() && prec1 < prec2)) {
								outQueue.add(operatorStack.pop());
							} else {
								break;
							}
						}
						operatorStack.push(token);
					}
					break;
				case Tokenizer.Token.PARENTHESIS_OPEN_TOKEN:
					operatorStack.push(token);
					break;
				case Tokenizer.Token.PARENTHESIS_CLOSE_TOKEN:
					if(operatorStack.isEmpty()) {
						return Result.syntacticalError();
					}

					// until '(' on stack, pop operators.
					while (operatorStack.peek().type != Tokenizer.Token.PARENTHESIS_OPEN_TOKEN) {
						outQueue.add(operatorStack.pop());

						if(operatorStack.isEmpty()) {
							return Result.syntacticalError();
						}
					}
					operatorStack.pop();
					break;
			}
		}

		while (!operatorStack.isEmpty()) {
			outQueue.addLast(operatorStack.pop());
		}

		return Result.result(outQueue);
	}

}
