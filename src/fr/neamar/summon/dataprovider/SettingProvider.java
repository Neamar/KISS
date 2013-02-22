package fr.neamar.summon.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import fr.neamar.summon.R;
import fr.neamar.summon.SummonActivity;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.holder.SettingHolder;

public class SettingProvider extends Provider {
	private ArrayList<SettingHolder> settings = new ArrayList<SettingHolder>();

	public SettingProvider(final Context context) {
		super();
		holderScheme = "setting://";
		Thread thread = new Thread(null, new Runnable() {
			public void run() {
				settings.add(createHolder("Airplane mode",
						android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS,
						R.attr.airplane));
				settings.add(createHolder("Device info",
						android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS));
				settings.add(createHolder(
						"Applications",
						android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
				settings.add(createHolder("Connectivity",
						android.provider.Settings.ACTION_WIRELESS_SETTINGS,
						R.attr.wifi));
				Intent i = new Intent(SummonActivity.LOAD_OVER);
		        context.sendBroadcast(i);
			}

			private SettingHolder createHolder(String name, String settingName) {
				return createHolder(name, settingName, R.drawable.settings);
			}

			private SettingHolder createHolder(String name, String settingName,
					int resId) {
				SettingHolder holder = new SettingHolder();
				holder.id = holderScheme + settingName.toLowerCase();
				holder.name = "Setting: " + name;
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
		String settingNameLowerCased;
		for (int i = 0; i < settings.size(); i++) {
			SettingHolder setting = settings.get(i);
			relevance = 0;
			settingNameLowerCased = setting.nameLowerCased;
			if (settingNameLowerCased.startsWith(query))
				relevance = 10;
			else if (settingNameLowerCased.contains(" " + query))
				relevance = 5;

			if (relevance > 0) {
				setting.displayName = setting.name.replace("Setting:",
						"<small><small>Setting:</small></small>").replaceFirst(
						"(?i)(" + Pattern.quote(query) + ")", "{$1}");
				setting.relevance = relevance;
				holders.add(setting);
			}
		}

		return holders;
	}

	public Holder findById(String id) {
		for (int i = 0; i < settings.size(); i++) {
			if (settings.get(i).id.equals(id)) {
				settings.get(i).displayName = settings.get(i).name.replace(
						"Setting:", "<small><small>Setting:</small></small>");
				return settings.get(i);
			}

		}

		return null;
	}
}
