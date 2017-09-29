package fr.neamar.kiss.dataprovider;

import android.database.ContentObserver;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadContactsPojos;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.pojo.Pojo;

public class ContactsProvider extends Provider<ContactsPojo> {

    private ContentObserver cObserver = new ContentObserver(null) {

        @Override
        public void onChange(boolean selfChange) {
            //reload contacts
            reload();
        }
    };

    @Override
    public void reload() {
        this.initialize(new LoadContactsPojos(this));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //register content observer
        getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, cObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //deregister content observer
        getContentResolver().unregisterContentObserver(cObserver);
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
        for (ContactsPojo contact : pojos) {
            relevance = 0;
            contactNameNormalized = contact.nameNormalized;
            boolean alias = false;

            matchPositionStart = 0;
            matchPositionEnd = 0;
            if (contactNameNormalized.startsWith(query)) {
                relevance = 50;
                matchPositionEnd = matchPositionStart + query.length();
            } else if ((matchPositionStart = contactNameNormalized.indexOf(queryWithSpace)) > -1) {
                relevance = 40;
                matchPositionEnd = matchPositionStart + queryWithSpace.length();
            } else if (contact.nickname.contains(query)) {
                alias = true;
                contact.displayName = contact.name
                        + " <small>("
                        + contact.nickname.replaceFirst(
                        "(?i)(" + Pattern.quote(query) + ")", "{$1}")
                        + ")</small>";
                relevance = 30;
            } else if (query.length() > 2) {
                if ((matchPositionStart = contactNameNormalized.indexOf(query)) > -1) {
                    relevance = 15;
                    matchPositionEnd = matchPositionStart + query.length();
                } else {
                    matchPositionStart = 0;
                    matchPositionEnd = 0;
                    if (contact.phoneSimplified.startsWith(query)) {
                        relevance = 10;
                    } else if (contact.phoneSimplified.contains(query)) {
                        relevance = 5;
                    }
                }
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

                if (!alias)
                    contact.setDisplayNameHighlightRegion(matchPositionStart, matchPositionEnd);
                contact.relevance = relevance;
                results.add(contact);

                // Circuit-breaker to avoid spending too much time
                // building results
                // Important: this is made possible because LoadContactsPojos already
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

    public Pojo findByName(String name) {
        for (Pojo pojo : pojos) {
            if (pojo.name.equals(name))
                return pojo;
        }
        return null;
    }

    /**
     * Find a ContactsPojo from a phoneNumber
     * If many contacts match, the one most often contacted will be returned
     *
     * @param phoneNumber phone number to find (will be normalized)
     * @return a contactpojo, or null.
     */
    public ContactsPojo findByPhone(String phoneNumber) {
        String simplifiedPhoneNumber = PhoneNormalizer.simplifyPhoneNumber(phoneNumber);

        for (ContactsPojo pojo : pojos) {
            if (pojo.phoneSimplified.equals(simplifiedPhoneNumber)) {
                return pojo;
            }
        }

        return null;
    }
}
