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
    private static final int DEFAULT_MAX_RESULTS = 15;
    /**
     * Store user preferences
     */
    private SharedPreferences prefs;

    public QuerySearcher(MainActivity activity, String query) {
        super(activity);
        this.query = query;
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);

    }

    @Override
    protected List<Pojo> doInBackground(Void... voids) {
        // Ask for records
        final List<Pojo> pojos = KissApplication.getDataHandler(activity).getResults(
                activity, query);

        // Trim items
        int max_records = Integer.parseInt(prefs.getString("number-of-search-results", String.valueOf(DEFAULT_MAX_RESULTS)));

        if (pojos.size() > max_records) {
            return pojos.subList(0, max_records);
        }

        return pojos;
    }
}
