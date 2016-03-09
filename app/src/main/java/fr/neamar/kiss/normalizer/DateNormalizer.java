package fr.neamar.kiss.normalizer;

import android.text.format.DateFormat;

public class DateNormalizer {
    public static String normalizeDate(Long lastTimeContacted) {
        if(lastTimeContacted == null) {
            return "";
        }

        return DateFormat.format("k:m MM/dd/yy", lastTimeContacted).toString();
    }
}