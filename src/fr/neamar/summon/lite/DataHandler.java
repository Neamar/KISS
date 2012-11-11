package fr.neamar.summon.lite;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import fr.neamar.summon.lite.dataprovider.AliasProvider;
import fr.neamar.summon.lite.dataprovider.AppProvider;
import fr.neamar.summon.lite.dataprovider.ContactProvider;
import fr.neamar.summon.lite.dataprovider.Provider;
import fr.neamar.summon.lite.dataprovider.SearchProvider;
import fr.neamar.summon.lite.dataprovider.SettingProvider;
import fr.neamar.summon.lite.dataprovider.ToggleProvider;
import fr.neamar.summon.lite.db.DBHelper;
import fr.neamar.summon.lite.db.ValuedHistoryRecord;
import fr.neamar.summon.lite.holder.Holder;
import fr.neamar.summon.lite.holder.HolderComparator;

public class DataHandler {

	public String currentQuery;

	/**
	 * List all knowns providers
	 */
	private ArrayList<Provider> providers = new ArrayList<Provider>();

	/**
	 * Initialize all providers
	 */
	public DataHandler(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		// Initialize providers
		if(prefs.getBoolean("enable-apps", true)){
			providers.add(new AppProvider(context));
		}
		if(prefs.getBoolean("enable-contacts", true)){
			providers.add(new ContactProvider(context));
		}
		if(prefs.getBoolean("enable-search", true)){
			providers.add(new SearchProvider());
		}
		if(prefs.getBoolean("enable-toggles", true)){
			providers.add(new ToggleProvider(context));
		}
		if(prefs.getBoolean("enable-settings", true)){
			providers.add(new SettingProvider(context));
		}
		if(prefs.getBoolean("enable-aliases", true)){
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
	public ArrayList<Holder> getResults(Context context, String query) {
		query = query.toLowerCase();

		currentQuery = query;

		if (query.length() == 0) {
			// Searching for nothing returns the history
			return getHistory(context);
		}

		// Have we ever made the same query and selected something ?
		ArrayList<ValuedHistoryRecord> lastIdsForQuery = DBHelper
				.getPreviousResultsForQuery(context, query);

		// Ask all providers for datas
		ArrayList<Holder> allHolders = new ArrayList<Holder>();

		for (int i = 0; i < providers.size(); i++) {
			
			//Retrieve results for query:
			ArrayList<Holder> holders = providers.get(i).getResults(query);
			
			//Add results to list
			for (int j = 0; j < holders.size(); j++) {
				
				// Give a boost if item was previously selected for this query
				for (int k = 0; k < lastIdsForQuery.size(); k++) {
					if (holders.get(j).id.equals(lastIdsForQuery.get(k).record)) {
						holders.get(j).relevance += 25 * Math.min(5,
								lastIdsForQuery.get(k).value);
					}
				}
				
				allHolders.add(holders.get(j));
			}
		}

		// Sort records according to relevance
		Collections.sort(allHolders, new HolderComparator());

		return allHolders;
	}

	/**
	 * Return previously selected items.<br />
	 * May return null if no items were ever selected (app first use)<br />
	 * May return an empty set if the providers are not done building records,
	 * in this case it is probably a good idea to call this function 500ms after
	 * 
	 * @return
	 */
	protected ArrayList<Holder> getHistory(Context context) {
		ArrayList<Holder> history = new ArrayList<Holder>();

		// Read history
		ArrayList<ValuedHistoryRecord> ids = DBHelper.getHistory(context, 50);

		// Find associated items
		for (int i = 0; i < ids.size(); i++) {
			// Ask all providers if they know this id
			for (int j = 0; j < providers.size(); j++) {
				if (providers.get(j).mayFindById(ids.get(i).record)) {
					Holder holder = providers.get(j)
							.findById(ids.get(i).record);
					if (holder != null) {
						history.add(holder);
						break;
					}
				}
			}
		}

		return history;
	}
}
