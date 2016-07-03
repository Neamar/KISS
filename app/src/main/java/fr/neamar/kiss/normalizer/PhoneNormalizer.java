package fr.neamar.kiss.normalizer;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import fr.neamar.kiss.R;

public class PhoneNormalizer {
    static final PhoneNormalizer instance = new PhoneNormalizer();
    private Map<String, PrefixEntry> prefix_data;

    private PhoneNormalizer() {
    }

    public static void initialize(Context context) {
        instance.loadPrefixData(context);
    }

    public static String normalizePhone(String phoneNumber, String defaultcountryIso) {
        if (phoneNumber == null) return "";

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return PhoneNumberUtils.formatNumberToE164(phoneNumber, defaultcountryIso);
        } else {
            //noinspection deprecation
            return PhoneNumberUtils.formatNumber(instance.toE164(phoneNumber, defaultcountryIso));
        }
    }

    private String toE164(String input, String defaultCountryIso) {
        if (input.startsWith("+"))
            return input;

        if (prefix_data != null && prefix_data.containsKey(defaultCountryIso)) {
            PrefixEntry entry = prefix_data.get(defaultCountryIso);
            if (entry.international_prefix.length() > 0 && input.startsWith(entry.international_prefix)) {
                return "+" + input.substring(entry.international_prefix.length());
            }
            if (entry.national_prefix.length() > 0 && input.startsWith(entry.national_prefix)) {
                return "+" + entry.ptsn_country_code + input.substring(entry.national_prefix.length());
            }
        }

        // null? keep input? don't know what to do with input..
        return input;
    }

    private void loadPrefixData(Context context) {
        if (prefix_data != null)
            return;

        InputStream inputStream = context.getResources().openRawResource(R.raw.phone_number_prefixes);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        Map<String, PrefixEntry> data = new HashMap<>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length == 4) {
                    data.put(parts[0],
                            new PrefixEntry(parts[0], parts[1], parts[2], parts[3]));
                } else {
                    Log.w("PREFIX", "invalid csv record! " + line);
                }
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
            data = null;
        }
        prefix_data = data;
    }

    private class PrefixEntry {
        public String iso_country_code;
        public String ptsn_country_code;
        public String international_prefix;
        public String national_prefix;

        public PrefixEntry(String iso_country_code, String ptsn_country_code,
                           String international_prefix, String national_prefix) {
            this.iso_country_code = iso_country_code;
            this.ptsn_country_code = ptsn_country_code;
            this.international_prefix = international_prefix;
            this.national_prefix = national_prefix;
        }
    }
}
