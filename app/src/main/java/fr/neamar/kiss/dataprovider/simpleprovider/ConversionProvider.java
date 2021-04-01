package fr.neamar.kiss.dataprovider.simpleprovider;

import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.conversion.Converter;

public class ConversionProvider extends SimpleProvider {
    private HashMap<String, Pattern> conversionRegExps;
    private Pattern numberOnlyRegexp;


    public ConversionProvider() {
        //This should try to match as much as possible without going out of the expression,
        //even if the expression is not actually a computable operation.
        conversionRegExps = new HashMap<>();
        for (String unitType : Converter.getTypes()){
            String unitRegEx = Converter.getRegExUnitsString(unitType);
            conversionRegExps.put(unitType, Pattern.compile("(^-?\\d*\\.?\\d+)"+unitRegEx+"(to)"+unitRegEx+"$"));
        }
        numberOnlyRegexp = Pattern.compile("^\\+?[.,()\\d]+$");

    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        String spacelessQuery = query.replaceAll("\\s+", "");
        spacelessQuery = spacelessQuery.toLowerCase();
        for (Map.Entry<String, Pattern> e : conversionRegExps.entrySet()){
            Matcher m = e.getValue().matcher(spacelessQuery);
            if (m.find()) {
                if(numberOnlyRegexp.matcher(spacelessQuery).find()) {
                    return;
                }

                BigDecimal fromVal;
                String fv = m.group(1);
                if (fv != null) {
                    fromVal = BigDecimal.valueOf(Double.parseDouble(fv));
                } else {
                    return;
                }

                BigDecimal fromValmin;
                String fromUnit = m.group(2);
                String toUnit = m.group(4);
                BigDecimal toValue;
                Log.v("Spooner", e.getKey() +" -> " + fromVal + " " + fromUnit + " to " + toUnit);

                fromValmin = fromVal.multiply(Converter.getUnit(e.getKey(), fromUnit));
                toValue = fromValmin.divide(Converter.getUnit(e.getKey(), toUnit), MathContext.DECIMAL32);

                String queryProcessed = query + " = " + toValue;
                SearchPojo pojo = new SearchPojo("conversion://", queryProcessed, "", SearchPojo.CONVERSION_QUERY);

                pojo.relevance = 19;
                searcher.addResult(pojo);
            }
        }
    }
}
