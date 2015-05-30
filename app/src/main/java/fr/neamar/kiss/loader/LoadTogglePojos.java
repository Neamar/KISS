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
			toggles.add(createHolder("Wifi", "wifi", R.attr.wifi));
		}
		if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
			toggles.add(createHolder("Bluetooth", "bluetooth", R.attr.bluetooth));
		}
		toggles.add(createHolder("Silent", "silent", R.attr.silent));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
				&& pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
			toggles.add(createHolder("Mobile network data", "data", R.attr.data));
		}
		return toggles;
	}

	private TogglePojo createHolder(String name, String settingName, int resId) {
		TogglePojo holder = new TogglePojo();
		holder.id = holderScheme + name.toLowerCase();
		holder.name = "Toggle: " + name;
		holder.nameLowerCased = holder.name.toLowerCase();
		holder.settingName = settingName;
		holder.icon = resId;

		return holder;
	}
}
