package fr.neamar.summon.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.holder.SettingHolder;
import fr.neamar.summon.task.LoadSettingHolders;
import fr.neamar.summon.task.LoadSettingHoldersFromDB;

public class SettingProvider extends Provider<SettingHolder> {

	public SettingProvider(Context context) {
		super(new LoadSettingHolders(context));
	}

    private SettingProvider(LoadSettingHoldersFromDB loader) {
        super(loader);
    }

    public static SettingProvider fromDB(Context context){
        return new SettingProvider(new LoadSettingHoldersFromDB(context));
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

    @Override
    public void saveProvider(Context context) {
        //Nothing to save here
    }
}
