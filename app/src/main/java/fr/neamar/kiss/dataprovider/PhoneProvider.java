package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import android.content.pm.PackageManager;
import android.content.Context;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PhonePojo;
import fr.neamar.kiss.task.LoadPhoneHolders;

public class PhoneProvider extends Provider<PhonePojo> {
	public boolean deviceIsPhoneEnabled = false;

	public PhoneProvider(Context context) {
		super(new LoadPhoneHolders(context));

		PackageManager pm = context.getPackageManager();
		deviceIsPhoneEnabled = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
	}

	public ArrayList<Pojo> getResults(String query) {
		ArrayList<Pojo> pojos = new ArrayList<Pojo>();

		// Append an item only if query looks like a phone number and device has phone capabilities
		if(deviceIsPhoneEnabled && query.matches("^[0-9+ .]{2,}$")) {
			PhonePojo holder = new PhonePojo();
			holder.phone = query;
			holder.relevance = 20;
			pojos.add(holder);
		}

		return pojos;
	}
}
