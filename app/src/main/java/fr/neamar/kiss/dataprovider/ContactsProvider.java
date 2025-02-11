package fr.neamar.kiss.dataprovider;

import android.database.ContentObserver;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

import fr.neamar.kiss.loader.LoadContactsPojos;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.fuzzy.FuzzyFactory;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;
import fr.neamar.kiss.utils.Permission;
import fr.neamar.kiss.utils.fuzzy.MatchInfo;

public class ContactsProvider extends Provider<ContactsPojo> {
    protected static final String TAG = ContactsProvider.class.getSimpleName();
    private final ContentObserver cObserver = new ContentObserver(null) {

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            onChange(selfChange, uri, 0);
        }

        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri, int flags) {
            onChange(selfChange, Collections.singletonList(uri), flags);
        }

        @Override
        public void onChange(boolean selfChange, @NonNull Collection<Uri> uris, int flags) {
            //reload contacts
            Log.v(TAG, "Contacts changed, reloading provider: " + uris + ", flags: " + flags);
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
        if (Permission.checkPermission(this, Permission.PERMISSION_READ_CONTACTS)) {
            getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, cObserver);
        } else {
            Permission.askPermission(Permission.PERMISSION_READ_CONTACTS, new Permission.PermissionResultListener() {
                @Override
                public void onGranted() {
                    // Great! Reload the contact provider. We're done :)
                    reload();
                }
            });
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

        FuzzyScore fuzzyScore = FuzzyFactory.createFuzzyScore(this, queryNormalized.codePoints);

        for (ContactsPojo pojo : getPojos()) {
            MatchInfo matchInfo;
            boolean match = false;

            if (pojo.normalizedName != null) {
                matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
                match = pojo.updateMatchingRelevance(matchInfo, match);
            }

            // Match also for alternative name, see https://developer.android.com/reference/android/provider/ContactsContract.ContactNameColumns#DISPLAY_NAME_ALTERNATIVE
            // This may result in better match but eventually some missing highlighting
            if (pojo.normalizedNameAlternative != null) {
                matchInfo = fuzzyScore.match(pojo.normalizedNameAlternative.codePoints);
                match = pojo.updateMatchingRelevance(matchInfo, match);
            }

            // Match also for phonetic name
            // This may result in better match but eventually some missing highlighting
            if (pojo.normalizedPhoneticName != null) {
                matchInfo = fuzzyScore.match(pojo.normalizedPhoneticName.codePoints);
                match = pojo.updateMatchingRelevance(matchInfo, match);
            }

            if (pojo.normalizedNickname != null) {
                matchInfo = fuzzyScore.match(pojo.normalizedNickname.codePoints);
                match = pojo.updateMatchingRelevance(matchInfo, match);
            }

            if (!match && queryNormalized.length() > 2 && pojo.normalizedPhone != null) {
                // search for the phone number
                matchInfo = fuzzyScore.match(pojo.normalizedPhone.codePoints);
                match = pojo.updateMatchingRelevance(matchInfo, match);
            }

            if (!match && queryNormalized.length() > 2 && pojo.getContactData() != null && pojo.getContactData().getNormalizedIdentifier() != null) {
                // search for IM identifier
                matchInfo = fuzzyScore.match(pojo.getContactData().getNormalizedIdentifier().codePoints);
                match = pojo.updateMatchingRelevance(matchInfo, match);
            }

            if (match) {
                if (pojo.starred) {
                    pojo.relevance += 40;
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
        StringNormalizer.Result simplifiedPhoneNumber = PhoneNormalizer.simplifyPhoneNumber(phoneNumber);

        for (ContactsPojo pojo : getPojos()) {
            if (pojo.normalizedPhone != null && pojo.normalizedPhone.equals(simplifiedPhoneNumber)) {
                return pojo;
            }
        }

        return null;
    }
}
