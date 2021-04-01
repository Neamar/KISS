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
    private static HashMap<String, TreeMap<String, BigDecimal>> units;

    static {
        //all distance units in terms of mm, conversion via google
        distUnits = new TreeMap<>();
        distUnits.put("miles", BigDecimal.valueOf(1609344));
        distUnits.put("mile", BigDecimal.valueOf(1609344));
        distUnits.put("mi", BigDecimal.valueOf(1609344));
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

        //all volume units in terms of ml, conversion via google
        volUnits = new TreeMap<>();
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

        //all speed units in terms of mm/s, conversion via google
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

        //all area units in terms of mm^2, conversion via google
        areaUnits = new TreeMap<>();
        areaUnits.put("squaremiles", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("squaremile", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("sqmiles", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("sqmile", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("sqmi", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("mile^2", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("mi^2", BigDecimal.valueOf(2589988110000.0));
        areaUnits.put("squarefeet", BigDecimal.valueOf(92903));
        areaUnits.put("squarefoot", BigDecimal.valueOf(92903));
        areaUnits.put("sqfeet", BigDecimal.valueOf(92903));
        areaUnits.put("sqfooft", BigDecimal.valueOf(92903));
        areaUnits.put("sqft", BigDecimal.valueOf(92903));
        areaUnits.put("feet^2", BigDecimal.valueOf(92903));
        areaUnits.put("foot^2", BigDecimal.valueOf(92903));
        areaUnits.put("ft^2", BigDecimal.valueOf(92903));
        areaUnits.put("squareinches", BigDecimal.valueOf(645.16));
        areaUnits.put("squareinch", BigDecimal.valueOf(645.16));
        areaUnits.put("sqinches", BigDecimal.valueOf(645.16));
        areaUnits.put("sqinch", BigDecimal.valueOf(645.16));
        areaUnits.put("sqin", BigDecimal.valueOf(645.16));
        areaUnits.put("inches^2", BigDecimal.valueOf(645.16));
        areaUnits.put("inch^2", BigDecimal.valueOf(645.16));
        areaUnits.put("in^2", BigDecimal.valueOf(645.16));
        areaUnits.put("squarekilometers", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("squarekilometer", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("sqkilometers", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("sqkilometer", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("sqkm", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("kilometers^2", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("kilometer^2", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("km^2", BigDecimal.valueOf(1000000000000.0));
        areaUnits.put("squaremeters", BigDecimal.valueOf(1000000));
        areaUnits.put("squaremeter", BigDecimal.valueOf(1000000));
        areaUnits.put("sqmeters", BigDecimal.valueOf(1000000));
        areaUnits.put("sqmeter", BigDecimal.valueOf(1000000));
        areaUnits.put("sqm", BigDecimal.valueOf(1000000));
        areaUnits.put("meters^2", BigDecimal.valueOf(1000000));
        areaUnits.put("meter^2", BigDecimal.valueOf(1000000));
        areaUnits.put("m^2", BigDecimal.valueOf(1000000));
        areaUnits.put("squarecentimeters", BigDecimal.valueOf(100));
        areaUnits.put("squarecentimeter", BigDecimal.valueOf(100));
        areaUnits.put("sqcentimeters", BigDecimal.valueOf(100));
        areaUnits.put("sqcentimeter", BigDecimal.valueOf(100));
        areaUnits.put("centimeters^2", BigDecimal.valueOf(100));
        areaUnits.put("centimeter^2", BigDecimal.valueOf(100));
        areaUnits.put("cm^2", BigDecimal.valueOf(100));
        areaUnits.put("squaremillimeters", BigDecimal.valueOf(1));
        areaUnits.put("squaremillimeter", BigDecimal.valueOf(1));
        areaUnits.put("sqmillimeters", BigDecimal.valueOf(1));
        areaUnits.put("sqmillimeter", BigDecimal.valueOf(1));
        areaUnits.put("sqmm", BigDecimal.valueOf(1));
        areaUnits.put("millimeters^2", BigDecimal.valueOf(1));
        areaUnits.put("millimeter^2", BigDecimal.valueOf(1));
        areaUnits.put("mm^2", BigDecimal.valueOf(1));

        units = new HashMap<>();
        units.put("mile", distUnits);
        units.put("volume", volUnits);
        units.put("speed", spdUnits);
        units.put("area", areaUnits);
    }

    public static BigDecimal getUnit(String unitType, String unitName){
        return units.get(unitType).get(unitName);
    }

    public static String getRegExUnitsString(String unitType){
        String out = "(";

        for (Map.Entry<String, BigDecimal> each : units.get(unitType).entrySet()){
            out += regExify(each.getKey()) + "|";
        }
        out = out.substring(0, out.length()-1);
        out += ")";

        return out;
    }

    public static String regExify(String s){
        String out = s.replaceAll("\\^", "\\\\\\^");
        return out;
    }

    public static Set<String> getTypes(){
        return units.keySet();
    }
}