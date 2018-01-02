package fr.neamar.kiss;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap.CompressFormat;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.neamar.kiss.dataprovider.AppProvider;
import fr.neamar.kiss.dataprovider.ContactsProvider;
import fr.neamar.kiss.dataprovider.IProvider;
import fr.neamar.kiss.dataprovider.Provider;
import fr.neamar.kiss.dataprovider.SearchProvider;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.db.ValuedHistoryRecord;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.utils.UserHandle;

public class DataHandler extends BroadcastReceiver
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * Package the providers reside in
     */
    final static private String PROVIDER_PREFIX = IProvider.class.getPackage().getName() + ".";
    /**
     * List all known providers
     */
    final static private List<String> PROVIDER_NAMES = Arrays.asList(
            "app", "contacts", "phone", "search", "settings", "shortcuts", "toggles"
    );

    final private Context context;
    private String currentQuery;

    private Map<String, ProviderEntry> providers = new HashMap<>();
    private boolean providersReady = false;

    private static TagsHandler tagsHandler;

    /**
     * Initialize all providers
     */
    public DataHandler(Context context) {
        // Make sure we are in the context of the main activity
        // (otherwise we might receive an exception about broadcast listeners not being able
        //  to bind to services)
        this.context = context.getApplicationContext();

        IntentFilter intentFilter = new IntentFilter(MainActivity.LOAD_OVER);
        this.context.getApplicationContext().registerReceiver(this, intentFilter);

        Intent i = new Intent(MainActivity.START_LOAD);
        this.context.sendBroadcast(i);

        // Monitor changes for service preferences (to automatically start and stop services)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Connect to initial providers
        for (String providerName : PROVIDER_NAMES) {
            if (prefs.getBoolean("enable-" + providerName, true)) {
                this.connectToProvider(providerName);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.startsWith("enable-")) {
            String providerName = key.substring(7);
            if (PROVIDER_NAMES.contains(providerName)) {
                if (sharedPreferences.getBoolean(key, true)) {
                    this.connectToProvider(providerName);
                } else {
                    this.disconnectFromProvider(providerName);
                }
            }
        }
    }

    /**
     * Generate an intent that can be used to start or stop the given provider
     *
     * @param name The name of the provider
     * @return Android intent for this provider
     */
    protected Intent providerName2Intent(String name) {
        // Build expected fully-qualified provider class name
        StringBuilder className = new StringBuilder(50);
        className.append(PROVIDER_PREFIX);
        className.append(Character.toUpperCase(name.charAt(0)));
        className.append(name.substring(1).toLowerCase());
        className.append("Provider");

        // Try to create reflection class instance for class name
        try {
            return new Intent(this.context, Class.forName(className.toString()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Require the data handler to be connected to the data provider with the given name
     *
     * @param name Data provider name (i.e.: `ContactsProvider` → `"contacts"`)
     */
    protected void connectToProvider(final String name) {
        // Do not continue if this provider has already been connected to
        if (this.providers.containsKey(name)) {
            return;
        }

        // Find provider class for the given service name
        Intent intent = this.providerName2Intent(name);
        if (intent == null) {
            return;
        }

        // Send "start service" command first so that the service can run independently
        // of the activity
        this.context.startService(intent);

        final ProviderEntry entry = new ProviderEntry();

        // Connect and bind to provider service
        this.context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                Provider.LocalBinder binder = (Provider.LocalBinder) service;
                IProvider provider = binder.getService();

                // Update provider info so that it contains something useful
                entry.provider = provider;
                entry.connection = this;

                if (provider.isLoaded()) {
                    handleProviderLoaded();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }
        }, Context.BIND_AUTO_CREATE);

        // Add empty provider object to list of providers
        this.providers.put(name, entry);
    }

    /**
     * Terminate any connection between the data handler and the data provider with the given name
     *
     * @param name Data provider name (i.e.: `AppProvider` → `"app"`)
     */
    protected void disconnectFromProvider(String name) {
        // Skip already disconnected services
        ProviderEntry entry = this.providers.get(name);
        if (entry == null) {
            return;
        }

        // Disconnect from provider service
        this.context.unbindService(entry.connection);

        // Stop provider service
        this.context.stopService(new Intent(this.context, entry.provider.getClass()));

        // Remove provider from list
        this.providers.remove(name);
    }

    /**
     * Called when some event occurred that makes us believe that all data providers
     * might be ready now
     */
    private void handleProviderLoaded() {
        if (this.providersReady) {
            return;
        }

        // Make sure that all providers are fully connected
        for (ProviderEntry entry : this.providers.values()) {
            if (entry.provider == null || !entry.provider.isLoaded()) {
                return;
            }
        }

        // Broadcast the fact that the new providers list is ready
        try {
            this.context.unregisterReceiver(this);
            Intent i = new Intent(MainActivity.FULL_LOAD_OVER);
            this.context.sendBroadcast(i);
        } catch (IllegalArgumentException e) {
            // Nothing
        }

        this.providersReady = true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.handleProviderLoaded();
    }

    /**
     * Reload all currently used data providers
     */
    public void reloadAll() {
        for (ProviderEntry entry : this.providers.values()) {
            if (entry.provider != null && entry.provider.isLoaded()) {
                entry.provider.reload();
            }
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
        query = query.toLowerCase().trim().replaceAll("<", "&lt;");

        currentQuery = query;

        // Have we ever made the same query and selected something ?
        List<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(
                context, query);
        HashMap<String, Integer> knownIds = new HashMap<>();
        for (ValuedHistoryRecord id : lastIdsForQuery) {
            knownIds.put(id.record, id.value);
        }

        // Ask all providers for data
        ArrayList<Pojo> allPojos = new ArrayList<>();

        for (ProviderEntry entry : this.providers.values()) {
            if (entry.provider != null) {
                // Retrieve results for query:
                List<Pojo> pojos = entry.provider.getResults(query);

                // Add results to list
                for (Pojo pojo : pojos) {
                    // Give a boost if item was previously selected for this query
                    if (knownIds.containsKey(pojo.id)) {
                        pojo.relevance += 25 * Math.min(5, knownIds.get(pojo.id));
                    }
                    allPojos.add(pojo);
                }
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
     * @param context        android context
     * @param itemCount      max number of items to retrieve, total number may be less (search or calls are not returned for instance)
     * @param smartHistory   Recency vs Frecency
     * @param itemsToExclude Items to exclude from history
     * @return pojos in recent history
     */
    public ArrayList<Pojo> getHistory(Context context, int itemCount, boolean smartHistory, ArrayList<Pojo> itemsToExclude) {
        // Pre-allocate array slots that are likely to be used based on the current maximum item
        // count
        ArrayList<Pojo> history = new ArrayList<>(Math.min(itemCount, 256));

        // Read history
        List<ValuedHistoryRecord> ids = DBHelper.getHistory(context, itemCount, smartHistory);

        // Find associated items
        for (int i = 0; i < ids.size(); i++) {
            // Ask all providers if they know this id
            Pojo pojo = getPojo(ids.get(i).record);
            if (pojo != null) {
                //Look if the pojo should get excluded
                boolean exclude = false;
                for (int j = 0; j < itemsToExclude.size(); j++) {
                    if (itemsToExclude.get(j).id.equals(pojo.id)) {
                        exclude = true;
                        break;
                    }
                }

                if (!exclude) {
                    history.add(pojo);
                }
            }
        }

        return history;
    }

    public int getHistoryLength() {
        return DBHelper.getHistoryLength(this.context);
    }

    public void addShortcut(ShortcutsPojo shortcut) {
        ShortcutRecord record = new ShortcutRecord();
        record.name = shortcut.name;
        record.iconResource = shortcut.resourceName;
        record.packageName = shortcut.packageName;
        record.intentUri = shortcut.intentUri;

        if (shortcut.icon != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            shortcut.icon.compress(CompressFormat.PNG, 100, baos);
            record.icon_blob = baos.toByteArray();
        }

        DBHelper.insertShortcut(this.context, record);

        if (this.getShortcutsProvider() != null) {
            this.getShortcutsProvider().reload();
        }

        Toast.makeText(context, R.string.shortcut_added, Toast.LENGTH_SHORT).show();
    }

    public void clearHistory()
    {
        DBHelper.clearHistory(this.context);
    }

    public void removeShortcut(ShortcutsPojo shortcut) {
        DBHelper.removeShortcut(this.context, shortcut.name);

        if (this.getShortcutsProvider() != null) {
            this.getShortcutsProvider().reload();
        }
    }

    public void removeShortcuts(String packageName) {
        DBHelper.removeShortcuts(this.context, packageName);

        if (this.getShortcutsProvider() != null) {
            this.getShortcutsProvider().reload();
        }
    }


    public void addToExcluded(String packageName, UserHandle user) {
		packageName = user.addUserSuffixToString(packageName, '#');
		
        String excludedAppList = PreferenceManager.getDefaultSharedPreferences(context).
                getString("excluded-apps-list", context.getPackageName() + ";");
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("excluded-apps-list", excludedAppList + packageName + ";").apply();
    }

    public void removeFromExcluded(String packageName, UserHandle user) {
		packageName = user.addUserSuffixToString(packageName, '#');
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        String excluded = prefs.getString("excluded-apps-list", context.getPackageName() + ";");
        prefs.edit().putString("excluded-apps-list", excluded.replaceAll(packageName + ";", "")).apply();
    }

	public void removeFromExcluded(UserHandle user) {
		// This is only intended for apps from foreign-profiles
		if(user.isCurrentUser()) {
			return;
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
		String[] excludedList = prefs.getString("excluded-apps-list", context.getPackageName() + ";").split(";");
		
		StringBuilder excluded = new StringBuilder();
		for(String excludedItem : excludedList) {
			if(!user.hasStringUserSuffix(excludedItem, '#')) {
				excluded.append(excludedItem + ";");
			}
		}
		
		prefs.edit().putString("excluded-apps-list", excluded.toString()).apply();
	}

    /**
     * Return all applications
     *
     * @return pojos for all applications
     */
    public ArrayList<Pojo> getApplications() {
        return this.getAppProvider().getAllApps();
    }

    public ContactsProvider getContactsProvider() {
        ProviderEntry entry = this.providers.get("contacts");
        return (entry != null) ? ((ContactsProvider) entry.provider) : null;
    }

    public ShortcutsProvider getShortcutsProvider() {
        ProviderEntry entry = this.providers.get("shortcuts");
        return (entry != null) ? ((ShortcutsProvider) entry.provider) : null;
    }

    public AppProvider getAppProvider() {
        ProviderEntry entry = this.providers.get("app");
        return (entry != null) ? ((AppProvider) entry.provider) : null;
    }

    public SearchProvider getSearchProvider() {
        ProviderEntry entry = this.providers.get("search");
        return (entry != null) ? ((SearchProvider) entry.provider) : null;
    }

    /**
     * Return most used items.<br />
     * May return null if no items were ever selected (app first use)
     *
     * @param limit max number of items to retrieve. You may end with less items if favorites contains non existing items.
     * @return favorites' pojo
     */
    public ArrayList<Pojo> getFavorites(int limit) {
        ArrayList<Pojo> favorites = new ArrayList<>(limit);

        String favApps = PreferenceManager.getDefaultSharedPreferences(this.context).
                getString("favorite-apps-list", "");
        List<String> favAppsList = Arrays.asList(favApps.split(";"));

        // Find associated items
        for (int i = 0; i < favAppsList.size(); i++) {
            Pojo pojo = getPojo(favAppsList.get(i));
            if (pojo != null) {
                favorites.add(pojo);
            }
            if (favorites.size() >= limit) {
                break;
            }
        }

        return favorites;
    }

    public boolean addToFavorites(MainActivity context, String id) {

        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");

        // Check if we are already a fav icon
        if (favApps.contains(id + ";")) {
            //shouldn't happen
            return false;
        }

        List<String> favAppsList = Arrays.asList(favApps.split(";"));
        if (favAppsList.size() >= context.getFavIconsSize()) {
            favApps = favApps.substring(favApps.indexOf(";") + 1);
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("favorite-apps-list", favApps + id + ";").apply();

        context.displayFavorites();

        return true;
    }

    public boolean removeFromFavorites(MainActivity context, String id) {

        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");

        // Check if we are not already a fav icon
        if (!favApps.contains(id + ";")) {
            //shouldn't happen
            return false;
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("favorite-apps-list", favApps.replace(id + ";", "")).apply();

        context.displayFavorites();

        return true;
    }
    
	public void removeFromFavorites(UserHandle user) {
		// This is only intended for apps from foreign-profiles
		if(user.isCurrentUser()) {
			return;
		}
		
		String[] favAppList = PreferenceManager.getDefaultSharedPreferences(this.context)
		                                       .getString("favorite-apps-list", "").split(";");
		
		StringBuilder favApps = new StringBuilder();
		for(String favAppID : favAppList) {
			if(!favAppID.startsWith("app://") || !user.hasStringUserSuffix(favAppID, '/')) {
				favApps.append(favAppID + ";");
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(this.context).edit()
		                 .putString("favorite-apps-list", favApps.toString()).apply();
	}

    /**
     * Insert specified ID (probably a pojo.id) into history
     *
     * @param id pojo.id of item to record
     */
    public void addToHistory(String id) {
        boolean frozen = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("freeze-history", false);

        if (!frozen) {
            DBHelper.insertHistory(this.context, currentQuery, id);
        }
    }

    private Pojo getPojo(String id) {
        // Ask all providers if they know this id
        for (ProviderEntry entry : this.providers.values()) {
            if (entry.provider != null && entry.provider.mayFindById(id)) {
                return entry.provider.findById(id);
            }
        }

        return null;
    }

    protected static final class ProviderEntry {
        public IProvider provider = null;
        public ServiceConnection connection = null;
    }

    public TagsHandler getTagsHandler() {
        if (tagsHandler == null) {
            tagsHandler = new TagsHandler(context);
        }
        return tagsHandler;
    }

    public void resetTagsHandler() {
        tagsHandler = new TagsHandler(this.context);
    }
}
