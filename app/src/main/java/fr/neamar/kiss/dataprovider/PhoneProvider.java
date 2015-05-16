package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import android.content.pm.PackageManager;
import android.content.Context;
import fr.neamar.kiss.holder.Holder;
import fr.neamar.kiss.holder.PhoneHolder;
import fr.neamar.kiss.task.LoadPhoneHolders;

public class PhoneProvider extends Provider<PhoneHolder> {
	public boolean deviceIsPhoneEnabled = false;

	public PhoneProvider(Context context) {
		super(new LoadPhoneHolders(context));

		PackageManager pm = context.getPackageManager();
		deviceIsPhoneEnabled = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
	}

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> holders = new ArrayList<Holder>();

		// Append an item only if query looks like a phone number and device has phone capabilities
		if(deviceIsPhoneEnabled && query.matches("^[0-9+ .]{2,}$")) {
			PhoneHolder holder = new PhoneHolder();
			holder.phone = query;
			holder.relevance = 20;
			holders.add(holder);
		}

		return holders;
	}
}
