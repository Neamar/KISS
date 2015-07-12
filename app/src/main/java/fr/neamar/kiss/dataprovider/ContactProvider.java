package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadContactPojos;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
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

        final String highlightRegexp = "(?i)(" + StringNormalizer.unNormalize(query) + ")";
        final String queryWithSpace = " " + query;

        for (int i = 0; i < pojos.size(); i++) {
            ContactPojo contact = pojos.get(i);
            relevance = 0;
            contactNameLowerCased = contact.nameLowerCased;

            if (contactNameLowerCased.startsWith(query))
                relevance = 50;
            else if (contactNameLowerCased.contains(queryWithSpace))
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

                // Circuit-breaker to avoid spending too much time
                // building results
                // Important: this is made possible because LoadContactPojos already
                // returns contacts sorted by popularity, so the first items should be the most useful ones.
                // (short queries, e.g. "a" with thousands of contacts,
                // can return hundreds of results which are then slow to sort and display)
                if(results.size() > 50) {
                    break;
                }
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


    /**
     * Find a ContactPojo from a phoneNumber
     * If many contacts match, the one most often contacted will be returned
     * @param phoneNumber phone number to find (will be normalized)
     * @return a contactpojo, or null.
     */
    public ContactPojo findByPhone(String phoneNumber) {
        // We need to normalize, since the phone can be without any formatting and with international code,
        // Contacts are stored with formatting and sometimes without code
        // Thus, normalizing them allow for simpler comparison
        // (contact phone number are already normalized at build time)
        String normalizedPhoneNumber = PhoneNormalizer.normalizePhone(phoneNumber);

        for (int i = 0; i < pojos.size(); i++) {
            if (pojos.get(i).phone.equals(normalizedPhoneNumber)) {
                return pojos.get(i);
            }
        }

        return null;
    }
}
