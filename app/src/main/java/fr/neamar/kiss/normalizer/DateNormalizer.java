package fr.neamar.kiss.normalizer;

import android.text.format.DateFormat;

public class DateNormalizer {
    public static String normalizeDate(Long lastTimeContacted) {
        if(lastTimeContacted == null || lastTimeContacted == 0) {
            return "";
        }

        return DateFormat.format("HH:mm MM/dd/yy", lastTimeContacted).toString();
    }
}