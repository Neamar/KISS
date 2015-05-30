package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadAppPojos;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;

public class AppProvider extends Provider<AppPojo> {

	public AppProvider(Context context) {
		super(new LoadAppPojos(context));
	}

	public ArrayList<Pojo> getResults(String query) {
		ArrayList<Pojo> records = new ArrayList<Pojo>();

		int relevance;
		String appNameLowerCased;
		for (int i = 0; i < pojos.size(); i++) {
			relevance = 0;
			appNameLowerCased = pojos.get(i).nameLowerCased;
			if (appNameLowerCased.startsWith(query))
				relevance = 100;
			else if (appNameLowerCased.contains(" " + query))
				relevance = 50;
			else if (appNameLowerCased.contains(query))
				relevance = 1;

			if (relevance > 0) {
				pojos.get(i).displayName = pojos.get(i).name.replaceFirst(
						"(?i)(" + Pattern.quote(query) + ")", "{$1}");
				pojos.get(i).relevance = relevance;
				records.add(pojos.get(i));
			}
		}

		return records;
	}

	public Pojo findById(String id) {
		for (int i = 0; i < pojos.size(); i++) {
			if (pojos.get(i).id.equals(id)) {
				pojos.get(i).displayName = pojos.get(i).name;
				return pojos.get(i);
			}

		}

		return null;
	}

	public ArrayList<Pojo> getAllApps() {
		ArrayList<Pojo> records = new ArrayList<Pojo>(pojos.size());
		records.trimToSize();
		String appNameLowerCased;

		for (int i = 0; i < pojos.size(); i++) {
			pojos.get(i).displayName = pojos.get(i).name;
			records.add(pojos.get(i));
		}
		return records;
	}
}
