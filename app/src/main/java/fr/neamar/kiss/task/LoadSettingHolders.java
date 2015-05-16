package fr.neamar.kiss.task;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Locale;

import fr.neamar.kiss.R;
import fr.neamar.kiss.holder.SettingHolder;

public class LoadSettingHolders extends LoadHolders<SettingHolder> {

	public LoadSettingHolders(Context context) {
		super(context, "setting://");
	}

	@Override
	protected ArrayList<SettingHolder> doInBackground(Void... params) {
		ArrayList<SettingHolder> settings = new ArrayList<SettingHolder>();
		settings.add(createHolder("Airplane mode",
				android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS, R.attr.airplane));
		settings.add(createHolder("Device info",
				android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS));
		settings.add(createHolder("Applications",
				android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, R.attr.application));
		settings.add(createHolder("Connectivity",
				android.provider.Settings.ACTION_WIRELESS_SETTINGS, R.attr.wifi));
		settings.add(createHolder("Battery", Intent.ACTION_POWER_USAGE_SUMMARY, R.attr.battery));
		return settings;
	}

	private SettingHolder createHolder(String name, String settingName) {
		return createHolder(name, settingName, android.R.drawable.ic_menu_manage);
	}

	private SettingHolder createHolder(String name, String settingName, int resId) {
		SettingHolder holder = new SettingHolder();
		holder.id = holderScheme + settingName.toLowerCase(Locale.ENGLISH);
		holder.name = "Setting: " + name;
		holder.nameLowerCased = holder.name.toLowerCase(Locale.ENGLISH);
		holder.settingName = settingName;
		holder.icon = resId;

		return holder;
	}
}
