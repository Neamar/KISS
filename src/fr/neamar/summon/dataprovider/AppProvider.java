package fr.neamar.summon.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.summon.db.DBHelper;
import fr.neamar.summon.holder.AppHolder;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.task.LoadAppHolders;
import fr.neamar.summon.task.LoadAppHoldersFromDB;

public class AppProvider extends Provider<AppHolder> {

    private AppProvider(LoadAppHoldersFromDB loader){
        super(loader);
    }

	public AppProvider(Context context) {
		super(new LoadAppHolders(context));
	}

    public static AppProvider fromDB(Context context) {
        return new AppProvider(new LoadAppHoldersFromDB(context));
    }

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> records = new ArrayList<Holder>();

		int relevance;
		String appNameLowerCased;
		for (int i = 0; i < holders.size(); i++) {
			relevance = 0;
			appNameLowerCased = holders.get(i).nameLowerCased;
			if (appNameLowerCased.startsWith(query))
				relevance = 100;
			else if (appNameLowerCased.contains(" " + query))
				relevance = 50;
			else if (appNameLowerCased.contains(query))
				relevance = 1;

			if (relevance > 0) {
				holders.get(i).displayName = holders.get(i).name.replaceFirst(
						"(?i)(" + Pattern.quote(query) + ")", "{$1}");
				holders.get(i).relevance = relevance;
				records.add(holders.get(i));
			}
		}

		return records;
	}

	public Holder findById(String id) {
		for (int i = 0; i < holders.size(); i++) {
			if (holders.get(i).id.equals(id)) {
				holders.get(i).displayName = holders.get(i).name;
				return holders.get(i);
			}

		}

		return null;
	}

    @Override
    public void saveProvider(Context context) {
        DBHelper.saveAppHolders(context, holders);
    }
}
