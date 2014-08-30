package fr.neamar.summon.task;

import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import fr.neamar.summon.R;
import fr.neamar.summon.holder.ToggleHolder;

public class LoadToogleHolders extends LoadHolders<ToggleHolder> {

	public LoadToogleHolders(Context context) {
		super(context, "app://");
	}

	@Override
	protected ArrayList<ToggleHolder> doInBackground(Void... params) {
		ArrayList<ToggleHolder> toggles = new ArrayList<ToggleHolder>();
		PackageManager pm = context.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
			toggles.add(createHolder("Wifi", "wifi", R.attr.wifi));
		}
		if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
			toggles.add(createHolder("Bluetooth", "bluetooth", R.attr.bluetooth));
		}
		toggles.add(createHolder("Silent", "silent", R.attr.silent));
		// toggles.add(createHolder("GPS", "gps", R.drawable.toggle_gps));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
				&& pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
			toggles.add(createHolder("Mobile network data", "data", R.attr.data));
		}
		return toggles;
	}

	private ToggleHolder createHolder(String name, String settingName, int resId) {
		ToggleHolder holder = new ToggleHolder();
		holder.id = holderScheme + name.toLowerCase();
		holder.name = "Toggle: " + name;
		holder.nameLowerCased = holder.name.toLowerCase();
		holder.settingName = settingName;
		holder.icon = resId;

		return holder;
	}
}
