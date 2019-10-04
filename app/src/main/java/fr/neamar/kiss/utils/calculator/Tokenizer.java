package fr.neamar.kiss.utils.calculator;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayDeque;

import androidx.annotation.NonNull;

public class Tokenizer {
	public static final class Token {
		public static final int SUM_TOKEN = 0;
		public static final int SUBTRACT_TOKEN = 1;
		public static final int MULTIPLY_TOKEN = 2;
		public static final int DIVIDE_TOKEN = 3;
		public static final int EXP_TOKEN = 4;

		public static final int UNARY_PLUS_TOKEN = 8;
		public static final int UNARY_MINUS_TOKEN = 9;

		public static final int NUMBER_TOKEN = 16;
		public static final int PARENTHESIS_OPEN_TOKEN = 17;
		public static final int PARENTHESIS_CLOSE_TOKEN = 18;


		public final int type;
		public final BigDecimal number;

		public Token(int type) {
			if(type != SUM_TOKEN && type != SUBTRACT_TOKEN && type != MULTIPLY_TOKEN && type != DIVIDE_TOKEN
					&& type != EXP_TOKEN
					&& type != UNARY_PLUS_TOKEN && type != UNARY_MINUS_TOKEN) {
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
				case UNARY_PLUS_TOKEN:
				case UNARY_MINUS_TOKEN:
					return 4;
				case SUM_TOKEN:
				case SUBTRACT_TOKEN:
					return 1;
				case MULTIPLY_TOKEN:
				case DIVIDE_TOKEN:
					return 2;
				case EXP_TOKEN:
					return 3;
				default:
					return -1;
			}
		}

		public final boolean isRightAssociative() {
			switch (type) {
				case UNARY_PLUS_TOKEN:
				case UNARY_MINUS_TOKEN:
					return true;
				case SUM_TOKEN:
				case SUBTRACT_TOKEN:
				case MULTIPLY_TOKEN:
				case DIVIDE_TOKEN:
				case EXP_TOKEN:
					return false;
				default:
					throw new IllegalStateException();
			}
		}

		public final boolean isLeftAssociative() {
			return !isRightAssociative();
		}
	}

	public static Result<ArrayDeque<Token>> tokenize(String expression) {
		ArrayDeque<Token> tokens = new ArrayDeque<>();

		for (int i = 0; i < expression.length(); i++) {
			char operator = expression.charAt(i);

			Token token = null;

			switch (operator) {
				case '+':
					if(!tokens.isEmpty() && (tokens.peekLast().type == Token.NUMBER_TOKEN
							|| tokens.peekLast().type == Token.PARENTHESIS_CLOSE_TOKEN)) {
						token = new Token(Token.SUM_TOKEN);
					} else {
						token = new Token(Token.UNARY_PLUS_TOKEN);
					}
					break;
				case '-':
					if(!tokens.isEmpty() && (tokens.peekLast().type == Token.NUMBER_TOKEN
							|| tokens.peekLast().type == Token.PARENTHESIS_CLOSE_TOKEN)) {
						token = new Token(Token.SUBTRACT_TOKEN);
					} else {
						token = new Token(Token.UNARY_MINUS_TOKEN);
					}
					break;
				case '*':
					if(expression.length() > i+1 && expression.charAt(i+1) == '*') {// '**'
						i++;
						token = new Token(Token.EXP_TOKEN);
					} else {
						token = new Token(Token.MULTIPLY_TOKEN);
					}
					break;
				case '×':
				case 'x':
					token = new Token(Token.MULTIPLY_TOKEN);
					break;
				case '/':
				case '÷':
					token = new Token(Token.DIVIDE_TOKEN);
					break;
				case '^':
					token = new Token(Token.EXP_TOKEN);
					break;
				case '(':
					token = new Token(true);
					break;
				case ')':
					token = new Token(false);
					break;
				default:
					//Numbers
					StringBuilder numberBuilder = new StringBuilder();

					if(checkOperatorIsPartOfNumber(operator)) {
						numberBuilder.append(operator);

						while (i+1 < expression.length()) {
							operator = expression.charAt(i+1);
							if(checkOperatorIsPartOfNumber(operator)) {
								numberBuilder.append(operator);
								i++;
							} else {
								break;
							}
						}
					}

					if(numberBuilder.length() != 0) {
						DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance();
						decimalFormat.setParseBigDecimal(true);

						BigDecimal number = null;

						try {
							number = (BigDecimal) decimalFormat.parse(numberBuilder.toString());
						} catch (ParseException e) {
							return Result.syntacticalError();
						}

						token = new Token(number);
					}
			}

			if(token == null) {
				continue;
			}

			tokens.addLast(token);
		}

		return Result.result(tokens);
	}

	private static boolean checkOperatorIsPartOfNumber(char operator) {
		return Character.isDigit(operator) || operator == '.' || operator == ',' || operator == 'E'
				|| operator == ' ' || operator == '\'';
	}
}
