package fr.neamar.kiss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import fr.neamar.kiss.dataprovider.AliasProvider;
import fr.neamar.kiss.dataprovider.AppProvider;
import fr.neamar.kiss.dataprovider.ContactProvider;
import fr.neamar.kiss.dataprovider.PhoneProvider;
import fr.neamar.kiss.dataprovider.Provider;
import fr.neamar.kiss.dataprovider.SearchProvider;
import fr.neamar.kiss.dataprovider.SettingProvider;
import fr.neamar.kiss.dataprovider.ToggleProvider;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ValuedHistoryRecord;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;

public class DataHandler extends BroadcastReceiver {

    /**
     * List all known providers
     */
    private final ArrayList<Provider<? extends Pojo>> providers = new ArrayList<>();
    private final AppProvider appProvider;
    private final ContactProvider contactProvider;
    private String currentQuery;
    private int providersLoaded = 0;

    /**
     * Initialize all providers
     */
    public DataHandler(Context context) {
        IntentFilter intentFilter = new IntentFilter(MainActivity.LOAD_OVER);
        context.getApplicationContext().registerReceiver(this, intentFilter);

        Intent i = new Intent(MainActivity.START_LOAD);
        context.sendBroadcast(i);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Initialize providers
        appProvider = new AppProvider(context);
        providers.add(appProvider);

        if (prefs.getBoolean("enable-contacts", true)) {
            contactProvider = new ContactProvider(context);
            providers.add(contactProvider);
        } else {
            contactProvider = null;
        }

        if (prefs.getBoolean("enable-search", true)) {
            providers.add(new SearchProvider(context));
        }
        if (prefs.getBoolean("enable-phone", true)) {
            providers.add(new PhoneProvider(context));
        }
        if (prefs.getBoolean("enable-toggles", true)) {
            providers.add(new ToggleProvider(context));
        }
        if (prefs.getBoolean("enable-settings", true)) {
            providers.add(new SettingProvider(context));
        }
        if (prefs.getBoolean("enable-alias", true)) {
            providers.add(new AliasProvider(context, appProvider));
        }
    }

    /**
     * Get records for this query.
     *
     * @param context android context
     * @param query   query to run
     * @return ordered list of records
     */
    public ArrayList<Pojo> getResults(Context context, String query) {
        query = query.toLowerCase().replaceAll("<", "&lt;");

        currentQuery = query;

        // Have we ever made the same query and selected something ?
        ArrayList<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(
                context, query);
        HashMap<String, Integer> knownIds = new HashMap<>();
        for (ValuedHistoryRecord id : lastIdsForQuery) {
            knownIds.put(id.record, id.value);
        }

        // Ask all providers for data
        ArrayList<Pojo> allPojos = new ArrayList<>();

        for (Provider<? extends Pojo> provider : providers) {
            // Retrieve results for query:
            ArrayList<Pojo> pojos = provider.getResults(query);

            // Add results to list
            for (Pojo pojo : pojos) {
                // Give a boost if item was previously selected for this query
                if (knownIds.containsKey(pojo.id)) {
                    pojo.relevance += 25 * Math.min(5, knownIds.get(pojo.id));
                }
                allPojos.add(pojo);
            }
        }

        // Sort records according to relevance
        Collections.sort(allPojos, new PojoComparator());

        return allPojos;
    }

    /**
     * Return previously selected items.<br />
     * May return null if no items were ever selected (app first use)<br />
     * May return an empty set if the providers are not done building records,
     * in this case it is probably a good idea to call this function 500ms after
     *
     * @param context   android context
     * @param itemCount max number of items to retrieve, total number may be less (search or calls are not returned for instance)
     * @return pojos in recent history
     */
    public ArrayList<Pojo> getHistory(Context context, int itemCount) {
        ArrayList<Pojo> history = new ArrayList<>(itemCount);

        // Read history
        ArrayList<ValuedHistoryRecord> ids = DBHelper.getHistory(context, itemCount);

        // Find associated items
        for (int i = 0; i < ids.size(); i++) {
            // Ask all providers if they know this id
            Pojo pojo = getPojo(ids.get(i).record);
            if (pojo != null) {
                history.add(pojo);
            }
        }

        return history;
    }

    public int getHistoryLength(Context context) {
        return DBHelper.getHistoryLength(context);
    }

    /**
     * Return all applications
     *
     * @return pojos for all applications
     */
    public ArrayList<Pojo> getApplications() {
        return appProvider.getAllApps();
    }

    public ContactProvider getContactProvider() {
        return contactProvider;
    }

    /**
     * Return most used items.<br />
     * May return null if no items were ever selected (app first use)
     *
     * @param context android context
     * @param limit   max number of items to retrieve. You may end with less items if favorites contains non existing items.
     * @return favorites' pojo
     */
    ArrayList<Pojo> getFavorites(Context context, int limit) {
        ArrayList<Pojo> favorites = new ArrayList<>();

        // Read history
        ArrayList<ValuedHistoryRecord> ids = DBHelper.getFavorites(context, limit);


        // Find associated items
        for (int i = 0; i < ids.size(); i++) {
            Pojo pojo = getPojo(ids.get(i).record);
            if (pojo != null) {
                favorites.add(pojo);
            }
        }


        return favorites;
    }

    /**
     * Insert specified ID (probably a pojo.id) into history
     *
     * @param context android context
     * @param id      pojo.id of item to record
     */
    public void addToHistory(Context context, String id) {
        DBHelper.insertHistory(context, currentQuery, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        providersLoaded++;
        if (providersLoaded == providers.size()) {
            try {
                context.unregisterReceiver(this);
                Intent i = new Intent(MainActivity.FULL_LOAD_OVER);
                context.sendBroadcast(i);
                providersLoaded = 0;
            } catch (IllegalArgumentException e) {
                // Nothing
            }
        }
    }

    private Pojo getPojo(String id) {
        // Ask all providers if they know this id
        for (Provider provider : providers) {
            if (provider.mayFindById(id)) {
                return provider.findById(id);
            }
        }

        return null;
    }
}