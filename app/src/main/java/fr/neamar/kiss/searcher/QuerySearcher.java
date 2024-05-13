package fr.neamar.kiss.searcher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ValuedHistoryRecord;
import fr.neamar.kiss.pojo.Pojo;

/**
 * AsyncTask retrieving data from the providers and updating the view
 *
 * @author dorvaryn
 */
public class QuerySearcher extends Searcher {
    private static int MAX_RESULT_COUNT = -1;
    private HashMap<String, Integer> knownIds;
    /**
     * Store user preferences
     */
    private final SharedPreferences prefs;

    public QuerySearcher(MainActivity activity, String query, boolean isRefresh) {
        super(activity, query, isRefresh);
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    @Override
    protected int getMaxResultCount() {
        if (MAX_RESULT_COUNT == -1) {
            // Convert `"number-of-display-elements"` to double first before truncating to int to avoid
            // `java.lang.NumberFormatException` crashes for values larger than `Integer.MAX_VALUE`
            try {
                MAX_RESULT_COUNT = Double.valueOf(prefs.getString("number-of-display-elements", String.valueOf(DEFAULT_MAX_RESULTS))).intValue();
            } catch (NumberFormatException e) {
                // If, for any reason, setting is empty, return default value.
                MAX_RESULT_COUNT = DEFAULT_MAX_RESULTS;
            }
        }

        return MAX_RESULT_COUNT;
    }

    @Override
    public boolean addResults(List<? extends Pojo> pojos) {
        for (Pojo pojo : pojos) {
            if (pojo.isDisabled()) {
                // Give penalty for disabled items, these should not be preferred
                pojo.relevance -= 200;
            } else {
                // Give a boost if item was previously selected for this query
                Integer value = knownIds.get(pojo.id);
                if (value != null) {
                    pojo.relevance += 25 * value;
                }
            }
        }

        // call super implementation to update the adapter
        return super.addResults(pojos);
    }

    /**
     * Called on the background thread
     */
    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return null;

        // Have we ever made the same query and selected something ?
        List<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(activity, query);
        knownIds = new HashMap<>();
        for (ValuedHistoryRecord id : lastIdsForQuery) {
            knownIds.put(id.record, id.value);
        }

        // Request results via "addResult"
        KissApplication.getApplication(activity).getDataHandler().requestResults(query, this);
        return null;
    }

    public static void clearMaxResultCountCache() {
        MAX_RESULT_COUNT = -1;
    }
}
