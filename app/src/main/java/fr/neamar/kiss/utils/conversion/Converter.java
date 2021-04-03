package fr.neamar.kiss.utils.conversion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.TreeMap;

public class Converter {
    private static TreeMap<String, BigDecimal> distUnits;
    private static TreeMap<String, BigDecimal> volUnits;
    private static TreeMap<String, BigDecimal> spdUnits;
    private static TreeMap<String, BigDecimal> areaUnits;
    private static TreeMap<String, BigDecimal> massUnits;
    private static TreeMap<String, BigDecimal> tempUnits;
    private static TreeMap<String, BigDecimal> timeUnits;

    private static HashMap<String, TreeMap<String, BigDecimal>> units;

    //all distance units in terms of mm, conversion via google and https://www.convertunits.com
    static {
        distUnits = new TreeMap<>();
        distUnits.put("miles", BigDecimal.valueOf(1609344));
        distUnits.put("mile", BigDecimal.valueOf(1609344));
        distUnits.put("mi", BigDecimal.valueOf(1609344));
        distUnits.put("yards", BigDecimal.valueOf(914.4));
        distUnits.put("yard", BigDecimal.valueOf(914.4));
        distUnits.put("yd", BigDecimal.valueOf(914.4));
        distUnits.put("feet", BigDecimal.valueOf(304.8));
        distUnits.put("foot", BigDecimal.valueOf(304.8));
        distUnits.put("ft", BigDecimal.valueOf(304.8));
        distUnits.put("inches", BigDecimal.valueOf(25.4));
        distUnits.put("inch", BigDecimal.valueOf(25.4));
        distUnits.put("in", BigDecimal.valueOf(25.4));
        distUnits.put("millimeters", BigDecimal.valueOf(1));
        distUnits.put("millimeter", BigDecimal.valueOf(1));
        distUnits.put("mm", BigDecimal.valueOf(1));
        distUnits.put("kilometers", BigDecimal.valueOf(1000000));
        distUnits.put("kilometer", BigDecimal.valueOf(1000000));
        distUnits.put("km", BigDecimal.valueOf(1000000));
        distUnits.put("meters", BigDecimal.valueOf(1000));
        distUnits.put("meter", BigDecimal.valueOf(1000));
        distUnits.put("m", BigDecimal.valueOf(1000));
        distUnits.put("centimeters", BigDecimal.valueOf(10));
        distUnits.put("centimeter", BigDecimal.valueOf(10));
        distUnits.put("cm", BigDecimal.valueOf(10));
    }

    //all volume units in terms of ml, conversion via google and https://www.convertunits.com
    static {
        volUnits = new TreeMap<>();
        volUnits.put("cubicmiles", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("cubicmile", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("cumiles", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("cumile", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("cumi", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("miles^3", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("miles3", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("mile^3", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("mile3", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("mi^3", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("mi3", BigDecimal.valueOf(4168181843058500.0));
        volUnits.put("cubicfeet", BigDecimal.valueOf(28316.846711688));
        volUnits.put("cubicfoot", BigDecimal.valueOf(28316.846711688));
        volUnits.put("cubicft", BigDecimal.valueOf(28316.846711688));
        volUnits.put("cufeet", BigDecimal.valueOf(28316.846711688));
        volUnits.put("cufoot", BigDecimal.valueOf(28316.846711688));
        volUnits.put("cuft", BigDecimal.valueOf(28316.846711688));
        volUnits.put("feet^3", BigDecimal.valueOf(28316.846711688));
        volUnits.put("feet3", BigDecimal.valueOf(28316.846711688));
        volUnits.put("foot^3", BigDecimal.valueOf(28316.846711688));
        volUnits.put("foot3", BigDecimal.valueOf(28316.846711688));
        volUnits.put("ft^3", BigDecimal.valueOf(28316.846711688));
        volUnits.put("ft3", BigDecimal.valueOf(28316.846711688));
        volUnits.put("cubicinches", BigDecimal.valueOf(16.387064069264));
        volUnits.put("cubicinch", BigDecimal.valueOf(16.387064069264));
        volUnits.put("cuinches", BigDecimal.valueOf(16.387064069264));
        volUnits.put("cuinch", BigDecimal.valueOf(16.387064069264));
        volUnits.put("cuin", BigDecimal.valueOf(16.387064069264));
        volUnits.put("inches^3", BigDecimal.valueOf(16.387064069264));
        volUnits.put("inches3", BigDecimal.valueOf(16.387064069264));
        volUnits.put("inch^3", BigDecimal.valueOf(16.387064069264));
        volUnits.put("inch3", BigDecimal.valueOf(16.387064069264));
        volUnits.put("in^3", BigDecimal.valueOf(16.387064069264));
        volUnits.put("in3", BigDecimal.valueOf(16.387064069264));
        volUnits.put("cubickilometers", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("cubickilometer", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("cukilometers", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("cukilometer", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("cukm", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("kilometers^3", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("kilometers3", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("kilometer^3", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("kilometer3", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("km^3", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("km3", BigDecimal.valueOf(1000000000000000.0));
        volUnits.put("cubicmeters", BigDecimal.valueOf(1000000));
        volUnits.put("cubicmeter", BigDecimal.valueOf(1000000));
        volUnits.put("cumeters", BigDecimal.valueOf(1000000));
        volUnits.put("cumeter", BigDecimal.valueOf(1000000));
        volUnits.put("cum", BigDecimal.valueOf(1000000));
        volUnits.put("meters^3", BigDecimal.valueOf(1000000));
        volUnits.put("meters3", BigDecimal.valueOf(1000000));
        volUnits.put("meter^3", BigDecimal.valueOf(1000000));
        volUnits.put("meter3", BigDecimal.valueOf(1000000));
        volUnits.put("m^3", BigDecimal.valueOf(1000000));
        volUnits.put("m3", BigDecimal.valueOf(1000000));
        volUnits.put("cubiccentimeters", BigDecimal.valueOf(1));
        volUnits.put("cubiccentimeter", BigDecimal.valueOf(1));
        volUnits.put("cucentimeters", BigDecimal.valueOf(1));
        volUnits.put("cucentimeter", BigDecimal.valueOf(1));
        volUnits.put("cucm", BigDecimal.valueOf(1));
        volUnits.put("centimeters^3", BigDecimal.valueOf(1));
        volUnits.put("centimeters3", BigDecimal.valueOf(1));
        volUnits.put("centimeter^3", BigDecimal.valueOf(1));
        volUnits.put("centimeter3", BigDecimal.valueOf(1));
        volUnits.put("cm^3", BigDecimal.valueOf(1));
        volUnits.put("cm3", BigDecimal.valueOf(1));
        volUnits.put("cubicmillimeters", BigDecimal.valueOf(0.001));
        volUnits.put("cubicmillimeter", BigDecimal.valueOf(0.001));
        volUnits.put("cumillimeters", BigDecimal.valueOf(0.001));
        volUnits.put("cumillimeter", BigDecimal.valueOf(0.001));
        volUnits.put("cumm", BigDecimal.valueOf(0.001));
        volUnits.put("millimeters^3", BigDecimal.valueOf(0.001));
        volUnits.put("millimeters3", BigDecimal.valueOf(0.001));
        volUnits.put("millimeter^3", BigDecimal.valueOf(0.001));
        volUnits.put("millimeter3", BigDecimal.valueOf(0.001));
        volUnits.put("mm^3", BigDecimal.valueOf(0.001));
        volUnits.put("mm3", BigDecimal.valueOf(0.001));
        volUnits.put("gallons", BigDecimal.valueOf(3785.41));
        volUnits.put("gallon", BigDecimal.valueOf(3785.41));
        volUnits.put("gal", BigDecimal.valueOf(3785.41));
        volUnits.put("g", BigDecimal.valueOf(3785.41));
        volUnits.put("quarts", BigDecimal.valueOf(946.353));
        volUnits.put("quart", BigDecimal.valueOf(946.353));
        volUnits.put("qrt", BigDecimal.valueOf(946.353));
        volUnits.put("qt", BigDecimal.valueOf(946.353));
        volUnits.put("pints", BigDecimal.valueOf(473.176));
        volUnits.put("pint", BigDecimal.valueOf(473.176));
        volUnits.put("pnt", BigDecimal.valueOf(473.176));
        volUnits.put("pt", BigDecimal.valueOf(473.176));
        volUnits.put("ounces", BigDecimal.valueOf(29.5735));
        volUnits.put("ounce", BigDecimal.valueOf(29.5735));
        volUnits.put("oz", BigDecimal.valueOf(29.5735));
        volUnits.put("liters", BigDecimal.valueOf(1000));
        volUnits.put("liter", BigDecimal.valueOf(1000));
        volUnits.put("l", BigDecimal.valueOf(1000));
        volUnits.put("milliliters", BigDecimal.valueOf(1));
        volUnits.put("milliliter", BigDecimal.valueOf(1));
        volUnits.put("ml", BigDecimal.valueOf(1));
        volUnits.put("cups", BigDecimal.valueOf(240));
        volUnits.put("cup", BigDecimal.valueOf(240));
        volUnits.put("cp", BigDecimal.valueOf(240));
        volUnits.put("tablespoons", BigDecimal.valueOf(14.7868));
        volUnits.put("tablespoon", BigDecimal.valueOf(14.7868));
        volUnits.put("tbsp", BigDecimal.valueOf(14.7868));
        volUnits.put("teaspoons", BigDecimal.valueOf(4.9289317406874));
        volUnits.put("teaspoon", BigDecimal.valueOf(4.9289317406874));
        volUnits.put("tsp", BigDecimal.valueOf(4.9289317406874));
    }

    //all speed units in terms of mm/s, conversion via google and https://www.convertunits.com
    static {
        spdUnits = new TreeMap<>();
        spdUnits.put("mph", BigDecimal.valueOf(447.04));
        spdUnits.put("m/h", BigDecimal.valueOf(447.04));
        spdUnits.put("ftps", BigDecimal.valueOf(304.8));
        spdUnits.put("fps", BigDecimal.valueOf(304.8));
        spdUnits.put("ft/s", BigDecimal.valueOf(304.8));
        spdUnits.put("f/s", BigDecimal.valueOf(304.8));
        spdUnits.put("inps", BigDecimal.valueOf(25.4));
        spdUnits.put("in/s", BigDecimal.valueOf(25.4));
        spdUnits.put("kmph", BigDecimal.valueOf(277.778));
        spdUnits.put("kph", BigDecimal.valueOf(277.778));
        spdUnits.put("km/h", BigDecimal.valueOf(277.778));
        spdUnits.put("k/h", BigDecimal.valueOf(277.778));
        spdUnits.put("mps", BigDecimal.valueOf(1000));
        spdUnits.put("m/s", BigDecimal.valueOf(1000));
        spdUnits.put("cmps", BigDecimal.valueOf(10));
        spdUnits.put("cps", BigDecimal.valueOf(10));
        spdUnits.put("cm/s", BigDecimal.valueOf(10));
        spdUnits.put("c/s", BigDecimal.valueOf(10));
        spdUnits.put("mmps", BigDecimal.valueOf(1));
        spdUnits.put("mm/s", BigDecimal.valueOf(1));
    }

    //all area units in terms of mm^2, conversion via google and https://www.convertunits.com
    static {
        areaUnits = new TreeMap<>();
        areaUnits.put("squaremiles", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("squaremile", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("sqmiles", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("sqmile", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("sqmi", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("mile^2", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("mi^2", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("mi2", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("acre", BigDecimal.valueOf(4046856422.4));
        areaUnits.put("ac", BigDecimal.valueOf(4046856422.4));
        areaUnits.put("squarefeet", BigDecimal.valueOf(92903));
        areaUnits.put("squarefoot", BigDecimal.valueOf(92903));
        areaUnits.put("sqfeet", BigDecimal.valueOf(92903));
        areaUnits.put("sqfooft", BigDecimal.valueOf(92903));
        areaUnits.put("sqft", BigDecimal.valueOf(92903));
        areaUnits.put("feet^2", BigDecimal.valueOf(92903));
        areaUnits.put("feet2", BigDecimal.valueOf(92903));
        areaUnits.put("foot^2", BigDecimal.valueOf(92903));
        areaUnits.put("foot2", BigDecimal.valueOf(92903));
        areaUnits.put("ft^2", BigDecimal.valueOf(92903));
        areaUnits.put("ft2", BigDecimal.valueOf(92903));
        areaUnits.put("squareinches", BigDecimal.valueOf(645.16));
        areaUnits.put("squareinch", BigDecimal.valueOf(645.16));
        areaUnits.put("sqinches", BigDecimal.valueOf(645.16));
        areaUnits.put("sqinch", BigDecimal.valueOf(645.16));
        areaUnits.put("sqin", BigDecimal.valueOf(645.16));
        areaUnits.put("inches^2", BigDecimal.valueOf(645.16));
        areaUnits.put("inches2", BigDecimal.valueOf(645.16));
        areaUnits.put("inch^2", BigDecimal.valueOf(645.16));
        areaUnits.put("inch2", BigDecimal.valueOf(645.16));
        areaUnits.put("in^2", BigDecimal.valueOf(645.16));
        areaUnits.put("in2", BigDecimal.valueOf(645.16));
        areaUnits.put("squarekilometers", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("squarekilometer", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("sqkilometers", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("sqkilometer", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("sqkm", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("kilometers^2", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("kilometers2", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("kilometer^2", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("kilometer2", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("km^2", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("km2", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("squaremeters", BigDecimal.valueOf(1000000));
        areaUnits.put("squaremeter", BigDecimal.valueOf(1000000));
        areaUnits.put("sqmeters", BigDecimal.valueOf(1000000));
        areaUnits.put("sqmeter", BigDecimal.valueOf(1000000));
        areaUnits.put("sqm", BigDecimal.valueOf(1000000));
        areaUnits.put("meters^2", BigDecimal.valueOf(1000000));
        areaUnits.put("meters2", BigDecimal.valueOf(1000000));
        areaUnits.put("meter^2", BigDecimal.valueOf(1000000));
        areaUnits.put("meter2", BigDecimal.valueOf(1000000));
        areaUnits.put("m^2", BigDecimal.valueOf(1000000));
        areaUnits.put("m2", BigDecimal.valueOf(1000000));
        areaUnits.put("squarecentimeters", BigDecimal.valueOf(100));
        areaUnits.put("squarecentimeter", BigDecimal.valueOf(100));
        areaUnits.put("sqcentimeters", BigDecimal.valueOf(100));
        areaUnits.put("sqcentimeter", BigDecimal.valueOf(100));
        areaUnits.put("centimeters^2", BigDecimal.valueOf(100));
        areaUnits.put("centimeters2", BigDecimal.valueOf(100));
        areaUnits.put("centimeter^2", BigDecimal.valueOf(100));
        areaUnits.put("centimeter2", BigDecimal.valueOf(100));
        areaUnits.put("cm^2", BigDecimal.valueOf(100));
        areaUnits.put("cm2", BigDecimal.valueOf(100));
        areaUnits.put("squaremillimeters", BigDecimal.valueOf(1));
        areaUnits.put("squaremillimeter", BigDecimal.valueOf(1));
        areaUnits.put("sqmillimeters", BigDecimal.valueOf(1));
        areaUnits.put("sqmillimeter", BigDecimal.valueOf(1));
        areaUnits.put("sqmm", BigDecimal.valueOf(1));
        areaUnits.put("millimeters^2", BigDecimal.valueOf(1));
        areaUnits.put("millimeters2", BigDecimal.valueOf(1));
        areaUnits.put("millimeter^2", BigDecimal.valueOf(1));
        areaUnits.put("millimeter2", BigDecimal.valueOf(1));
        areaUnits.put("mm^2", BigDecimal.valueOf(1));
        areaUnits.put("mm2", BigDecimal.valueOf(1));
    }

    //all mass units in terms of mg, conversion via google and https://www.convertunits.com
    static {
        massUnits = new TreeMap<>();
        massUnits.put("shorttons", BigDecimal.valueOf(907184740.0));
        massUnits.put("shortton", BigDecimal.valueOf(907184740.0));
        massUnits.put("tons", BigDecimal.valueOf(907184740.0));
        massUnits.put("ton", BigDecimal.valueOf(907184740.0));
        massUnits.put("metrictons", BigDecimal.valueOf(1000000000));
        massUnits.put("metricton", BigDecimal.valueOf(1000000000));
        massUnits.put("longtons", BigDecimal.valueOf(1016046908.8));
        massUnits.put("longton", BigDecimal.valueOf(1016046908.8));
        massUnits.put("pounds", BigDecimal.valueOf(453592.37));
        massUnits.put("pound", BigDecimal.valueOf(453592.37));
        massUnits.put("lbs", BigDecimal.valueOf(453592.37));
        massUnits.put("lb", BigDecimal.valueOf(453592.37));
        massUnits.put("ounces", BigDecimal.valueOf(28349.523125));
        massUnits.put("ounce", BigDecimal.valueOf(28349.523125));
        massUnits.put("oz", BigDecimal.valueOf(28349.523125));
        massUnits.put("carats", BigDecimal.valueOf(200));
        massUnits.put("carat", BigDecimal.valueOf(200));
        massUnits.put("caratsuk", BigDecimal.valueOf(259.19564));
        massUnits.put("caratuk", BigDecimal.valueOf(259.19564));
        massUnits.put("kilograms", BigDecimal.valueOf(1000000));
        massUnits.put("kilogram", BigDecimal.valueOf(1000000));
        massUnits.put("kg", BigDecimal.valueOf(1000000));
        massUnits.put("grams", BigDecimal.valueOf(1000));
        massUnits.put("gram", BigDecimal.valueOf(1000));
        massUnits.put("g", BigDecimal.valueOf(1000));
        massUnits.put("stones", BigDecimal.valueOf(6350293.18));
        massUnits.put("stone", BigDecimal.valueOf(6350293.18));
        massUnits.put("st", BigDecimal.valueOf(6350293.18));
    }

    //all temp units in terms of c, conversion via google and https://www.convertunits.com
    static {
        tempUnits = new TreeMap<>();
        tempUnits.put("fahrenheit", BigDecimal.valueOf(0.55555555555));
        tempUnits.put("f", BigDecimal.valueOf(0.55555555555));
        tempUnits.put("celsius", BigDecimal.valueOf(1));
        tempUnits.put("c", BigDecimal.valueOf(1));
        tempUnits.put("kelvin", BigDecimal.valueOf(1));
        tempUnits.put("k", BigDecimal.valueOf(1));
    }

    //all time units in terms of ms, conversion via google and https://www.unitjuggler.com
    static {
        timeUnits = new TreeMap<>();
        timeUnits.put("years", BigDecimal.valueOf(31556952000.0));
        timeUnits.put("year", BigDecimal.valueOf(31556952000.0));
        timeUnits.put("yr", BigDecimal.valueOf(31556952000.0));
        timeUnits.put("y", BigDecimal.valueOf(31556952000.0));
        timeUnits.put("months", BigDecimal.valueOf(2629800000.0));
        timeUnits.put("month", BigDecimal.valueOf(2629800000.0));
        timeUnits.put("mon", BigDecimal.valueOf(2629800000.0));
        timeUnits.put("days", BigDecimal.valueOf(86400000.0));
        timeUnits.put("day", BigDecimal.valueOf(86400000.0));
        timeUnits.put("d", BigDecimal.valueOf(86400000.0));
        timeUnits.put("hours", BigDecimal.valueOf(3600000.0));
        timeUnits.put("hour", BigDecimal.valueOf(3600000.0));
        timeUnits.put("hr", BigDecimal.valueOf(3600000.0));
        timeUnits.put("h", BigDecimal.valueOf(3600000.0));
        timeUnits.put("minutes", BigDecimal.valueOf(60000));
        timeUnits.put("minute", BigDecimal.valueOf(60000));
        timeUnits.put("min", BigDecimal.valueOf(60000));
        timeUnits.put("mn", BigDecimal.valueOf(60000));
        timeUnits.put("m", BigDecimal.valueOf(60000));
        timeUnits.put("seconds", BigDecimal.valueOf(1000));
        timeUnits.put("second", BigDecimal.valueOf(1000));
        timeUnits.put("sec", BigDecimal.valueOf(1000));
        timeUnits.put("sc", BigDecimal.valueOf(1000));
        timeUnits.put("s", BigDecimal.valueOf(1000));
        timeUnits.put("milliseconds", BigDecimal.valueOf(1));
        timeUnits.put("millisec", BigDecimal.valueOf(1));
        timeUnits.put("msecond", BigDecimal.valueOf(1));
        timeUnits.put("msec", BigDecimal.valueOf(1));
        timeUnits.put("ms", BigDecimal.valueOf(1));
    }

    static {
        units = new HashMap<>();
        units.put("mile", distUnits);
        units.put("volume", volUnits);
        units.put("speed", spdUnits);
        units.put("area", areaUnits);
        units.put("mass", massUnits);
        units.put("temp", tempUnits);
        units.put("time", timeUnits);
    }

    public static BigDecimal getUnit(String unitType, String unitName) {
        if (units != null) {
            TreeMap<String, BigDecimal> u = units.get(unitType);
            if (u != null) {
                BigDecimal mu = u.get(unitName);
                if (mu != null) {
                    return mu;
                }
            }
        }
        return BigDecimal.valueOf(0);
    }

    public static String getRegExUnitsString(String unitType){
        if (units != null) {
            TreeMap<String, BigDecimal> u = units.get(unitType);
            if (u != null) {
                StringBuilder out = new StringBuilder();
                out.append("(");

                Set<Map.Entry<String, BigDecimal>> unitEntries = u.entrySet();
                for (Map.Entry<String, BigDecimal> each : unitEntries) {
                    out.append(regExify(each.getKey()));
                    out.append("|");
                }
                out.deleteCharAt(out.length() - 1);
                out.append(")");

                return out.toString();
            }
        }
        return "()";
    }

    public static String regExify(String s){
        return s.replaceAll("\\^", "\\\\\\^");
    }

    public static Set<String> getTypes(){
        return units.keySet();
    }
}