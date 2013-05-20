package fr.neamar.summon;

import android.content.*;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.neamar.summon.dataprovider.*;
import fr.neamar.summon.db.DBHelper;
import fr.neamar.summon.db.ValuedHistoryRecord;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.holder.HolderComparator;

public class DataHandler extends BroadcastReceiver {

	public String currentQuery;

	/**
	 * List all knowns providers
	 */
	private ArrayList<Provider> providers = new ArrayList<Provider>();
	private int providersLoaded = 0;
    private ExecutorService poolExecutor = Executors.newFixedThreadPool(6);

    private DataHandler() {
        super();
    }

    /**
	 * Initialize all providers
	 */
	public DataHandler(Context context) {
		IntentFilter intentFilter = new IntentFilter(SummonActivity.LOAD_OVER);
		context.getApplicationContext().registerReceiver(this, intentFilter);

		Intent i = new Intent(SummonActivity.START_LOAD);
		context.sendBroadcast(i);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		// Initialize providers
		if (prefs.getBoolean("enable-apps", true)) {
			providers.add(new AppProvider(context));
		}
		if (prefs.getBoolean("enable-contacts", true)) {
			providers.add(new ContactProvider(context));
		}
		if (prefs.getBoolean("enable-search", true)) {
			providers.add(new SearchProvider(context));
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

    public static DataHandler fromDB(Context context) {
        DataHandler newIntance = new DataHandler();

        Intent i = new Intent(SummonActivity.START_LOAD);
        context.sendBroadcast(i);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Initialize providers
        if (prefs.getBoolean("enable-apps", true)) {
            newIntance.providers.add(AppProvider.fromDB(context));
        }
        if (prefs.getBoolean("enable-contacts", true)) {
            newIntance.providers.add(ContactProvider.fromDB(context));
        }
        if (prefs.getBoolean("enable-search", true)) {
            newIntance.providers.add(SearchProvider.fromDB(context));
        }
        if (prefs.getBoolean("enable-toggles", true)) {
            newIntance.providers.add(ToggleProvider.fromDB(context));
        }
        if (prefs.getBoolean("enable-settings", true)) {
            newIntance.providers.add(SettingProvider.fromDB(context));
        }
        if (prefs.getBoolean("enable-aliases", true)) {
            newIntance.providers.add(AliasProvider.fromDB(context, newIntance.providers));
        }
        return newIntance;
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
		ArrayList<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(
				context, query);

		// Ask all providers for datas
		ArrayList<Holder> allHolders = new ArrayList<Holder>();

		for (int i = 0; i < providers.size(); i++) {

			// Retrieve results for query:
			ArrayList<Holder> holders = providers.get(i).getResults(query);

			// Add results to list
			for (int j = 0; j < holders.size(); j++) {

				// Give a boost if item was previously selected for this query
				for (int k = 0; k < lastIdsForQuery.size(); k++) {
					if (holders.get(j).id.equals(lastIdsForQuery.get(k).record)) {
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
	protected ArrayList<Holder> getHistory(Context context) {
		ArrayList<Holder> history = new ArrayList<Holder>();

		// Read history
		ArrayList<ValuedHistoryRecord> ids = DBHelper.getHistory(context, 50);

		// Find associated items
		for (int i = 0; i < ids.size(); i++) {
			// Ask all providers if they know this id
			for (int j = 0; j < providers.size(); j++) {
				if (providers.get(j).mayFindById(ids.get(i).record)) {
					//TODO: use new getHolder() function
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

	/**
	 * Return most used items.<br />
	 * May return null if no items were ever selected (app first use)
	 * 
	 * @return
	 */
	protected ArrayList<Holder> getFavorites(Context context) {
		ArrayList<Holder> favorites = new ArrayList<Holder>();

		// Read history
		ArrayList<ValuedHistoryRecord> ids = DBHelper.getFavorites(context, 5);

		
		// Find associated items
		for (int i = 0; i < ids.size(); i++) {
			Holder holder = getHolder(ids.get(i).record);
			if (holder != null) {
				favorites.add(holder);
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
                SummonApplication.loadingOver();
				Intent i = new Intent(SummonActivity.FULL_LOAD_OVER);
                context.sendBroadcast(i);
				providersLoaded = 0;
			} catch (IllegalArgumentException e) {
				// Nothing
			}
		}
	}

	private Holder getHolder(String id)
	{
		// Ask all providers if they know this id
		for (int i = 0; i < providers.size(); i++) {
			if (providers.get(i).mayFindById(id)) {
				return providers.get(i).findById(id);
			}
		}
			
		return null;
	}

    public void saveHandler(final Context context){
        new Thread(new Runnable() {
            @Override
            public void run() {
                DBHelper.clearHolders(context);
                for (Provider p : providers) {
                    p.saveProvider(context);
                }
            }
        }).start();
    }
}
