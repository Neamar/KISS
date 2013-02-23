package fr.neamar.summon.lite.task;

import java.util.ArrayList;

import fr.neamar.summon.lite.R;
import fr.neamar.summon.lite.holder.ToggleHolder;

import android.content.Context;
import android.content.pm.PackageManager;

public class LoadToogleHolders extends LoadHolders<ToggleHolder>{
	
	public LoadToogleHolders(Context context) {
		super(context, "app://");
	}

	@Override
	protected ArrayList<ToggleHolder> doInBackground(Void... params) {
		ArrayList<ToggleHolder> toggles = new ArrayList<ToggleHolder>();
		PackageManager pm = context.getPackageManager();
		if(pm.hasSystemFeature(PackageManager.FEATURE_WIFI)){
			toggles.add(createHolder("Wifi", "wifi", R.attr.wifi));
		}
		if(pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
			toggles.add(createHolder("Bluetooth", "bluetooth",
					R.attr.bluetooth));
		}
		toggles.add(createHolder("Silent", "silent",
				R.attr.silent));
		// toggles.add(createHolder("GPS", "gps", R.drawable.toggle_gps));
		// toggles.add(createHolder("Mobile network data", "data",
		// R.drawable.toggle_data));
		return toggles;
	}	
	
	private ToggleHolder createHolder(String name, String settingName,
			int resId) {
		ToggleHolder holder = new ToggleHolder();
		holder.id = holderScheme + name.toLowerCase();
		holder.name = "Toggle: " + name;
		holder.nameLowerCased = holder.name.toLowerCase();
		holder.settingName = settingName;
		holder.icon = resId;

		return holder;
	}
}
