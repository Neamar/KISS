package fr.neamar.summon.lite.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import fr.neamar.summon.lite.R;
import fr.neamar.summon.lite.holder.Holder;
import fr.neamar.summon.lite.holder.ToggleHolder;
import android.content.pm.PackageManager;

public class ToggleProvider extends Provider {
	private ArrayList<ToggleHolder> toggles = new ArrayList<ToggleHolder>();

	public ToggleProvider(final Context context) {
		super();
		holderScheme = "toggle://";
		Thread thread = new Thread(null, new Runnable() {
			public void run() {
				PackageManager pm = context.getPackageManager();
				if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
					toggles.add(createHolder("Wifi", "wifi",
							R.drawable.toggle_wifi));
				}
				if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
					toggles.add(createHolder("Bluetooth", "bluetooth",
							R.drawable.toggle_bluetooth));
				}
				toggles.add(createHolder("Silent", "silent",
						R.drawable.toggle_silent));
				// toggles.add(createHolder("GPS", "gps",
				// R.drawable.toggle_gps));
				// toggles.add(createHolder("Mobile network data", "data",
				// R.drawable.toggle_data));
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
		});
		thread.setPriority(Thread.NORM_PRIORITY + 1);
		thread.start();
	}

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> holders = new ArrayList<Holder>();

		int relevance;
		String toggleNameLowerCased;
		for (int i = 0; i < toggles.size(); i++) {
			ToggleHolder toggle = toggles.get(i);

			relevance = 0;
			toggleNameLowerCased = toggle.nameLowerCased;
			if (toggleNameLowerCased.startsWith(query))
				relevance = 75;
			else if (toggleNameLowerCased.contains(" " + query))
				relevance = 30;
			else if (toggleNameLowerCased.contains(query))
				relevance = 1;

			if (relevance > 0) {
				toggle.displayName = toggle.name.replace("Toggle:",
						"<small><small>Toggle:</small></small>").replaceFirst(
						"(?i)(" + Pattern.quote(query) + ")", "{$1}");
				toggle.relevance = relevance;
				holders.add(toggle);
			}
		}

		return holders;
	}

	public Holder findById(String id) {
		for (int i = 0; i < toggles.size(); i++) {
			if (toggles.get(i).id.equals(id)) {
				toggles.get(i).displayName = toggles.get(i).name.replace(
						"Toggle:", "<small><small>Toggle:</small></small>");
				return toggles.get(i);
			}

		}

		return null;
	}
}
