package fr.neamar.kiss.dataprovider.simpleprovider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.dataprovider.IProvider;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;

public class CalculatorProvider implements IProvider {
    private Pattern p;

    public CalculatorProvider() {
        p = Pattern.compile("([0-9.]+)\\s?([+\\-*/×x÷])\\s?([0-9.]+)");
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        // Now create matcher object.
        Matcher m = p.matcher(query);
        if (m.find()) {
            SearchPojo pojo = new SearchPojo();
            pojo.id = "calculator://";
            pojo.type = SearchPojo.CALCULATOR_QUERY;

            String operator = m.group(2);

            // let's go for floating point arithmetic
            // we need to add a "0" on top of it to support ".2" => 0.2
            // For every other case, this doesn't change the number "01" => 1
            float lhs = Float.parseFloat("0" + m.group(1));
            float rhs = Float.parseFloat("0" + m.group(3));

            float floatResult = 0;
            switch (operator) {
                case "+":
                    floatResult = lhs + rhs;
                    break;
                case "-":
                    floatResult = lhs - rhs;
                    break;
                case "*":
                case "×":
                case "x":
                    floatResult = lhs * rhs;
                    operator = "×";
                    break;
                case "/":
                case "÷":
                    floatResult = lhs / rhs;
                    operator = "÷";
                    break;
                default:
                    floatResult = Float.POSITIVE_INFINITY;
            }

            pojo.query = floatToString(lhs) + " " + operator + " " + floatToString(rhs) + " = " + floatToString(floatResult);
            pojo.relevance = 100;
            searcher.addResult(pojo);
        }
    }

    private String floatToString(float f) {
        // If f is an int, we don't want to display 9.0: cast to int
        if (f == Math.round(f)) {
            return Integer.toString(Math.round(f));
        } else {
            // otherwise, keep it as float, knowing that some floating-point issues can happen
            // (try for instance 0.3 - 0.2)
            return Float.toString(f);
        }
    }

    @Override
    public void reload() {
        // Simple providers can't be reloaded
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean mayFindById(String id) {
        return false;
    }

    @Override
    public Pojo findById(String id) {
        return null;
    }
}
