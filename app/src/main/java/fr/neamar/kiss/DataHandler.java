package fr.neamar.kiss;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap.CompressFormat;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import fr.neamar.kiss.dataprovider.AliasProvider;
import fr.neamar.kiss.dataprovider.AppProvider;
import fr.neamar.kiss.dataprovider.ContactProvider;
import fr.neamar.kiss.dataprovider.IProvider;
import fr.neamar.kiss.dataprovider.PhoneProvider;
import fr.neamar.kiss.dataprovider.Provider;
import fr.neamar.kiss.dataprovider.SearchProvider;
import fr.neamar.kiss.dataprovider.SettingProvider;
import fr.neamar.kiss.dataprovider.ShortcutProvider;
import fr.neamar.kiss.dataprovider.ToggleProvider;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.db.ValuedHistoryRecord;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.pojo.ShortcutPojo;

public class DataHandler extends BroadcastReceiver {

    /**
     * List all known providers
     */
    private Map<IProvider, ServiceConnection> providers = new HashMap<>();
    private AppProvider appProvider           = null;
    private ContactProvider contactProvider   = null;
    private ShortcutProvider shortcutProvider = null;
    private String currentQuery;
    private final Context context;

    private Map<IProvider, ServiceConnection> providersNew;
    private int                               providersNewCount;


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

        this.reload();
    }

    /**
     * Reconnect to all provider services
     */
    public void reload() {
        this.providersNew      = new HashMap<>();
        this.providersNewCount = 0;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);

        // Initialize providers
        this.connect(new Intent(this.context, AppProvider.class));

        if (prefs.getBoolean("enable-contacts", true)) {
            this.connect(new Intent(this.context, ContactProvider.class));
        }

        if (prefs.getBoolean("enable-search", true)) {
            this.connect(new Intent(this.context, SearchProvider.class));
        }
        if (prefs.getBoolean("enable-phone", true)) {
            this.connect(new Intent(this.context, PhoneProvider.class));
        }
        if (prefs.getBoolean("enable-toggles", true)) {
            this.connect(new Intent(this.context, ToggleProvider.class));
        }
        if (prefs.getBoolean("enable-settings", true)) {
            this.connect(new Intent(this.context, SettingProvider.class));
        }
        if (prefs.getBoolean("enable-alias", true)) {
            this.connect(new Intent(this.context, AliasProvider.class));
        }
        if (prefs.getBoolean("enable-shortcuts", true)) {
            this.connect(new Intent(this.context, ShortcutProvider.class));
        }
    }

    private void connect(Intent intent) {
        // Count the number of providers that we expect to be initialized to make sure that
        // `handleProviderLoaded(Context)` will not emit a `FULL_LOAD_OVER` event before all
        // providers have been actually added to the `providers` list
        this.providersNewCount++;

        // Send "start service" command first so that the service can run independently
        // of the activity
        this.context.startService(intent);

        // Connect and bind to provider service
        this.context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                Provider.LocalBinder binder = (Provider.LocalBinder) service;
                IProvider provider = binder.getService();
                providersNew.put(provider, this);

                if (provider.isLoaded()) {
                    handleProviderLoaded();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {}
        }, Context.BIND_AUTO_CREATE);
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
        ArrayList<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(
                context, query);
        HashMap<String, Integer> knownIds = new HashMap<>();
        for (ValuedHistoryRecord id : lastIdsForQuery) {
            knownIds.put(id.record, id.value);
        }

        // Ask all providers for data
        ArrayList<Pojo> allPojos = new ArrayList<>();

        for (IProvider provider : providers.keySet()) {
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

    public int getHistoryLength() {
        return DBHelper.getHistoryLength(this.context);
    }
    
    public void addShortcut(ShortcutPojo shortcut) {
        ShortcutRecord record = new ShortcutRecord();
        record.name = shortcut.name;
        record.iconResource = shortcut.resourceName;
        record.packageName = shortcut.packageName;
        record.intentUri = shortcut.intentUri;
        
        if (shortcut.icon != null) {            
               ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
               shortcut.icon.compress(CompressFormat.PNG,100,baos);               
               record.icon_blob = baos.toByteArray();            
        }        
        
        DBHelper.insertShortcut(this.context, record);

        if(this.shortcutProvider != null) {
            this.shortcutProvider.reload();
        }
    }
    
    public void removeShortcut(ShortcutPojo shortcut) {
        DBHelper.removeShortcut(this.context, shortcut.name);

        if(this.shortcutProvider != null) {
            this.shortcutProvider.reload();
        }
    }

    public void removeShortcuts(String packageName) {
        DBHelper.removeShortcuts(this.context, packageName);

        if(this.shortcutProvider != null) {
            this.shortcutProvider.reload();
        }
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

    public ShortcutProvider getShortcutProvider() {
        return shortcutProvider;
    }

    public AppProvider getAppProvider() {
        return appProvider;
    }


    /**
     * Return most used items.<br />
     * May return null if no items were ever selected (app first use)
     *
     * @param context android context
     * @param limit   max number of items to retrieve. You may end with less items if favorites contains non existing items.
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
        if (favApps.contains(id + ";")) {
            //shouldn't happen
            return false;
        }

        List<String> favAppsList = Arrays.asList(favApps.split(";"));
        if (favAppsList.size() >= context.getFavIconsSize()) {
            favApps = favApps.substring(favApps.indexOf(";") + 1);
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("favorite-apps-list", favApps + id + ";").commit();

        context.retrieveFavorites();

        return true;
    }

    /**
     * Insert specified ID (probably a pojo.id) into history
     *
     * @param context android context
     * @param id      pojo.id of item to record
     */
    public void addToHistory(String id) {
        DBHelper.insertHistory(this.context, currentQuery, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.handleProviderLoaded();
    }

    private void handleProviderLoaded() {
        // Do not continue if not all providers are connected yet
        if(this.providersNewCount != this.providersNew.size()) {
            return;
        }

        // Make sure all providers have completely loaded
        for(IProvider provider : this.providersNew.keySet()) {
            if(!provider.isLoaded()) {
                return;
            }
        }

        // Obtain reference to old and new providers list
        final Map<IProvider, ServiceConnection> providersOld = this.providers;
        final Map<IProvider, ServiceConnection> providersNew = this.providersNew;

        // Store references to important services
        for(IProvider provider : this.providersNew.keySet()) {
            final String providerName = provider.getClass().getName();
            if(providerName.equals(AppProvider.class.getName())) {
                this.appProvider      = (AppProvider) provider;
            } else if(providerName.equals(ContactProvider.class.getName())) {
                this.contactProvider  = (ContactProvider) provider;
            } else if(providerName.equals(ShortcutProvider.class.getName())) {
                this.shortcutProvider = (ShortcutProvider) provider;
            }
        }

        // Switch to the new providers list
        this.providers = this.providersNew;

        // Broadcast the fact that the new providers list is ready
        try {
            this.context.unregisterReceiver(this);
            Intent i = new Intent(MainActivity.FULL_LOAD_OVER);
            this.context.sendBroadcast(i);
        } catch (IllegalArgumentException e) {
            // Nothing
        }

        // Stop all providers that were previously used but are not used anymore
        for(Map.Entry<IProvider, ServiceConnection> entryOld : providersOld.entrySet()) {
            final IProvider         providerOld   = entryOld.getKey();
            final ServiceConnection connectionOld = entryOld.getValue();

            boolean used = false;
            for(IProvider providerNew : providersNew.keySet()) {
                if(providerOld.getClass().getName() == providerNew.getClass().getName()) {
                    used = true;
                    break;
                }
            }

            if(!used) {
                // Disconnect from provider service
                this.context.unbindService(connectionOld);

                // Stop provider service
                this.context.stopService(new Intent(this.context, providerOld.getClass()));
            }
        }

        // Clean up
        this.providersNew      = null;
        this.providersNewCount = 0;
    }

    private Pojo getPojo(String id) {
        // Ask all providers if they know this id
        for (IProvider provider : providers.keySet()) {
            if (provider.mayFindById(id)) {
                return provider.findById(id);
            }
        }

        return null;
    }
}
