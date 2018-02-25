package fr.neamar.kiss.dataprovider;

import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadPhonePojos;
import fr.neamar.kiss.pojo.PhonePojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.searcher.Searcher;

public class PhoneProvider extends Provider<PhonePojo> {
    public static final String PHONE_SCHEME = "phone://";
    private boolean deviceIsPhone = false;

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadPhonePojos(this));

        PackageManager pm = this.getPackageManager();
        deviceIsPhone = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    @Override
    public void requestResults(String s, Searcher searcher) {
        searcher.addResult(getResults(s).toArray(new Pojo[0]));
    }

    private ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> pojos = new ArrayList<>();

        // Append an item only if query looks like a phone number and device has phone capabilities
        if (deviceIsPhone && query.matches("^([0-9+ .-]{2,}|[*#]{1,3}[0-9]{1,3}[*a-zA-Z0-9]*#)$")) {
            pojos.add(getResult(query));
        }

        return pojos;
    }

    public Pojo findById(String id) {
        return getResult(id.replaceFirst(Pattern.quote(PHONE_SCHEME), ""));
    }

    private Pojo getResult(String phoneNumber) {
        PhonePojo pojo = new PhonePojo();
        pojo.id = PHONE_SCHEME + phoneNumber;
        pojo.phone = phoneNumber;
        pojo.relevance = 20;
        pojo.setName(phoneNumber, false);
        return pojo;
    }
}
