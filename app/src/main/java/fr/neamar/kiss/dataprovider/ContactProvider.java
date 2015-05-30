package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import fr.neamar.kiss.pojo.ContactPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.loader.LoadContactPojos;

public class ContactProvider extends Provider<ContactPojo> {

	public ContactProvider(final Context context) {
		super(new LoadContactPojos(context));
	}

	public ArrayList<Pojo> getResults(String query) {
		ArrayList<Pojo> results = new ArrayList<Pojo>();

		int relevance;
		String contactNameLowerCased;
		for (int i = 0; i < pojos.size(); i++) {
			ContactPojo contact = pojos.get(i);
			relevance = 0;
			contactNameLowerCased = contact.nameLowerCased;

			if (contactNameLowerCased.startsWith(query))
				relevance = 50;
			else if (contactNameLowerCased.contains(" " + query))
				relevance = 40;

			if (relevance > 0) {
				// Increase relevance according to number of times the contacts
				// was phoned :
				relevance += contact.timesContacted;
				// Increase relevance for starred contacts:
				if (contact.starred)
					relevance += 30;
				// Decrease for home numbers:
				if (contact.homeNumber)
					relevance -= 1;

				contact.displayName = pojos.get(i).name.replaceFirst(
						"(?i)(" + Pattern.quote(query) + ")", "{$1}");
				contact.relevance = relevance;
				results.add(contact);
			}
		}

		return results;
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
}
