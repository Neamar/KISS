package fr.neamar.kiss.dataprovider;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadPhonePojos;
import fr.neamar.kiss.pojo.PhonePojo;
import fr.neamar.kiss.pojo.Pojo;

public class PhoneProvider extends Provider<PhonePojo> {
    private boolean deviceIsPhoneEnabled = false;

    public PhoneProvider(Context context) {
        super(new LoadPhonePojos(context));

        PackageManager pm = context.getPackageManager();
        deviceIsPhoneEnabled = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> pojos = new ArrayList<>();

        // Append an item only if query looks like a phone number and device has phone capabilities
        if (deviceIsPhoneEnabled && query.matches("^[0-9+ .]{2,}$")) {
            PhonePojo pojo = new PhonePojo();
            pojo.phone = query;
            pojo.relevance = 20;
            pojos.add(pojo);
        }

        return pojos;
    }
}
