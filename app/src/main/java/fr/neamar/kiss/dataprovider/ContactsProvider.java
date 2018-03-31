package fr.neamar.kiss.dataprovider;

import android.database.ContentObserver;
import android.provider.ContactsContract;
import android.util.Pair;

import java.util.List;

import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.loader.LoadContactsPojos;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;

public class ContactsProvider extends Provider<ContactsPojo> {

    private final ContentObserver cObserver = new ContentObserver(null) {

        @Override
        public void onChange(boolean selfChange) {
            //reload contacts
            reload();
        }
    };

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadContactsPojos(this));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // register content observer if we have permission
        if(Permission.checkContactPermission(this)) {
            getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, cObserver);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //deregister content observer
        getContentResolver().unregisterContentObserver(cObserver);
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo = new FuzzyScore.MatchInfo();
        for (ContactsPojo pojo : pojos) {
            boolean match = fuzzyScore.match(pojo.normalizedName.codePoints, matchInfo);
            pojo.relevance = matchInfo.score;

            if (!pojo.nickname.isEmpty()) {
                if (fuzzyScore.match(pojo.nickname, matchInfo)) {
                    if (!match || (matchInfo.score > pojo.relevance)) {
                        match = true;
                        pojo.relevance = matchInfo.score;
                    }
                }
            }

            if (!match && queryNormalized.length() > 2) {
                // search for the phone number
                if (fuzzyScore.match(pojo.phoneSimplified, matchInfo)) {
                    match = true;
                    pojo.relevance = matchInfo.score;
                }
            }

            if (match) {
                pojo.relevance += Math.min(15, pojo.timesContacted);
                if(pojo.starred) {
                    pojo.relevance += 15;
                }

                if (!searcher.addResult(pojo))
                    return;
            }
        }
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
