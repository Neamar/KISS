package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SettingPojo;
import fr.neamar.kiss.loader.LoadSettingPojos;

public class SettingProvider extends Provider<SettingPojo> {

	public SettingProvider(Context context) {
		super(new LoadSettingPojos(context));
	}

	public ArrayList<Pojo> getResults(String query) {
		ArrayList<Pojo> results = new ArrayList<Pojo>();

		int relevance;
		String settingNameLowerCased;
		for (int i = 0; i < pojos.size(); i++) {
			SettingPojo setting = pojos.get(i);
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

	public Pojo findById(String id) {
		for (int i = 0; i < pojos.size(); i++) {
			if (pojos.get(i).id.equals(id)) {
				pojos.get(i).displayName = pojos.get(i).name.replace("Setting:",
						"<small><small>Setting:</small></small>");
				return pojos.get(i);
			}

		}

		return null;
	}
}
