package fr.neamar.summon.lite;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.SharedPreferences;
import fr.neamar.summon.dataprovider.AppProvider;
import fr.neamar.summon.dataprovider.ContactProvider;
import fr.neamar.summon.dataprovider.Provider;
import fr.neamar.summon.dataprovider.SearchProvider;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.record.RecordComparator;

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
	}

	/**
	 * Get records for this query.
	 * 
	 * @param query
	 * 
	 * @return ordered list of records
	 */
	public ArrayList<Record> getRecords(String query) {
		// Save currentQuery
		SharedPreferences prefs = context.getSharedPreferences("history",
				Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString("currentQuery", query);
		ed.commit();

		if (query.isEmpty()) {
			// Searching for nothing returns the history
			return getHistory();
		}

		// Have we ever made the same query and selected something ?
		String lastIdForQuery = prefs.getString("query://" + query, "(none)");
		// Ask all providers for datas
		ArrayList<Record> allRecords = new ArrayList<Record>();

		for (int i = 0; i < providers.size(); i++) {
			ArrayList<Record> records = providers.get(i).getRecords(query);
			for (int j = 0; j < records.size(); j++) {
				// Give a boost if item was previously selected for this query
				if (records.get(j).holder.id.equals(lastIdForQuery))
					records.get(j).relevance += 50;
				allRecords.add(records.get(j));
			}
		}

		// Sort records according to relevance
		Collections.sort(allRecords, new RecordComparator());

		return allRecords;
	}

	/**
	 * Return previously selected items.<br />
	 * May return null if no items were ever selected (app first use)<br />
	 * May return an empty set if the providers are not done building records,
	 * in this case it is probably a good idea to call this function 500ms after
	 * 
	 * @return
	 */
	protected ArrayList<Record> getHistory() {
		ArrayList<Record> history = new ArrayList<Record>();

		// Read history
		ArrayList<String> ids = new ArrayList<String>();
		SharedPreferences prefs = context.getSharedPreferences("history",
				Context.MODE_PRIVATE);

		for (int k = 0; k < 50; k++) {
			String id = prefs.getString(Integer.toString(k), "(none)");

			// Not enough history yet
			if (id.equals("(none)")) {

				if (k == 0)
					return null;// App first use !
				else
					break;// Not enough item in history yet, we'll do with
							// this.
			}

			// No duplicates, only keep recent
			if (!ids.contains(id))
				ids.add(id);
		}

		// Find associated items
		for (int i = 0; i < ids.size(); i++) {

			// Ask all providers if they know this id
			for (int j = 0; j < providers.size(); j++) {
				Record record = providers.get(j).findById(ids.get(i));
				if (record != null) {
					history.add(record);
					break;
				}
			}
		}

		return history;
	}
}
