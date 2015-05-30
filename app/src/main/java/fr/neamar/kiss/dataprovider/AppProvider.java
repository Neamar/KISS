package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.loader.LoadAppPojos;

public class AppProvider extends Provider<AppPojo> {

	public AppProvider(Context context) {
		super(new LoadAppPojos(context));
	}

	public ArrayList<Pojo> getResults(String query) {
		ArrayList<Pojo> records = new ArrayList<Pojo>();

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

	public Pojo findById(String id) {
		for (int i = 0; i < holders.size(); i++) {
			if (holders.get(i).id.equals(id)) {
				holders.get(i).displayName = holders.get(i).name;
				return holders.get(i);
			}

		}

		return null;
	}
}
