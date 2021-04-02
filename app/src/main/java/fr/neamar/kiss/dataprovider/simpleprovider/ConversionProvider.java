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
                BigDecimal toValue = BigDecimal.valueOf(0);
                Log.v("Spooner", e.getKey() +" -> " + fromVal + " " + fromUnit + " to " + toUnit);

                if (e.getKey().equals("temp") && fromUnit != null && toUnit != null){
                    if (fromUnit.startsWith("f") && toUnit.startsWith("c")){
                        toValue = fromVal.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5.0/9), MathContext.DECIMAL32);
                    } else if (fromUnit.startsWith("c") && toUnit.startsWith("f")) {
                        toValue = fromVal.multiply(BigDecimal.valueOf(9.0/5)).add(BigDecimal.valueOf(32), MathContext.DECIMAL32);
                    } else if (fromUnit.startsWith("f") && toUnit.startsWith("k")) {
                        toValue = fromVal.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5.0/9), MathContext.DECIMAL32).add(BigDecimal.valueOf(255.372));
                    } else if (fromUnit.startsWith("k") && toUnit.startsWith("f")) {
                        toValue = fromVal.subtract(BigDecimal.valueOf(273.15)).multiply(BigDecimal.valueOf(9.0/5)).add(BigDecimal.valueOf(32), MathContext.DECIMAL32);
                    } else if (fromUnit.startsWith("c") && toUnit.startsWith("k")) {
                        toValue = fromVal.add(BigDecimal.valueOf(273.15));
                    } else if (fromUnit.startsWith("k") && toUnit.startsWith("c")) {
                        toValue = fromVal.subtract(BigDecimal.valueOf(273.15));
                    } else if (fromUnit.startsWith(toUnit.substring(0,1))) {
                        toValue = fromVal;
                    }
                } else{
                    fromValmin = fromVal.multiply(Converter.getUnit(e.getKey(), fromUnit));
                    toValue = fromValmin.divide(Converter.getUnit(e.getKey(), toUnit), MathContext.DECIMAL32);
                }



                String queryProcessed = query + " = " + toValue;
                SearchPojo pojo = new SearchPojo("conversion://", queryProcessed, "", SearchPojo.CONVERSION_QUERY);

                pojo.relevance = 19;
                searcher.addResult(pojo);
            }
        }
    }
}
