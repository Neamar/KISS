package fr.neamar.kiss.searcher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;

/**
 * Retrieve pojos from history
 */
public class HistorySearcher extends Searcher {
    private SharedPreferences prefs;

    public HistorySearcher(MainActivity activity) {
        super(activity, "<history>");
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    @Override
    protected Void doInBackground( Void... voids )
    {
        // Ask for records
        boolean smartHistory = !prefs.getString("history-mode", "recency").equals("recency");
        boolean excludeFavorites = prefs.getBoolean("exclude-favorites", false);

        // Convert `"number-of-display-elements"` to double first before truncating to int to avoid
        // `java.lang.NumberFormatException` crashes for values larger than `Integer.MAX_VALUE`
        int maxRecords = (Double.valueOf(prefs.getString("number-of-display-elements", String.valueOf(DEFAULT_MAX_RESULTS)))).intValue();

        MainActivity activity = activityWeakReference.get();
        if( activity == null )
            return null;

        //Gather favorites
        ArrayList<Pojo> favoritesPojo = new ArrayList<>( 0 );
        if(excludeFavorites){
            favoritesPojo = KissApplication.getDataHandler(activity).getFavorites(activity.tryToRetrieve);
        }

        List<Pojo> pojos = KissApplication.getDataHandler(activity).getHistory(activity, maxRecords, smartHistory, favoritesPojo);
        this.addResult( pojos.toArray(new Pojo[0]) );
        return null;
    }
}
