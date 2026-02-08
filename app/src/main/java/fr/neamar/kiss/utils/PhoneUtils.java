package fr.neamar.kiss.utils;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import fr.neamar.kiss.normalizer.StringNormalizer;

public class PhoneUtils {

    // See https://github.com/Neamar/KISS/issues/1137
    private static final Pattern KISS_PHONE_PATTERN = Pattern.compile("^[*+0-9# ]{3,}$");

    private final String countryIso;

    public PhoneUtils(Context context) {
        this.countryIso = getDefaultCountryIso(context);
    }

    public String format(String phoneNumber) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !TextUtils.isEmpty(countryIso)) {
            String formattedPhone = PhoneNumberUtils.formatNumber(phoneNumber, countryIso);
            if (!TextUtils.isEmpty(formattedPhone)) {
                return formattedPhone;
            }
        }
        return phoneNumber;
    }

    public boolean areSamePhoneNumber(@NonNull StringNormalizer.Result number1, @NonNull StringNormalizer.Result number2) {
        if (number1.equals(number2)) {
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (TextUtils.isEmpty(countryIso)) {
                return false;
            } else {
                return PhoneNumberUtils.areSamePhoneNumber(number1.toString(), number2.toString(), countryIso);
            }
        } else {
            return PhoneNumberUtils.compare(number1.toString(), number2.toString());
        }
    }

    @NonNull
    private String getDefaultCountryIso(Context context) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return manager.getNetworkCountryIso();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isPhoneNumber(String phoneNumber) {
        return KISS_PHONE_PATTERN.matcher(phoneNumber).matches() || Patterns.PHONE.matcher(phoneNumber).matches();
    }

    public static String convertKeypadLettersToDigits(String phoneNumber) {
        return PhoneNumberUtils.convertKeypadLettersToDigits(phoneNumber);
    }
}
