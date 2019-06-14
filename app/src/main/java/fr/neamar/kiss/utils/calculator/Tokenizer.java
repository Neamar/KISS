package fr.neamar.kiss.utils.calculator;

import android.view.inputmethod.InputConnection;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Stack;

import androidx.annotation.NonNull;

public class Tokenizer {
	public static final class Token {
		public static final int SUM_TOKEN = 0;
		public static final int SUBTRACT_TOKEN = 1;
		public static final int MULTIPLY_TOKEN = 2;
		public static final int DIVIDE_TOKEN = 3;
		public static final int EXP_TOKEN = 4;

		public static final int NUMBER_TOKEN = 16;
		public static final int PARENTHESIS_OPEN_TOKEN = 17;
		public static final int PARENTHESIS_CLOSE_TOKEN = 18;


		public final int type;
		public final BigDecimal number;

		public Token(int type) {
			if(type != SUM_TOKEN && type != SUBTRACT_TOKEN && type != MULTIPLY_TOKEN && type != DIVIDE_TOKEN) {
				throw new IllegalArgumentException("Wrong constructor!");
			}
			this.type = type;
			number = null;
		}

		public Token(@NonNull BigDecimal number) {
			this.type = NUMBER_TOKEN;
			this.number = number;
		}

		public Token(boolean isParenthesisOpen) {
			this.type = isParenthesisOpen? PARENTHESIS_OPEN_TOKEN : PARENTHESIS_CLOSE_TOKEN;
			this.number = null;
		}

		public final int getPrecedence() {
			switch (type) {
				case SUM_TOKEN:
				case SUBTRACT_TOKEN:
					return 0;
				case MULTIPLY_TOKEN:
				case DIVIDE_TOKEN:
					return 1;
				case EXP_TOKEN:
					return 2;
				default:
					return -1;
			}
		}
	}

	public static final ArrayDeque<Token> tokenize(String expression) {
		ArrayDeque<Token> tokens = new ArrayDeque<>();

		for (int i = 0; i < expression.length(); i++) {
			char operator = expression.charAt(i);

			Token token = null;

			switch (operator) {
				case '+':
					token = new Token(Token.SUM_TOKEN);
					break;
				case '-':
					token = new Token(Token.SUBTRACT_TOKEN);
					break;
				case '*':
				case '×':
				case 'x':
					token = new Token(Token.MULTIPLY_TOKEN);
					break;
				case '/':
				case '÷':
					token = new Token(Token.DIVIDE_TOKEN);
					break;
				case '(':
					token = new Token(true);
					break;
				case ')':
					token = new Token(false);
					break;
				default:
					StringBuilder number = new StringBuilder();

					if(Character.isDigit(operator) || operator == '.' || operator == ',') {
						number.append(operator);

						while (i+1 < expression.length()) {
							operator = expression.charAt(i+1);
							if(Character.isDigit(operator) || operator == '.' || operator == ',') {
								number.append(operator);
								i++;
							} else {
								break;
							}
						}
					}

					if(number.length() != 0) {
						token = new Token(new BigDecimal(number.toString()));
					}
			}

			if(token == null) {
				continue;
			}

			tokens.addLast(token);
		}

		return tokens;
	}
}
