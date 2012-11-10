package fr.neamar.summon;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import fr.neamar.summon.dataprovider.AliasProvider;
import fr.neamar.summon.dataprovider.AppProvider;
import fr.neamar.summon.dataprovider.ContactProvider;
import fr.neamar.summon.dataprovider.Provider;
import fr.neamar.summon.dataprovider.SearchProvider;
import fr.neamar.summon.dataprovider.SettingProvider;
import fr.neamar.summon.dataprovider.ToggleProvider;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.holder.HolderComparator;
import fr.neamar.summon.misc.DBHelper;
import fr.neamar.summon.misc.ValuedHistoryRecord;

public class DataHandler {

	private Context context;

	/**
	 * List all knowns providers
	 */
	private ArrayList<Provider> providers = new ArrayList<Provider>();

	/**
	 * Initialize all providers
	 */
	public DataHandler(Context context) {
		this.context = context;

		// Initialize providers
		providers.add(new AppProvider(context));
		providers.add(new ContactProvider(context));
		providers.add(new SearchProvider(context));
		providers.add(new ToggleProvider(context));
		providers.add(new SettingProvider(context));
		providers.add(new AliasProvider(context, providers));
	}

	/**
	 * Get records for this query.
	 * 
	 * @param query
	 * 
	 * @return ordered list of records
	 */
	public ArrayList<Holder> getResults(String query) {
		query = query.toLowerCase();

		// Save currentQuery
		SharedPreferences prefs = context.getSharedPreferences("history",
				Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString("currentQuery", query);
		ed.commit();

		if (query.length() == 0) {
			// Searching for nothing returns the history
			return getHistory();
		}

		// Have we ever made the same query and selected something ?
		ArrayList<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(context, query);

		// Ask all providers for datas
		ArrayList<Holder> allHolders = new ArrayList<Holder>();

		for (int i = 0; i < providers.size(); i++) {
			ArrayList<Holder> holders = providers.get(i).getResults(query);
			for (int j = 0; j < holders.size(); j++) {
				// Give a boost if item was previously selected for this query
				for(int k = 0; k < lastIdsForQuery.size(); k++)
				{
					if (holders.get(j).id.equals(lastIdsForQuery.get(k).record))
					{
						holders.get(j).relevance += 25 * Math.min(5, lastIdsForQuery.get(k).value);
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
	protected ArrayList<Holder> getHistory() {
		ArrayList<Holder> history = new ArrayList<Holder>();

		// Read history
		ArrayList<ValuedHistoryRecord> ids = DBHelper.getHistory(context, 50);

		// Find associated items
		for (int i = 0; i < ids.size(); i++) {
			// Ask all providers if they know this id
			for (int j = 0; j < providers.size(); j++) {
				if (providers.get(j).mayFindById(ids.get(i).record)) {
					Holder holder = providers.get(j).findById(ids.get(i).record);
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
