package fr.neamar.kiss.searcher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;

/**
 * AsyncTask retrieving data from the providers and updating the view
 *
 * @author dorvaryn
 */
public class QuerySearcher extends Searcher {

    private final String query;
    /**
     * Store user preferences
     */
    private SharedPreferences prefs;

    public QuerySearcher(MainActivity activity, String query) {
        super(activity);
        this.query = query;
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);

    }

    /**
     * Called on the background thread 
     */
    @Override
    protected Void doInBackground( Void... voids )
    {
        // Request results via "addResult"
        KissApplication.getDataHandler(activity).requestResults( activity, query, this );
        return null;
    }
}
