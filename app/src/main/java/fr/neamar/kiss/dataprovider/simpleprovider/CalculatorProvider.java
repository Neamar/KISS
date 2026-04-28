package fr.neamar.kiss.dataprovider.simpleprovider;

import androidx.annotation.VisibleForTesting;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.pojo.SearchPojoType;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.calculator.Calculator;
import fr.neamar.kiss.utils.calculator.Result;
import fr.neamar.kiss.utils.calculator.ShuntingYard;
import fr.neamar.kiss.utils.calculator.Tokenizer;

public class CalculatorProvider extends SimpleProvider<SearchPojo> {
    @VisibleForTesting()
    final Pattern computableRegexp;
    // A regexp to detect plain numbers (including phone numbers)
    private final Pattern numberOnlyRegexp;
    // Trailing "+ N%" / "- N%" applied to the preceding base expression.
    // Group 1: base expression (must not contain '%'). Group 2: '+' or '-'. Group 3: percentage value.
    private final Pattern trailingPercentRegexp;
    private final NumberFormat LOCALIZED_NUMBER_FORMATTER = NumberFormat.getInstance();
    private static final BigDecimal HUNDRED = new BigDecimal(100);

    public CalculatorProvider() {
        //This should try to match as much as possible without going out of the expression,
        //even if the expression is not actually a computable operation.
        computableRegexp = Pattern.compile("^[\\-.,\\d+*×x/÷^'()%]+$");
        numberOnlyRegexp = Pattern.compile("^\\+?[.,()\\d]+$");
        trailingPercentRegexp = Pattern.compile("^([^%]+?)([+\\-])(\\d+(?:[.,]\\d+)?)%$");
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        String spacelessQuery = query.replaceAll("\\s+", "");
        // Now create matcher object.
        Matcher m = computableRegexp.matcher(spacelessQuery);
        if (m.find()) {
            if (numberOnlyRegexp.matcher(spacelessQuery).find()) {
                return;
            }

            String operation = m.group();
            BigDecimal value = compute(operation);
            if (value == null) {
                return;
            }

            String queryProcessed = operation + " = " + LOCALIZED_NUMBER_FORMATTER.format(value);
            SearchPojo pojo = new SearchPojo("calculator://", queryProcessed, "", SearchPojoType.CALCULATOR_QUERY);

            pojo.relevance = 19;
            searcher.addResult(pojo);
        }
    }

    @VisibleForTesting
    BigDecimal compute(String spacelessQuery) {
        Matcher percentMatcher = trailingPercentRegexp.matcher(spacelessQuery);
        if (percentMatcher.matches()) {
            BigDecimal base = evaluate(percentMatcher.group(1));
            if (base == null) {
                return null;
            }
            BigDecimal percent = new BigDecimal(percentMatcher.group(3).replace(",", "."));
            BigDecimal delta = base.multiply(percent).divide(HUNDRED, MathContext.DECIMAL32);
            return percentMatcher.group(2).equals("+") ? base.add(delta) : base.subtract(delta);
        }
        return evaluate(spacelessQuery);
    }

    private static BigDecimal evaluate(String expression) {
        Result<ArrayDeque<Tokenizer.Token>> tokenized = Tokenizer.tokenize(expression);
        if (tokenized.syntacticalError || tokenized.arithmeticalError) {
            return null;
        }
        Result<ArrayDeque<Tokenizer.Token>> postfixed = ShuntingYard.infixToPostfix(tokenized.result);
        if (postfixed.syntacticalError || postfixed.arithmeticalError) {
            return null;
        }
        Result<BigDecimal> result = Calculator.calculateExpression(postfixed.result);
        if (result.syntacticalError || result.arithmeticalError) {
            return null;
        }
        return result.result;
    }
}
