package fr.neamar.kiss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;

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

	public String currentQuery;

	/**
	 * List all known providers
	 */
	private final ArrayList<Provider> providers = new ArrayList<Provider>();
	private final AppProvider appProvider;
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
		if (prefs.getBoolean("enable-apps", true)) {
			providers.add(appProvider);
		}
		if (prefs.getBoolean("enable-contacts", true)) {
			providers.add(new ContactProvider(context));
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
		if (prefs.getBoolean("enable-aliases", true)) {
			providers.add(new AliasProvider(context, providers));
		}
	}

	/**
	 * Get records for this query.
	 *
	 * @param query
	 *
	 * @return ordered list of records
	 */
	public ArrayList<Pojo> getResults(Context context, String query) {
		query = query.toLowerCase();

		currentQuery = query;

		// Have we ever made the same query and selected something ?
		ArrayList<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(
				context, query);

		// Ask all providers for datas
		ArrayList<Pojo> allPojos = new ArrayList<Pojo>();

		for (int i = 0; i < providers.size(); i++) {

			// Retrieve results for query:
			ArrayList<Pojo> pojos = providers.get(i).getResults(query);

			// Add results to list
			for (int j = 0; j < pojos.size(); j++) {
				// Give a boost if item was previously selected for this query
				for (int k = 0; k < lastIdsForQuery.size(); k++) {
					if (pojos.get(j).id.equals(lastIdsForQuery.get(k).record)) {
						pojos.get(j).relevance += 25 * Math.min(5, lastIdsForQuery.get(k).value);
					}
				}

				allPojos.add(pojos.get(j));
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
	 * @param context
	 * @param itemCount max number of items to retrieve, total number may be less (search or calls are not returned for instance)
	 * @return
	 */
	public ArrayList<Pojo> getHistory(Context context, int itemCount) {
		ArrayList<Pojo> history = new ArrayList<Pojo>(itemCount);

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

	/**
	 * Return all applications
	 * @param context
	 * @return
	 */
	public ArrayList<Pojo> getApplications(Context context) {
		return appProvider.getAllApps();
	}

	/**
	 * Return most used items.<br />
	 * May return null if no items were ever selected (app first use)
	 * @param context
	 * @param limit max number of items to retrieve. You may end with less items if favorites contains non existing items.
	 * @return
	 */
	protected ArrayList<Pojo> getFavorites(Context context, int limit) {
		ArrayList<Pojo> favorites = new ArrayList<Pojo>();

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

	private Pojo getPojo(String id)
	{
		// Ask all providers if they know this id
		for (int i = 0; i < providers.size(); i++) {
			if (providers.get(i).mayFindById(id)) {
				return providers.get(i).findById(id);
			}
		}

		return null;
	}
}
