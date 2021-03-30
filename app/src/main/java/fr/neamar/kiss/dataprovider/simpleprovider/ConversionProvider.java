package fr.neamar.kiss.dataprovider.simpleprovider;

import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;

public class ConversionProvider extends SimpleProvider {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    final Pattern distConversionRegexp;
    private final Pattern numberOnlyRegexp;

    public ConversionProvider() {
        //This should try to match as much as possible without going out of the expression,
        //even if the expression is not actually a computable operation.
        String distUnitRegEx = "(miles|mile|mi|feet|foot|ft|inches|inch|in|millimeters|millimeter|mm|kilometers|kilometer|km|meters|meter|m|centimeters|centimeter|cm)";
        distConversionRegexp = Pattern.compile("(^\\-?\\d*.?\\d+)"+distUnitRegEx+"(to)"+distUnitRegEx);
        String volUnitRegEx = "(gallons|gallon|gal|quart|q)";
        numberOnlyRegexp = Pattern.compile("^\\+?[.,()\\d]+$");
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        Log.v("Spooner", "Conv Query: `" + query);
        String spacelessQuery = query.replaceAll("\\s+", "");
        Matcher m = distConversionRegexp.matcher(spacelessQuery);

        if (m.find()) {
            if(numberOnlyRegexp.matcher(spacelessQuery).find()) {
                return;
            }

            BigDecimal fromVal = new BigDecimal(Double.parseDouble(Objects.requireNonNull(m.group(1))));
            BigDecimal fromValmm;
            String fromUnit = m.group(2);
            String toUnit = m.group(4);
            BigDecimal toValue;
            Log.v("Spooner", "m.group() -> " + fromVal + " " + fromUnit + " to " + toUnit);

            switch (fromUnit){
                case "miles":
                case "mile":
                case "mi":
                    fromValmm = fromVal.multiply(BigDecimal.valueOf(1609344));
                    break;
                case "feet":
                case "foot":
                case "ft":
                    fromValmm = fromVal.multiply(BigDecimal.valueOf(304.8));
                    break;
                case "inches":
                case "inch":
                case "in":
                    fromValmm = fromVal.multiply(BigDecimal.valueOf(25.4));
                    break;
                case "millimeters":
                case "milimeter":
                case "mm":
                    fromValmm = fromVal;
                    break;
                case "kilometers":
                case "kilometer":
                case "km":
                    fromValmm = fromVal.multiply(BigDecimal.valueOf(1000000));
                    break;
                case "meters":
                case "meter":
                case "m":
                    fromValmm = fromVal.multiply(BigDecimal.valueOf(1000));
                    break;
                case "centimeters":
                case "centimeter":
                case "cm":
                    fromValmm = fromVal.multiply(BigDecimal.valueOf(10));
                    break;
                default:
                    fromValmm = BigDecimal.valueOf(0);
                    break;

            }
            switch (toUnit){
                case "miles":
                case "mile":
                case "mi":
                    toValue = fromValmm.divide(BigDecimal.valueOf(1609344), 2, RoundingMode.HALF_UP);
                    break;
                case "feet":
                case "foot":
                case "ft":
                    toValue = fromValmm.divide(BigDecimal.valueOf(304.8), 2, RoundingMode.HALF_UP);
                    break;
                case "inches":
                case "inch":
                case "in":
                    toValue = fromValmm.divide(BigDecimal.valueOf(25.4), 2, RoundingMode.HALF_UP);
                    break;
                case "millimeters":
                case "milimeter":
                case "mm":
                    toValue = fromValmm;
                    break;
                case "kilometers":
                case "kilometer":
                case "km":
                    toValue = fromValmm.divide(BigDecimal.valueOf(1000000), 2, RoundingMode.HALF_UP);
                    break;
                case "meters":
                case "meter":
                case "m":
                    toValue = fromValmm.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
                    break;
                case "centimeters":
                case "centimeter":
                case "cm":
                    toValue = fromValmm.divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
                    break;
                default:
                    toValue = BigDecimal.valueOf(0);
                    break;

            }

            String queryProcessed = query + " = " + toValue;
            SearchPojo pojo = new SearchPojo("conversion://", queryProcessed, "", SearchPojo.CONVERSION_QUERY);

            pojo.relevance = 19;
            searcher.addResult(pojo);
        }

    }
}
