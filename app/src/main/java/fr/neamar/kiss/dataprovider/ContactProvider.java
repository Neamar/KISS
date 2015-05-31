package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadContactPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactPojo;
import fr.neamar.kiss.pojo.Pojo;

public class ContactProvider extends Provider<ContactPojo> {

    public ContactProvider(final Context context) {
        super(new LoadContactPojos(context));
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> results = new ArrayList<>();

        int relevance;
        String contactNameLowerCased;

        final String highlightRegexp = "(?i)(" + StringNormalizer.unnormalize(query) + ")";

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

                contact.displayName = pojos.get(i).name.replaceFirst(highlightRegexp, "{$1}");
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
