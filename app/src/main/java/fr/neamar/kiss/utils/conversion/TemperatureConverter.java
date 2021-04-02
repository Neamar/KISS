package fr.neamar.kiss.utils.conversion;

import java.math.BigDecimal;
import java.math.MathContext;

public class TemperatureConverter {
    public static BigDecimal convertTemp(String fromUnit, BigDecimal fromTemp, String toUnit){
        if (fromUnit != null && fromTemp != null && toUnit != null) {
            if (fromUnit.startsWith("f")) {
                if (toUnit.startsWith("c")) {
                    return fromTemp.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5.0 / 9), MathContext.DECIMAL32);
                } else if (toUnit.startsWith("k")) {
                    return fromTemp.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5.0 / 9), MathContext.DECIMAL32).add(BigDecimal.valueOf(255.372));
                } else {
                    return fromTemp;
                }
            } else if (fromUnit.startsWith("c")) {
                if (toUnit.startsWith("f")) {
                    return fromTemp.multiply(BigDecimal.valueOf(9.0 / 5)).add(BigDecimal.valueOf(32), MathContext.DECIMAL32);
                } else if (toUnit.startsWith("k")) {
                    return fromTemp.add(BigDecimal.valueOf(273.15));
                } else {
                    return fromTemp;
                }
            } else if (fromUnit.startsWith("k")) {
                if (toUnit.startsWith("f")) {
                    return fromTemp.subtract(BigDecimal.valueOf(273.15)).multiply(BigDecimal.valueOf(9.0 / 5)).add(BigDecimal.valueOf(32), MathContext.DECIMAL32);
                } else if (toUnit.startsWith("c")) {
                    return fromTemp.subtract(BigDecimal.valueOf(273.15));
                } else {
                    return fromTemp;
                }
            }
        }
        return fromTemp;
    }
}

