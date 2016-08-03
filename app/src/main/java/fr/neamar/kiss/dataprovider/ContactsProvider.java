package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.database.ContentObserver;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;

import fr.neamar.kiss.loader.LoadContactsPojos;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
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

    private FuzzySearch.CallBack cb = new FuzzySearch.CallBack() {
        @Override
        public Pojo notRelevant(String query, Pojo pojo) {
            ContactsPojo contact = (ContactsPojo) pojo;
            if (contact.nickname.contains(query)) {
                contact.displayName = contact.name
                        + " <small>("
                        + contact.nickname.replaceFirst(
                        "(?i)(" + Pattern.quote(query) + ")", "{$1}")
                        + ")</small>";
                contact.relevance = 30;
            } else if (query.length() > 2) {
                if (contact.phoneSimplified.startsWith(query)) {
                    contact.relevance = 10;
                } else if (contact.phoneSimplified.contains(query)) {
                    contact.relevance = 5;
                }
            }
            return contact;
        }
    };

    public ArrayList<Pojo> getResults(String query) {
        // Search people with composed names, e.g "jean-marie"
        // (not part of the StringNormalizer class, since we want to keep dashes on other providers)
        query = query.replaceAll("-", " ");

        boolean fuzzy = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean("contacts-fuzzy", false);
        ArrayList<Pojo> fsResult;
        if (fuzzy) {
            fsResult = FuzzySearch.fuzzySearch(query, pojos, cb);
            return fsResult;
        }
        ArrayList<Pojo> results = new ArrayList<>();

        int matchPositionStart;
        int matchPositionEnd;
        String contactNameNormalized;

        final String queryWithSpace = " " + query;
        for (ContactsPojo contact : pojos) {
            contact.relevance = 0;
            contactNameNormalized = contact.nameNormalized;
            boolean alias = false;

            matchPositionStart = 0;
            matchPositionEnd = 0;
            if (contactNameNormalized.startsWith(query)) {
                contact.relevance = 50;
                matchPositionEnd = matchPositionStart + query.length();
            } else if ((matchPositionStart = contactNameNormalized.indexOf(queryWithSpace)) > -1) {
                contact.relevance = 40;
                matchPositionEnd = matchPositionStart + queryWithSpace.length();
            } else {
                contact = (ContactsPojo) cb.notRelevant(query, contact);
                alias = true;
            }

            if (contact.relevance > 0) {
                // Increase relevance according to number of times the contacts
                // was phoned :
                contact.relevance += contact.timesContacted;
                // Increase relevance for starred contacts:
                if (contact.starred)
                    contact.relevance += 30;
                // Decrease for home numbers:
                if (contact.homeNumber)
                    contact.relevance -= 1;

                if (! alias)
                    contact.setDisplayNameHighlightRegion(matchPositionStart, matchPositionEnd);
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
        // We need to normalize, since the phone can be without any formatting and with international code,
        // Contacts are stored with formatting and sometimes without code
        // Thus, normalizing them allow for simpler comparison
        // (contact phone number are already normalized at build time)
        String normalizedPhoneNumber = PhoneNormalizer.normalizePhone(phoneNumber);

        for (ContactsPojo pojo : pojos) {
            if (pojo.phone.equals(normalizedPhoneNumber)) {
                return pojo;
            }
        }

        return null;
    }
}
