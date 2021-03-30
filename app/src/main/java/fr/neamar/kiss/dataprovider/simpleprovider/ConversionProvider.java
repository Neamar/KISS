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
    final Pattern volConversionRegexp;
    final Pattern speedConversionRegexp;
    final Pattern areaConversionRegexp;
    private final Pattern numberOnlyRegexp;

    public ConversionProvider() {
        //This should try to match as much as possible without going out of the expression,
        //even if the expression is not actually a computable operation.
        String distUnitRegEx = "(miles|mile|mi|feet|foot|ft|inches|inch|in|millimeters|millimeter|mm|kilometers|kilometer|km|meters|meter|m|centimeters|centimeter|cm)";
        distConversionRegexp = Pattern.compile("(^\\-?\\d*.?\\d+)"+distUnitRegEx+"(to)"+distUnitRegEx);
        String volUnitRegEx = "(gallons|gallon|gal|quarts|quart|qrt|qt|pints|pint|pnt|pt|ounces|ounce|oz|liters|liter|l|milliliters|milliliter|ml|cups|cup|cp|tablespoons|tablespoon|tbsp|teaspoones|teaspoon|tsp)";
        volConversionRegexp = Pattern.compile("(^\\-?\\d*.?\\d+)"+volUnitRegEx+"(to)"+volUnitRegEx);
        String speedUnitRegEx = "(mph|m\\/h|inps|in\\/s|kph|k\\/p|mmps|mm\\/s)";
        speedConversionRegexp = Pattern.compile("(^\\-?\\d*.?\\d+)"+speedUnitRegEx+"(to)"+speedUnitRegEx);
        String areaUnitRegEx = "(mile\\^2||squaremiles|squaremile|mi\\^2|sqmi|squarefoot|squarefeet|sqft|ft\\^2|squareinches|squareinch|sqin|in\\^2|squaremeters|squaremeter|sqm|m\\^2)";
        areaConversionRegexp = Pattern.compile("(^\\-?\\d*.?\\d+)"+areaUnitRegEx+"(to)"+areaUnitRegEx);
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
            Log.v("Spooner", "Distance -> " + fromVal + " " + fromUnit + " to " + toUnit);

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
        m = volConversionRegexp.matcher(spacelessQuery);
        if (m.find()) {
            if(numberOnlyRegexp.matcher(spacelessQuery).find()) {
                return;
            }

            BigDecimal fromVal = new BigDecimal(Double.parseDouble(Objects.requireNonNull(m.group(1))));
            BigDecimal fromValml;
            String fromUnit = m.group(2);
            String toUnit = m.group(4);
            BigDecimal toValue;
            Log.v("Spooner", "Volume -> " + fromVal + " " + fromUnit + " to " + toUnit);

            switch (fromUnit){
                case "gallons":
                case "gallon":
                case "gal":
                case "g":
                    fromValml = fromVal.multiply(BigDecimal.valueOf(3785.41));
                    break;
                case "quarts":
                case "quart":
                case "qrt":
                case "qt":
                    fromValml = fromVal.multiply(BigDecimal.valueOf(946.353));
                    break;
                case "pints":
                case "pint":
                case "pnt":
                case "pt":
                    fromValml = fromVal.multiply(BigDecimal.valueOf(473.176));
                    break;
                case "ounces":
                case "ounce":
                case "oz":
                    fromValml = fromVal.multiply(BigDecimal.valueOf(29.5735));
                    break;
                case "liters":
                case "liter":
                case "l":
                    fromValml = fromVal.multiply(BigDecimal.valueOf(1000));
                    break;
                case "milliliters":
                case "milliliter":
                case "ml":
                    fromValml = fromVal;
                    break;
                case "cups":
                case "cup":
                case "cp":
                    fromValml = fromVal.multiply(BigDecimal.valueOf(240));
                    break;
                case "tablespoons":
                case "tablespoon":
                case "tbsp":
                    fromValml = fromVal.multiply(BigDecimal.valueOf(14.7868));
                    break;
                case "teaspoons":
                case "teaspoon":
                case "tsp":
                    fromValml = fromVal.multiply(BigDecimal.valueOf(4.9289317406874));
                    break;
                default:
                    fromValml = BigDecimal.valueOf(0);
                    break;
            }
            switch (toUnit){
                case "gallons":
                case "gallon":
                case "gal":
                case "g":
                    toValue = fromValml.divide(BigDecimal.valueOf(3785.41), 2, RoundingMode.HALF_UP);
                    break;
                case "quarts":
                case "quart":
                case "qrt":
                case "qt":
                    toValue = fromValml.divide(BigDecimal.valueOf(946.353), 2, RoundingMode.HALF_UP);
                    break;
                case "pints":
                case "pint":
                case "pnt":
                case "pt":
                    toValue = fromValml.divide(BigDecimal.valueOf(473.176), 2, RoundingMode.HALF_UP);
                    break;
                case "ounces":
                case "ounce":
                case "oz":
                    toValue = fromValml.divide(BigDecimal.valueOf(29.5735), 2, RoundingMode.HALF_UP);
                    break;
                case "liters":
                case "liter":
                case "l":
                    toValue = fromValml.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
                    break;
                case "milliliters":
                case "milliliter":
                case "ml":
                    toValue = fromValml;
                    break;
                case "cups":
                case "cup":
                case "cp":
                    toValue = fromValml.divide(BigDecimal.valueOf(240), 2, RoundingMode.HALF_UP);
                    break;
                case "tablespoons":
                case "tablespoon":
                case "tbsp":
                    toValue = fromValml.divide(BigDecimal.valueOf(14.7868), 2, RoundingMode.HALF_UP);
                    break;
                case "teaspoons":
                case "teaspoon":
                case "tsp":
                    toValue = fromValml.divide(BigDecimal.valueOf(4.9289317406874), 2, RoundingMode.HALF_UP);
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
