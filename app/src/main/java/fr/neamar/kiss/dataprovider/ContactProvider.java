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
        query = StringNormalizer.normalize(query);
        ArrayList<Pojo> results = new ArrayList<>();

        // Search people with composed names, e.g "jean-marie"
        // (not part of the StringNormalizer class, since we want to keep dashes on other providers)
        query = query.replaceAll("-", " ");

        int relevance;
        int matchPositionStart;
        int matchPositionEnd;
        String contactNameNormalized;

        final String queryWithSpace = " " + query;
        for (ContactPojo contact : pojos) {
            relevance = 0;
            contactNameNormalized = contact.nameNormalized;

            matchPositionStart = 0;
            matchPositionEnd = 0;
            if (contactNameNormalized.startsWith(query)) {
                relevance = 50;
                matchPositionEnd = matchPositionStart + query.length();
            } else if ((matchPositionStart = contactNameNormalized.indexOf(queryWithSpace)) > -1) {
                relevance = 40;
                matchPositionEnd = matchPositionStart + queryWithSpace.length();
            }

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

                contact.setDisplayNameHighlightRegion(matchPositionStart, matchPositionEnd);
                contact.relevance = relevance;
                results.add(contact);

                // Circuit-breaker to avoid spending too much time
                // building results
                // Important: this is made possible because LoadContactPojos already
                // returns contacts sorted by popularity, so the first items should be the most useful ones.
                // (short queries, e.g. "a" with thousands of contacts,
                // can return hundreds of results which are then slow to sort and display)
                if (results.size() > 50) {
                    break;
                }
            }
        }

        return results;
    }

    public Pojo findById(String id) {
        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                pojo.displayName = pojo.name;
                return pojo;
            }
        }

        return null;
    }


    /**
     * Find a ContactPojo from a phoneNumber
     * If many contacts match, the one most often contacted will be returned
     *
     * @param phoneNumber phone number to find (will be normalized)
     * @return a contactpojo, or null.
     */
    public ContactPojo findByPhone(String phoneNumber) {
        // We need to normalize, since the phone can be without any formatting and with international code,
        // Contacts are stored with formatting and sometimes without code
        // Thus, normalizing them allow for simpler comparison
        // (contact phone number are already normalized at build time)
        String normalizedPhoneNumber = PhoneNormalizer.normalizePhone(phoneNumber);

        for (ContactPojo pojo : pojos) {
            if (pojo.phone.equals(normalizedPhoneNumber)) {
                return pojo;
            }
        }

        return null;
    }
}
