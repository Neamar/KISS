package fr.neamar.kiss.dataprovider;

import android.database.ContentObserver;
import android.provider.ContactsContract;
import android.util.Pair;

import java.util.List;

import fr.neamar.kiss.loader.LoadContactsPojos;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;

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

    @Override
    public void requestResults( String query, Searcher searcher )
    {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult( query, false );
        // Search people with composed names, e.g "jean-marie"
        // (not part of the StringNormalizer class, since we want to keep dashes on other providers)
        queryNormalized = queryNormalized.replaceAll(Character.codePointAt("-", 0), Character.codePointAt(" ", 0));

        FuzzyScore   fuzzyScore = new FuzzyScore( queryNormalized.codePoints );
        FuzzyScore.MatchInfo matchInfo  = new FuzzyScore.MatchInfo();
        for (ContactsPojo pojo : pojos)
        {
            boolean match = fuzzyScore.match( pojo.normalizedName.codePoints, matchInfo );
            pojo.relevance = matchInfo.score;

            if ( match )
            {
                List<Pair<Integer, Integer>> positions = matchInfo.getMatchedSequences();
                try
                {
                    pojo.setDisplayNameHighlightRegion( positions );
                } catch( Exception e )
                {
                    pojo.setDisplayNameHighlightRegion( 0, pojo.normalizedName.length() );
                }
            }

            if ( !pojo.nickname.isEmpty() )
            {
                if( fuzzyScore.match( pojo.nickname, matchInfo ) )
                {
                    if( !match || (matchInfo.score > pojo.relevance) )
                    {
                        match = true;
                        pojo.relevance = matchInfo.score;
                        pojo.displayName = pojo.getName()
                                           + " <small>({"
                                           + pojo.nickname
                                           + "})</small>";
                    }
                }
            }

            if ( !match && queryNormalized.length() > 2 )
            {
                // search for the phone number
                if( fuzzyScore.match( pojo.phoneSimplified, matchInfo ) )
                {
                    match = true;
                    pojo.relevance = matchInfo.score;
                    pojo.setDisplayNameHighlightRegion(0, 0);
                }
            }

            if( match )
            {
                if( !searcher.addResult( pojo ) )
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
