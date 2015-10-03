package fr.neamar.kiss.normalizer;

import android.os.Build;
import android.telephony.PhoneNumberUtils;

import java.util.Locale;

public class PhoneNormalizer {
    public static String normalizePhone(String phoneNumber) {
        if(phoneNumber == null) {
            return "";
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().getCountry());
        } else {
            return PhoneNumberUtils.formatNumber(phoneNumber);
        }
    }
}
