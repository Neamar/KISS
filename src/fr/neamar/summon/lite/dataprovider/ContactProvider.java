package fr.neamar.summon.lite.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import fr.neamar.summon.lite.holder.ContactHolder;
import fr.neamar.summon.lite.holder.Holder;
import fr.neamar.summon.lite.task.LoadContactHolders;

public class ContactProvider extends Provider<ContactHolder> {

	public ContactProvider(final Context context) {
		super(new LoadContactHolders(context));
	}

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> results = new ArrayList<Holder>();

		int relevance;
		String contactNameLowerCased;
		for (int i = 0; i < holders.size(); i++) {
			ContactHolder contact = holders.get(i);
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

				contact.displayName = holders.get(i).name.replaceFirst("(?i)("
						+ Pattern.quote(query) + ")", "{$1}");
				contact.relevance = relevance;
				results.add(contact);
			}
		}

		return results;
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
}
