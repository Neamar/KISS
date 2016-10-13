package fr.neamar.kiss.normalizer;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import fr.neamar.kiss.R;

public class PhoneNormalizer {
    static PhoneNormalizer instance;
    private final PrefixEntry prefixEntry;
    private final String countryIso;

    private PhoneNormalizer(Context context) {
        countryIso = determineCountryIso(context);
        prefixEntry = loadPrefixEntry(context, countryIso);
    }

    public static void initialize(Context context) {
        if (instance == null)
            instance = new PhoneNormalizer(context);
    }

    public static String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) return "";

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String e164 = PhoneNumberUtils.formatNumberToE164(phoneNumber, instance.countryIso);
            if (e164 != null) {
                return PhoneNumberUtils.formatNumber(e164, instance.countryIso);
            }
        }

        //noinspection deprecation
        return PhoneNumberUtils.formatNumber(instance.toE164(phoneNumber));
    }

    private String toE164(String input) {
        if (input.startsWith("+")) return input;

        if (prefixEntry != null) {
            if (prefixEntry.international_prefix_pattern.length() > 0 &&
                    input.matches(prefixEntry.international_prefix_pattern)) {
                return "+" + input.replaceFirst(prefixEntry.international_prefix_pattern, "");
            }
            if (prefixEntry.national_prefix.length() > 0 && input.startsWith(prefixEntry.national_prefix)) {
                return "+" + prefixEntry.ptsn_country_code + input.substring(prefixEntry.national_prefix.length());
            }
        }

        // null? keep input? don't know what to do with input..
        return input;
    }

    private PrefixEntry loadPrefixEntry(Context context, String countryIso) {
        InputStream inputStream = context.getResources().openRawResource(R.raw.phone_number_prefixes);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length != 4) {
                    Log.w("PREFIX", "invalid csv record! " + line);
                    continue;
                }
                if (parts[0].equals(countryIso))
                    return new PrefixEntry(parts[0], parts[1], parts[2], parts[3]);
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        return null;
    }

    private String determineCountryIso(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null) {
            try {
                String simCountryIso = manager.getSimCountryIso();
                //Log.d("COUNTRY", "SIM: " + simCountryIso);
                if (!TextUtils.isEmpty(simCountryIso))
                    return simCountryIso;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                String networkCountryIso = manager.getNetworkCountryIso();
                //Log.d("COUNTRY", "NET: " + networkCountryIso);
                if (!TextUtils.isEmpty(networkCountryIso))
                    return networkCountryIso;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        //Log.d("COUNTRY", "LOCALE: " + Locale.getDefault().getCountry());
        return Locale.getDefault().getCountry();
    }

    private class PrefixEntry {
        public String iso_country_code;
        public String ptsn_country_code;
        public String international_prefix_pattern;
        public String national_prefix;

        public PrefixEntry(String iso_country_code, String ptsn_country_code,
                           String international_prefix_pattern, String national_prefix) {
            this.iso_country_code = iso_country_code;
            this.ptsn_country_code = ptsn_country_code;
            this.international_prefix_pattern = international_prefix_pattern;
            this.national_prefix = national_prefix;
        }
    }
}
