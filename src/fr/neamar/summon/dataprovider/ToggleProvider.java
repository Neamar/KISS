package fr.neamar.summon.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.holder.ToggleHolder;
import fr.neamar.summon.task.LoadToggleHolders;
import fr.neamar.summon.task.LoadToggleHoldersFromDB;

public class ToggleProvider extends Provider<ToggleHolder> {

    public ToggleProvider(Context context) {
		super(new LoadToggleHolders(context));
	}

    private ToggleProvider(LoadToggleHoldersFromDB loader) {
        super(loader);
    }

    public static ToggleProvider fromDB(Context context){
        return  new ToggleProvider(new LoadToggleHoldersFromDB(context));
    }

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> results = new ArrayList<Holder>();

		int relevance;
		String toggleNameLowerCased;
		for (int i = 0; i < holders.size(); i++) {
			ToggleHolder toggle = holders.get(i);

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
				results.add(toggle);
			}
		}

		return results;
	}

	public Holder findById(String id) {
		for (int i = 0; i < holders.size(); i++) {
			if (holders.get(i).id.equals(id)) {
				holders.get(i).displayName = holders.get(i).name.replace("Toggle:",
						"<small><small>Toggle:</small></small>");
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
