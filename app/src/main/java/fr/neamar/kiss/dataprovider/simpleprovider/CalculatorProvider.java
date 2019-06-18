package fr.neamar.kiss.dataprovider.simpleprovider;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.VisibleForTesting;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.calculator.Calculator;
import fr.neamar.kiss.utils.calculator.Result;
import fr.neamar.kiss.utils.calculator.ShuntingYard;
import fr.neamar.kiss.utils.calculator.Tokenizer;

public class CalculatorProvider extends SimpleProvider {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    Pattern p;

    public CalculatorProvider() {
        //This should try to match as much as possible without going out of the expression,
        //even if the expression is not actually a computable operation.
        p = Pattern.compile("\\(*[+\\-]?\\d*\\.?\\d+[()\\d.+\\-*/^]*\\d*\\.?\\d+\\)*");
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        // Now create matcher object.
        Matcher m = p.matcher(query.trim());
        if (m.find()) {
            String operation = m.group();

            ArrayDeque<Tokenizer.Token> tokenized = Tokenizer.tokenize(operation);
            Result<ArrayDeque<Tokenizer.Token>> posfixed = ShuntingYard.infixToPostfix(tokenized);

            String readableResult;

            if(posfixed.syntacticalError) {
                return;
            } else if(posfixed.arithmeticalError) {
                readableResult = "ARITHMETIC ERROR";
            } else {
                Result<BigDecimal> result = Calculator.calculateExpression(posfixed.result);

                if(result.syntacticalError) {
                    return;
                } else if(result.arithmeticalError) {
                    readableResult = "ARITHMETIC ERROR";
                } else {
                    readableResult = " = " + result.result.toPlainString();
                }
            }

            String queryProcessed = operation + readableResult;
            SearchPojo pojo = new SearchPojo("calculator://", queryProcessed, "", SearchPojo.CALCULATOR_QUERY);

            pojo.relevance = 100;
            searcher.addResult(pojo);
        }
    }
}
