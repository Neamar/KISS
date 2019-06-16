package fr.neamar.kiss.dataprovider.simpleprovider;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.calculator.Calculator;
import fr.neamar.kiss.utils.calculator.ShuntingYard;
import fr.neamar.kiss.utils.calculator.Tokenizer;

public class CalculatorProvider extends SimpleProvider {
    private Pattern p;

    public CalculatorProvider() {
        p = Pattern.compile("^(-?)([0-9.]+)\\s?([+\\-*/×x÷^])\\s?(-?)([0-9.]+)$");
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        // Now create matcher object.
        Matcher m = p.matcher(query);
        if (m.find()) {
            String operation = m.group();

            ArrayDeque<Tokenizer.Token> tokenized = Tokenizer.tokenize(operation);
            ArrayDeque<Tokenizer.Token> posfixed = ShuntingYard.infixToPostfix(tokenized);
            BigDecimal result = Calculator.calculateExpression(posfixed);

            String queryProcessed = operation + " = " + result.toPlainString();
            SearchPojo pojo = new SearchPojo("calculator://", queryProcessed, "", SearchPojo.CALCULATOR_QUERY);

            pojo.relevance = 100;
            searcher.addResult(pojo);
        }
    }
}
