package fr.neamar.kiss.dataprovider.simpleprovider;

import androidx.annotation.VisibleForTesting;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.calculator.Calculator;
import fr.neamar.kiss.utils.calculator.Result;
import fr.neamar.kiss.utils.calculator.ShuntingYard;
import fr.neamar.kiss.utils.calculator.Tokenizer;

public class CalculatorProvider extends SimpleProvider {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    final Pattern computableRegexp;
    // A regexp to detect plain numbers (including phone numbers)
    final Pattern numberOnlyRegexp;
    private final NumberFormat LOCALIZED_NUMBER_FORMATTER = NumberFormat.getInstance();

    public CalculatorProvider() {
        //This should try to match as much as possible without going out of the expression,
        //even if the expression is not actually a computable operation.
        computableRegexp = Pattern.compile("^[\\-.,\\d+*/^'()]+$");
        numberOnlyRegexp = Pattern.compile("^\\+?[.,()\\d]+$");
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        String spacelessQuery = query.replaceAll("\\s+", "");
        // Now create matcher object.
        Matcher m = computableRegexp.matcher(spacelessQuery);
        if (m.find()) {
            if(numberOnlyRegexp.matcher(spacelessQuery).find()) {
                return;
            }

            String operation = m.group();

            Result<ArrayDeque<Tokenizer.Token>> tokenized = Tokenizer.tokenize(operation);
            String readableResult;

            if(tokenized.syntacticalError) {
                return;
            } else if(tokenized.arithmeticalError) {
                return;
            } else {
                Result<ArrayDeque<Tokenizer.Token>> posfixed = ShuntingYard.infixToPostfix(tokenized.result);

                if (posfixed.syntacticalError) {
                    return;
                } else if (posfixed.arithmeticalError) {
                    return;
                } else {
                    Result<BigDecimal> result = Calculator.calculateExpression(posfixed.result);

                    if (result.syntacticalError) {
                        return;
                    } else if (result.arithmeticalError) {
                        return;
                    } else {
                        String localizedNumber = LOCALIZED_NUMBER_FORMATTER.format(result.result);
                        readableResult = " = " + localizedNumber;
                    }
                }
            }

            String queryProcessed = operation + readableResult;
            SearchPojo pojo = new SearchPojo("calculator://", queryProcessed, "", SearchPojo.CALCULATOR_QUERY);

            pojo.relevance = 19;
            searcher.addResult(pojo);
        }
    }
}
