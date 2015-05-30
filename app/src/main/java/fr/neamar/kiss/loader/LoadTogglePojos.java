package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.TogglePojo;

public class LoadTogglePojos extends LoadPojos<TogglePojo> {

	public LoadTogglePojos(Context context) {
		super(context, "app://");
	}

	@Override
	protected ArrayList<TogglePojo> doInBackground(Void... params) {
		ArrayList<TogglePojo> toggles = new ArrayList<TogglePojo>();
		PackageManager pm = context.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
			toggles.add(createPojo("Wifi", "wifi", R.attr.wifi));
		}
		if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
			toggles.add(createPojo("Bluetooth", "bluetooth", R.attr.bluetooth));
		}
		toggles.add(createPojo("Silent", "silent", R.attr.silent));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
				&& pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
			toggles.add(createPojo("Mobile network data", "data", R.attr.data));
		}
		return toggles;
	}

	private TogglePojo createPojo(String name, String settingName, int resId) {
		TogglePojo pojo = new TogglePojo();
		pojo.id = pojoScheme + name.toLowerCase();
		pojo.name = "Toggle: " + name;
		pojo.nameLowerCased = pojo.name.toLowerCase();
		pojo.settingName = settingName;
		pojo.icon = resId;

		return pojo;
	}
}
