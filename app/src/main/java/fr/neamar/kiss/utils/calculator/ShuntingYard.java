package fr.neamar.kiss.utils.calculator;

import java.util.ArrayDeque;

public class ShuntingYard {

	public static Result<ArrayDeque<Tokenizer.Token>> infixToPostfix(ArrayDeque<Tokenizer.Token> infix) {
		ArrayDeque<Tokenizer.Token> sb = new ArrayDeque<>();
		ArrayDeque<Tokenizer.Token> s = new ArrayDeque<>();

		for (Tokenizer.Token token : infix) {
			switch (token.type) {
				case Tokenizer.Token.PARENTHESIS_OPEN_TOKEN:
					s.push(token);
					break;
				case Tokenizer.Token.PARENTHESIS_CLOSE_TOKEN:
					if(s.isEmpty()) {
						return Result.syntacticalError();
					}

					// until '(' on stack, pop operators.
					while (s.peek().type != Tokenizer.Token.PARENTHESIS_OPEN_TOKEN) {
						sb.addLast(s.pop());
					}
					s.pop();
					break;
				default:
					if (s.isEmpty()) {
						s.push(token);
					} else {
						while (!s.isEmpty()) {
							int prec2 = s.peek().getPrecedence();
							int prec1 = token.getPrecedence();

							if (prec2 > prec1 || (prec2 == prec1 && token.type == Tokenizer.Token.EXP_TOKEN)) {
								sb.addLast(s.pop());
							} else {
								break;
							}
						}
						s.push(token);
					}
					break;
			}
		}

		while (!s.isEmpty()) {
			sb.addLast(s.pop());
		}

		return Result.result(sb);
	}

}
