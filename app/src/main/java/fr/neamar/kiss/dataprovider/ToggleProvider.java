package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.TogglePojo;
import fr.neamar.kiss.loader.LoadTogglePojos;

public class ToggleProvider extends Provider<TogglePojo> {

	public ToggleProvider(Context context) {
		super(new LoadTogglePojos(context));
	}

	public ArrayList<Pojo> getResults(String query) {
		ArrayList<Pojo> results = new ArrayList<>();

		int relevance;
		String toggleNameLowerCased;
		for (int i = 0; i < pojos.size(); i++) {
			TogglePojo toggle = pojos.get(i);

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

	public Pojo findById(String id) {
		for (int i = 0; i < pojos.size(); i++) {
			if (pojos.get(i).id.equals(id)) {
				pojos.get(i).displayName = pojos.get(i).name.replace("Toggle:",
						"<small><small>Toggle:</small></small>");
				return pojos.get(i);
			}

		}

		return null;
	}
}
