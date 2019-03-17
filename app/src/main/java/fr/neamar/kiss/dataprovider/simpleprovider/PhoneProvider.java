package fr.neamar.kiss.dataprovider.simpleprovider;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.regex.Pattern;

import fr.neamar.kiss.pojo.PhonePojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.searcher.Searcher;

public class PhoneProvider extends SimpleProvider {
    private static final String PHONE_SCHEME = "phone://";
    private boolean deviceIsPhone;
    private Pattern phonePattern = Pattern.compile("^[*+0-9# ]{3,}$");

    public PhoneProvider(Context context) {
        PackageManager pm = context.getPackageManager();
        deviceIsPhone = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        // Append an item only if query looks like a phone number and device has phone capabilities
        if (deviceIsPhone && phonePattern.matcher(query).find()) {
            searcher.addResult(getResult(query));
        }
    }

    @Override
    public boolean mayFindById(String id) {
        return id.startsWith(PHONE_SCHEME);
    }

    public Pojo findById(String id) {
        return getResult(id.replaceFirst(Pattern.quote(PHONE_SCHEME), ""));
    }

    private Pojo getResult(String phoneNumber) {
        PhonePojo pojo = new PhonePojo(PHONE_SCHEME + phoneNumber, phoneNumber);
        pojo.relevance = 20;
        pojo.setName(phoneNumber, false);
        return pojo;
    }
}
