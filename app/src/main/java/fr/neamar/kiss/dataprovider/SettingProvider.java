package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import fr.neamar.kiss.pojo.Holder;
import fr.neamar.kiss.pojo.SettingHolder;
import fr.neamar.kiss.task.LoadSettingHolders;

public class SettingProvider extends Provider<SettingHolder> {

	public SettingProvider(Context context) {
		super(new LoadSettingHolders(context));
	}

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> results = new ArrayList<Holder>();

		int relevance;
		String settingNameLowerCased;
		for (int i = 0; i < holders.size(); i++) {
			SettingHolder setting = holders.get(i);
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
				results.add(setting);
			}
		}

		return results;
	}

	public Holder findById(String id) {
		for (int i = 0; i < holders.size(); i++) {
			if (holders.get(i).id.equals(id)) {
				holders.get(i).displayName = holders.get(i).name.replace("Setting:",
						"<small><small>Setting:</small></small>");
				return holders.get(i);
			}

		}

		return null;
	}
}
