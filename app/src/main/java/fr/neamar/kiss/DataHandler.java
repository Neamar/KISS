package fr.neamar.kiss;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import fr.neamar.kiss.broadcast.ProfileChangedHandler;
import fr.neamar.kiss.dataprovider.AppProvider;
import fr.neamar.kiss.dataprovider.ContactsProvider;
import fr.neamar.kiss.dataprovider.IProvider;
import fr.neamar.kiss.dataprovider.Provider;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.CalculatorProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.PhoneProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.SettingsProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.TagsProvider;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.HistoryMode;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.db.ValuedHistoryRecord;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.NameComparator;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.UserHandle;

public class DataHandler implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected static final String TAG = DataHandler.class.getSimpleName();

    /**
     * Package the providers reside in
     */
    private static final String PROVIDER_PREFIX = IProvider.class.getPackage().getName() + ".";
    /**
     * List all known providers
     */
    private static final List<String> PROVIDER_NAMES = Arrays.asList(
            "app", "contacts", "shortcuts"
    );

    /**
     * Key for a preference that holds a String set of apps which are excluded from showing shortcuts.
     * Each string in the set is the packageName of an app which may not show shortcuts.
     */
    public final static String PREF_KEY_EXCLUDED_SHORTCUT_APPS = "excluded-shortcut-apps";

    private TagsHandler tagsHandler;
    final private Context context;
    private String currentQuery;
    private final Map<String, ProviderEntry> providers = new HashMap<>();

    /**
     * Initialize all providers
     */
    public DataHandler(Context context) {
        // Make sure we are in the context of the main application
        // (otherwise we might receive an exception about broadcast listeners not being able
        //  to bind to services)
        this.context = context.getApplicationContext();

        Intent startLoad = new Intent(MainActivity.START_LOAD);
        this.context.sendBroadcast(startLoad);

        // Monitor changes for profiles
        ProfileChangedHandler profileChangedHandler = new ProfileChangedHandler();
        profileChangedHandler.register(this.context.getApplicationContext());

        // Monitor changes for service preferences (to automatically start and stop services)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Connect to initial providers
        // Those are the complex providers, that are defined as Android services
        // to survive even if the app's UI is killed
        // (this way, we don't need to reload the app list everytime for instance)
        for (String providerName : PROVIDER_NAMES) {
            if (prefs.getBoolean("enable-" + providerName, true)) {
                this.connectToProvider(providerName, 0);
            }
        }

        // Some basic providers are defined directly,
        // as we don't need the overhead of a service for them
        // Those providers don't expose a service connection,
        // and you can't bind / unbind to them dynamically.
        ProviderEntry calculatorEntry = new ProviderEntry();
        calculatorEntry.provider = new CalculatorProvider();
        this.providers.put("calculator", calculatorEntry);
        ProviderEntry phoneEntry = new ProviderEntry();
        phoneEntry.provider = new PhoneProvider(context);
        this.providers.put("phone", phoneEntry);
        ProviderEntry searchEntry = new ProviderEntry();
        searchEntry.provider = new SearchProvider(context);
        this.providers.put("search", searchEntry);
        ProviderEntry settingsEntry = new ProviderEntry();
        settingsEntry.provider = new SettingsProvider(context);
        this.providers.put("settings", settingsEntry);
        ProviderEntry tagsEntry = new ProviderEntry();
        tagsEntry.provider = new TagsProvider();
        this.providers.put("tags", tagsEntry);

        // Some basic providers already loaded! We need to fire the LOAD_OVER event.
        Intent loadOver = new Intent(MainActivity.LOAD_OVER);
        this.context.sendBroadcast(loadOver);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null && key.startsWith("enable-")) {
            String providerName = key.substring(7);
            if (PROVIDER_NAMES.contains(providerName)) {
                if (sharedPreferences.getBoolean(key, true)) {
                    this.connectToProvider(providerName, 0);
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
    private Intent providerName2Intent(String name) {
        // Build expected fully-qualified provider class name
        StringBuilder className = new StringBuilder(50);
        className.append(PROVIDER_PREFIX);
        className.append(Character.toUpperCase(name.charAt(0)));
        className.append(name.substring(1).toLowerCase(Locale.ROOT));
        className.append("Provider");

        // Try to create reflection class instance for class name
        try {
            return new Intent(this.context, Class.forName(className.toString()));
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to get intent for provider name: " + name, e);
            return null;
        }
    }

    /**
     * Require the data handler to be connected to the data provider with the given name
     *
     * @param name Data provider name (i.e.: `ContactsProvider` → `"contacts"`)
     */
    protected void connectToProvider(final String name, final int counter) {
        // Do not continue if this provider has already been connected to
        if (this.providers.containsKey(name)) {
            return;
        }

        Log.v(TAG, "Connecting to " + name);

        // Find provider class for the given service name
        final Intent intent = this.providerName2Intent(name);
        if (intent == null) {
            return;
        }

        try {
            // Send "start service" command first so that the service can run independently
            // of the activity
            this.context.startService(intent);
        } catch (IllegalStateException e) {
            // When KISS is the default launcher,
            // the system will try to start KISS in the background after a reboot
            // however at this point we're not allowed to start services, and an IllegalStateException will be thrown
            // We'll then add a broadcast receiver for the next time the user turns his screen on
            // (or passes the lockscreen) to retry at this point
            // https://github.com/Neamar/KISS/issues/1130
            // https://github.com/Neamar/KISS/issues/1154
            Log.w(TAG, "Unable to start service for " + name + ". KISS is probably not in the foreground. Service will automatically be started when KISS gets to the foreground.");

            if (counter > 20) {
                Log.e(TAG, "Already tried and failed twenty times to start service. Giving up.");
                return;
            }

            // Add a receiver to get notified next time the screen is on
            // or next time the users successfully dismisses his lock screen
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, Intent intent) {
                    // Is there a lockscreen still visible to the user?
                    // If yes, we can't start background services yet, so we'll need to wait until we get ACTION_USER_PRESENT
                    KeyguardManager myKM = ContextCompat.getSystemService(context, KeyguardManager.class);
                    boolean isPhoneLocked = myKM.inKeyguardRestrictedInputMode();
                    if (!isPhoneLocked) {
                        context.unregisterReceiver(this);
                        final Handler handler = new Handler();
                        // Even when all the stars are aligned,
                        // starting the service needs to be slightly delayed because the Intent is fired *before* the app is considered in the foreground.
                        // Each new release of Android manages to make the developer life harder.
                        // Can't wait for the next one.
                        handler.postDelayed(() -> {
                            Log.i(TAG, "Screen turned on or unlocked, retrying to start background services");
                            connectToProvider(name, counter + 1);
                        }, 10);
                    }
                }
            }, intentFilter);

            // Stop here for now, the Receiver will re-trigger the whole flow when services can be started.
            return;
        }

        // Add empty provider object to list of providers
        final ProviderEntry entry = new ProviderEntry();
        this.providers.put(name, entry);

        // Connect and bind to provider service
        this.context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                Provider<?>.LocalBinder binder = (Provider<?>.LocalBinder) service;

                // Update provider info so that it contains something useful
                entry.provider = binder.getService();
                entry.connection = this;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);
    }

    /**
     * Terminate any connection between the data handler and the data provider with the given name
     *
     * @param name Data provider name (i.e.: `AppProvider` → `"app"`)
     */
    private void disconnectFromProvider(String name) {
        // Remove provider from list
        ProviderEntry entry = this.providers.remove(name);

        // Skip already disconnected services
        if (entry == null) {
            return;
        }

        // Disconnect from provider service
        if (entry.connection != null) {
            this.context.unbindService(entry.connection);
        }

        // Stop provider service
        if (entry.provider != null) {
            this.context.stopService(new Intent(this.context, entry.provider.getClass()));
        }

        // Providers changed! We need to fire the LOAD_OVER event.
        Intent loadOver = new Intent(MainActivity.LOAD_OVER);
        this.context.sendBroadcast(loadOver);
    }

    public boolean isAllProvidersLoaded() {
        for (ProviderEntry entry : this.providers.values()) {
            if (entry.provider == null || !entry.provider.isLoaded()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get records for this query.
     *
     * @param query    query to run
     * @param searcher the searcher currently running
     */
    public void requestResults(String query, Searcher searcher) {
        currentQuery = query;
        for (ProviderEntry entry : this.providers.values()) {
            if (searcher.isCancelled())
                break;
            if (entry.provider == null)
                continue;
            // Retrieve results for query:
            entry.provider.requestResults(query, searcher);
        }
    }

    /**
     * Get records for this query.
     *
     * @param searcher the searcher currently running
     */
    public void requestAllRecords(Searcher searcher) {
        List<Pojo> collectedPojos = new ArrayList<>();
        for (ProviderEntry entry : this.providers.values()) {
            if (searcher.isCancelled())
                break;
            if (entry.provider == null)
                continue;

            List<? extends Pojo> pojos = entry.provider.getPojos();
            if (pojos != null) {
                collectedPojos.addAll(pojos);
            }
        }
        searcher.addResults(collectedPojos);
    }

    /**
     * Return previously selected items.<br />
     * May return null if no items were ever selected (app first use)<br />
     * May return an empty set if the providers are not done building records,
     * in this case it is probably a good idea to call this function 500ms after
     *
     * @param context            android context
     * @param itemCount          max number of items to retrieve, total number may be less (search or calls are not returned for instance)
     * @param itemsToExcludeById Items to exclude from history by their id
     * @return pojos in recent history
     */
    public List<Pojo> getHistory(Context context, int itemCount, Set<String> itemsToExcludeById) {
        // Pre-allocate array slots that are likely to be used based on the current maximum item
        // count
        List<Pojo> history = new ArrayList<>(Math.min(itemCount, 256));

        // Max sure that we get enough items, regardless of how many may be excluded
        int extendedItemCount = itemCount + itemsToExcludeById.size();

        // Read history
        HistoryMode historyMode = getHistoryMode();
        List<ValuedHistoryRecord> ids = DBHelper.getHistory(context, extendedItemCount, historyMode);

        // Find associated items
        int size = ids.size();
        for (int i = 0; i < ids.size(); i++) {
            // Ask all providers if they know this id
            Pojo pojo = getPojo(ids.get(i).record);

            if (pojo == null) {
                continue;
            }

            if (itemsToExcludeById.contains(pojo.id)) {
                continue;
            }

            if (historyMode == HistoryMode.ALPHABETICALLY) {
                pojo.relevance = 0;
            } else {
                pojo.relevance = size - i;
            }
            history.add(pojo);
        }

        if (historyMode == HistoryMode.ALPHABETICALLY) {
            Collections.sort(history, new NameComparator());
        }

        // return only needed items
        return history.subList(0, Math.min(itemCount, history.size()));
    }

    /**
     * Apply relevance from history to given pojos.
     *
     * @param pojos       which needs to have relevance set
     * @param historyMode
     */
    public void applyRelevanceFromHistory(List<? extends Pojo> pojos, HistoryMode historyMode) {
        if (HistoryMode.ALPHABETICALLY == historyMode) {
            // "alphabetically" is special case because relevance needs to be set for all pojos instead of these from history.
            // This is done by setting all relevance to zero which results in order by name from used comparator.
            for (Pojo pojo : pojos) {
                pojo.relevance = 0;
            }
        } else {
            // Get length of history, this is needed so there are no entries missed.
            // If only number of displayed elements is used, this will result in more entries to be sorted by name.
            int historyLength = getHistoryLength();

            Map<String, Integer> relevance = new HashMap<>();
            List<ValuedHistoryRecord> ids = DBHelper.getHistory(context, historyLength, historyMode);
            int size = ids.size();
            for (int i = 0; i < size; i++) {
                relevance.put(ids.get(i).record, size - i);
            }

            for (Pojo pojo : pojos) {
                Integer calculated = relevance.get(pojo.id);
                pojo.relevance = calculated != null ? calculated : 0;
            }
        }
    }

    /**
     * @return history mode from settings: Recency vs Frecency vs Frequency vs Adaptive vs Alphabetically
     */
    public HistoryMode getHistoryMode() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return HistoryMode.valueById(prefs.getString("history-mode", "recency"));
    }

    public int getHistoryLength() {
        return DBHelper.getHistoryLength(this.context);
    }

    @Nullable
    public Pojo getItemById(String id) {
        return getPojo(id);
    }

    public void clearHistory() {
        DBHelper.clearHistory(this.context);
    }

    /**
     * Remove shortcut for given {@link ShortcutPojo}
     * This is used for remove of shortcut from gui.
     *
     * @param shortcut shortcut to be removed
     */
    private void removeShortcut(ShortcutPojo shortcut) {
        boolean shortcutUpdated = removeShortcut(shortcut.id, shortcut.packageName, shortcut.intentUri);
        if (shortcutUpdated) {
            reloadShortcuts();
        }
    }

    /**
     * Pin shortcut for given {@link ShortcutPojo}
     * This is used for pinning dynamic shortcut.
     *
     * @param shortcut shortcut to be pinned
     * @return true, if shortcut was pinned
     */
    public boolean pinShortcut(ShortcutPojo shortcut) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LauncherApps launcherApps = ContextCompat.getSystemService(context, LauncherApps.class);
            if (shortcut.isOreoShortcut() &&
                    launcherApps.hasShortcutHostPermission() &&
                    !PackageManagerUtils.isPrivateProfile(launcherApps, shortcut.getUserHandle().getRealHandle())) {
                ShortcutInfo shortcutToPin = ShortcutUtil.getShortCut(context, shortcut.getUserHandle().getRealHandle(), shortcut.packageName, shortcut.getOreoId());
                if (shortcutToPin != null) {
                    List<ShortcutInfo> shortcutInfos = ShortcutUtil.getShortcuts(context, shortcut.packageName);
                    List<String> pinnedShortcutIds = shortcutInfos.stream()
                            .filter(ShortcutInfo::isPinned)
                            .filter(shortcutInfo -> shortcutInfo.getUserHandle().equals(shortcutToPin.getUserHandle()))
                            .map(ShortcutInfo::getId)
                            .collect(Collectors.toList());
                    pinnedShortcutIds.add(shortcutToPin.getId());

                    launcherApps.pinShortcuts(shortcut.packageName, pinnedShortcutIds, shortcutToPin.getUserHandle());
                    updateShortcut(shortcutToPin, false);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Unpin shortcut for given {@link ShortcutPojo}
     * This is used for unpinning shortcut.
     *
     * @param shortcut shortcut to be unpinned
     * @return true, if shortcut was unpinned
     */
    public boolean unpinShortcut(ShortcutPojo shortcut) {
        if (!shortcut.isOreoShortcut()) {
            removeShortcut(shortcut);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LauncherApps launcherApps = ContextCompat.getSystemService(context, LauncherApps.class);
            if (shortcut.isOreoShortcut() &&
                    launcherApps.hasShortcutHostPermission() &&
                    !PackageManagerUtils.isPrivateProfile(launcherApps, shortcut.getUserHandle().getRealHandle())) {
                ShortcutInfo shortcutToUnpin = ShortcutUtil.getShortCut(context, shortcut.getUserHandle().getRealHandle(), shortcut.packageName, shortcut.getOreoId());
                if (shortcutToUnpin != null) {
                    List<ShortcutInfo> shortcutInfos = ShortcutUtil.getShortcuts(context, shortcut.packageName);
                    List<String> pinnedShortcutIds = shortcutInfos.stream()
                            .filter(ShortcutInfo::isPinned)
                            .filter(shortcutInfo -> shortcutInfo.getUserHandle().equals(shortcutToUnpin.getUserHandle()))
                            .map(ShortcutInfo::getId)
                            .collect(Collectors.toList());
                    pinnedShortcutIds.remove(shortcutToUnpin.getId());

                    launcherApps.pinShortcuts(shortcut.packageName, pinnedShortcutIds, shortcutToUnpin.getUserHandle());
                    removeShortcut(shortcut.id, shortcut.packageName, shortcut.intentUri);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Update DB with given {@link ShortcutRecord}.
     *
     * @param shortcutInfo       the shortcut to update.
     * @param includePackageName include package name in shortcut name
     * @return true if update was successful
     */
    public boolean updateShortcut(ShortcutInfo shortcutInfo, boolean includePackageName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }

        // Create Pojo
        ShortcutRecord shortcutRecord = ShortcutUtil.createShortcutRecord(context, shortcutInfo, includePackageName);

        if (shortcutRecord == null) {
            return false;
        }

        if (shortcutInfo.isEnabled()) {
            Log.d(TAG, "Adding shortcut for " + shortcutRecord.packageName);
            return DBHelper.insertShortcut(this.context, shortcutRecord);
        } else {
            Log.d(TAG, "Removing shortcut for " + shortcutRecord.packageName);
            String id = ShortcutUtil.generateShortcutId(new UserHandle(context, shortcutInfo.getUserHandle()), shortcutRecord);
            return removeShortcut(id, shortcutRecord.packageName, shortcutRecord.intentUri);
        }
    }

    /**
     * Remove given shortcut from favorites and from DB
     *
     * @param id          KISS shortcut id, same as {@link ShortcutPojo#id}
     * @param packageName package name, same as {@link ShortcutPojo#packageName}
     * @param intentUri   intent to be called, same as {@link ShortcutPojo#intentUri}
     * @return true, if shortcut was removed
     */
    private boolean removeShortcut(String id, String packageName, String intentUri) {
        Log.d(TAG, "Removing shortcut for " + packageName);
        // Also remove shortcut from favorites
        removeFromFavorites(id);
        return DBHelper.removeShortcut(this.context, packageName, intentUri);
    }

    /**
     * Removes all stored shortcuts for given packageName.
     *
     * @param packageName
     */
    public void removeShortcuts(String packageName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Remove all shortcuts from favorites for given package name
        List<ShortcutRecord> shortcutsList = DBHelper.getShortcuts(context, packageName);
        for (ShortcutRecord shortcutRecord : shortcutsList) {
            UserManager manager = ContextCompat.getSystemService(context, UserManager.class);
            for (android.os.UserHandle user : manager.getUserProfiles()) {
                String id = ShortcutUtil.generateShortcutId(new UserHandle(context, user), shortcutRecord);
                removeFromFavorites(id);
            }
            String id = ShortcutUtil.generateShortcutId(null, shortcutRecord);
            removeFromFavorites(id);
        }

        DBHelper.removeShortcuts(this.context, packageName);

        reloadShortcuts();
    }

    @NonNull
    public Set<String> getExcludedFromHistory() {
        Set<String> excluded = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("excluded-apps-from-history", null);
        if (excluded != null) {
            return new HashSet<>(excluded);
        } else {
            Set<String> defaultExcluded = new HashSet<>(1);
            defaultExcluded.add("app://" + AppPojo.getComponentName(context.getPackageName(), MainActivity.class.getName(), UserHandle.OWNER));
            return defaultExcluded;
        }
    }

    @NonNull
    public Set<String> getExcluded() {
        Set<String> excluded = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("excluded-apps", null);
        if (excluded != null) {
            return new HashSet<>(excluded);
        } else {
            Set<String> defaultExcluded = new HashSet<>(1);
            defaultExcluded.add(AppPojo.getComponentName(context.getPackageName(), MainActivity.class.getName(), UserHandle.OWNER));
            return defaultExcluded;
        }
    }

    /**
     * Get ids of favorites that should be excluded from apps/shortcuts
     *
     * @return set of favorite ids
     */
    @NonNull
    public Set<String> getExcludedFavorites() {
        Set<String> excludedFavorites = new HashSet<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("exclude-favorites-apps", false)) {
            String favApps = prefs.getString("favorite-apps-list", "");
            excludedFavorites.addAll(Arrays.asList(favApps.split(";")));
        }
        return excludedFavorites;
    }

    @NonNull
    public Set<String> getExcludedShortcutApps() {
        Set<String> excluded = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(PREF_KEY_EXCLUDED_SHORTCUT_APPS, null);
        if (excluded != null) {
            return new HashSet<>(excluded);
        } else {
            return new HashSet<>();
        }
    }

    public void addToExcludedFromHistory(AppPojo app) {
        Set<String> excluded = getExcludedFromHistory();
        excluded.add(app.id);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps-from-history", excluded).apply();
        app.setExcludedFromHistory(true);
    }

    public void removeFromExcludedFromHistory(AppPojo app) {
        Set<String> excluded = getExcludedFromHistory();
        excluded.remove(app.id);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps-from-history", excluded).apply();
        app.setExcludedFromHistory(false);
    }

    public void addToExcluded(AppPojo app) {
        Set<String> excluded = getExcluded();
        excluded.add(app.getComponentName());
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", excluded).apply();
        app.setExcluded(true);

        // Ensure it's removed from favorites too
        removeFromFavorites(app.id);

        // Exclude shortcuts for this app
        removeShortcuts(app.packageName);
    }

    /**
     * Add app as an app which is not allowed to show shortcuts
     */
    public void addToExcludedShortcutApps(AppPojo app) {
        Set<String> excluded = getExcludedShortcutApps();
        excluded.add(app.packageName);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(PREF_KEY_EXCLUDED_SHORTCUT_APPS, excluded).apply();
        app.setExcludedShortcuts(true);
        reloadShortcuts();
    }

    public void removeFromExcluded(AppPojo app) {
        Set<String> excluded = getExcluded();
        excluded.remove(app.getComponentName());
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", excluded).apply();
        app.setExcluded(false);

        // Add shortcuts for this app
        reloadShortcuts();
    }

    public void removeFromExcluded(String packageName) {
        Set<String> excluded = getExcluded();
        Set<String> newExcluded = new HashSet<>(excluded.size());
        for (String excludedItem : excluded) {
            if (!excludedItem.contains(packageName + "/")) {
                newExcluded.add(excludedItem);
            }
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", newExcluded).apply();
    }

    public void removeFromExcluded(UserHandle user) {
        // This is only intended for apps from foreign-profiles
        if (user.isCurrentUser()) {
            return;
        }

        Set<String> excluded = getExcluded();
        Set<String> newExcluded = new HashSet<>(excluded.size());
        for (String excludedItem : excluded) {
            if (!user.hasStringUserSuffix(excludedItem, '#')) {
                newExcluded.add(excludedItem);
            }
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", newExcluded).apply();
    }

    /**
     * Remove app from the apps which are not allowed to show shortcuts -
     * that is to say, this app may show shortcuts
     */
    public void removeFromExcludedShortcutApps(AppPojo app) {
        Set<String> excluded = getExcludedShortcutApps();
        excluded.remove(app.packageName);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(PREF_KEY_EXCLUDED_SHORTCUT_APPS, excluded).apply();
        app.setExcludedShortcuts(false);
        reloadShortcuts();
    }

    /**
     * Return all applications (including excluded)
     *
     * @return pojos for all applications
     */
    @Nullable
    public List<AppPojo> getApplications() {
        AppProvider appProvider = getAppProvider();
        return appProvider != null ? appProvider.getAllApps() : null;
    }

    /**
     * Return all applications
     *
     * @return pojos for all applications
     */
    @Nullable
    public List<AppPojo> getApplicationsWithoutExcluded() {
        AppProvider appProvider = getAppProvider();
        return appProvider != null ? appProvider.getAllAppsWithoutExcluded() : null;
    }

    /**
     * Return all pinned shortcuts
     *
     * @return pojos for all pinned shortcuts
     */
    @Nullable
    public List<ShortcutPojo> getPinnedShortcuts() {
        ShortcutsProvider shortcutsProvider = getShortcutsProvider();
        return shortcutsProvider != null ? shortcutsProvider.getPinnedShortcuts() : null;
    }


    @Nullable
    public ContactsProvider getContactsProvider() {
        ProviderEntry entry = this.providers.get("contacts");
        return (entry != null) ? ((ContactsProvider) entry.provider) : null;
    }

    public void reloadContactsProvider() {
        ContactsProvider contactsProvider = getContactsProvider();
        if (contactsProvider != null) {
            contactsProvider.reload();
        }
    }

    @Nullable
    public ShortcutsProvider getShortcutsProvider() {
        ProviderEntry entry = this.providers.get("shortcuts");
        return (entry != null) ? ((ShortcutsProvider) entry.provider) : null;
    }

    public void reloadShortcuts() {
        ShortcutsProvider shortcutsProvider = getShortcutsProvider();
        if (shortcutsProvider != null) {
            shortcutsProvider.reload();
        }
    }

    @Nullable
    public AppProvider getAppProvider() {
        ProviderEntry entry = this.providers.get("app");
        return (entry != null) ? ((AppProvider) entry.provider) : null;
    }

    public void reloadApps() {
        AppProvider appProvider = getAppProvider();
        if (appProvider != null) {
            appProvider.reload();
        }
    }

    @Nullable
    public SearchProvider getSearchProvider() {
        ProviderEntry entry = this.providers.get("search");
        return (entry != null) ? ((SearchProvider) entry.provider) : null;
    }

    public void reloadSearchProvider() {
        SearchProvider searchProvider = getSearchProvider();
        if (searchProvider != null) {
            searchProvider.reload();
        }
    }

    /**
     * Return most used items.<br />
     * May return null if no items were ever selected (app first use)
     *
     * @return favorites' pojo
     */
    public List<Pojo> getFavorites() {

        String favApps = PreferenceManager.getDefaultSharedPreferences(this.context).
                getString("favorite-apps-list", "");
        List<String> favAppsList = Arrays.asList(favApps.split(";"));
        List<Pojo> favorites = new ArrayList<>(favAppsList.size());
        // Find associated items
        for (int i = 0; i < favAppsList.size(); i++) {
            Pojo pojo = getPojo(favAppsList.get(i));
            if (pojo != null) {
                favorites.add(pojo);
            }
        }

        return favorites;
    }

    /**
     * This method is used to set the specific position of an app in the fav array.
     *
     * @param context  The mainActivity context
     * @param id       the app you want to set the position of
     * @param position the new position of the fav
     */
    public void setFavoritePosition(Context context, String id, int position) {
        List<Pojo> currentFavorites = getFavorites();
        List<String> favAppsList = new ArrayList<>();

        for (Pojo pojo : currentFavorites) {
            favAppsList.add(pojo.getFavoriteId());
        }

        int currentPos = favAppsList.indexOf(id);
        if (currentPos == -1) {
            Log.e(TAG, "Couldn't find id in favAppsList");
            return;
        }
        // Clamp the position so we don't just extend past the end of the array.
        position = Math.min(position, favAppsList.size() - 1);

        favAppsList.remove(currentPos);
        favAppsList.add(position, id);

        String newFavList = TextUtils.join(";", favAppsList);

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("favorite-apps-list", newFavList + ";").apply();

        refreshFavorites();
    }

    public void addToFavorites(String id) {
        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");

        // Check if we are already a fav icon
        assert favApps != null;
        if (favApps.contains(id + ";")) {
            //shouldn't happen
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("favorite-apps-list", favApps + id + ";").apply();

        boolean excludedApps = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("exclude-favorites-apps", false);
        if (excludedApps) {
            reloadApps();
        }
        refreshFavorites();
    }

    public void removeFromFavorites(String id) {
        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");

        // Check if we are not already a fav icon
        assert favApps != null;
        if (!favApps.contains(id + ";")) {
            //shouldn't happen
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("favorite-apps-list", favApps.replace(id + ";", "")).apply();

        boolean excludedApps = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("exclude-favorites-apps", false);
        if (excludedApps) {
            reloadApps();
        }
        refreshFavorites();
    }

    public void removeFromFavorites(UserHandle user) {
        // This is only intended for apps from foreign-profiles
        if (user.isCurrentUser()) {
            return;
        }

        String[] favAppList = PreferenceManager.getDefaultSharedPreferences(this.context)
                .getString("favorite-apps-list", "").split(";");

        StringBuilder favApps = new StringBuilder();
        for (String favAppID : favAppList) {
            if (!favAppID.startsWith("app://") || !user.hasStringUserSuffix(favAppID, '/')) {
                favApps.append(favAppID);
                favApps.append(";");
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this.context).edit()
                .putString("favorite-apps-list", favApps.toString()).apply();

        boolean excludedApps = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("exclude-favorites-apps", false);
        if (excludedApps) {
            reloadApps();
        }
        refreshFavorites();
    }

    /**
     * Insert launching activity of package into history
     *
     * @param context     context
     * @param userHandle  user
     * @param packageName packageName
     */
    public void addPackageToHistory(Context context, UserHandle userHandle, String packageName) {
        ComponentName componentName = PackageManagerUtils.getLaunchingComponent(context, packageName, userHandle);
        if (componentName != null) {
            // add new package to history
            String pojoID = userHandle.addUserSuffixToString("app://" + componentName.getPackageName() + "/" + componentName.getClassName(), '/');
            addToHistory(pojoID);
        }
    }

    /**
     * Insert specified ID (probably a pojo.id) into history
     *
     * @param id pojo.id of item to record
     */
    public void addToHistory(String id) {
        if (TextUtils.isEmpty(id)) {
            return;
        }

        boolean frozen = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("freeze-history", false);

        Set<String> excludedFromHistory = getExcludedFromHistory();

        if (!frozen && !excludedFromHistory.contains(id)) {
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

    public TagsHandler getTagsHandler() {
        if (tagsHandler == null) {
            tagsHandler = new TagsHandler(context);
        }
        return tagsHandler;
    }

    @Nullable
    private TagsProvider getTagsProvider() {
        ProviderEntry entry = this.providers.get("tags");
        return (entry != null) ? ((TagsProvider) entry.provider) : null;
    }

    public void reloadTags() {
        TagsProvider tagsProvider = getTagsProvider();
        if (tagsProvider != null) {
            tagsProvider.reload();
        }
    }

    public void refreshFavorites() {
        Intent startLoad = new Intent(MainActivity.REFRESH_FAVORITES);
        context.sendBroadcast(startLoad);
    }

    public void resetTagsHandler() {
        tagsHandler = new TagsHandler(this.context);
    }

    public void renameApp(String componentName, String newName) {
        DBHelper.addCustomAppName(context, componentName, newName);
    }

    public void removeRenameApp(String componentName) {
        DBHelper.removeCustomAppName(context, componentName);
    }

    public long setCustomAppIcon(String componentName) {
        return DBHelper.addCustomAppIcon(context, componentName);
    }

    public long removeCustomAppIcon(String componentName) {
        return DBHelper.removeCustomAppIcon(context, componentName);
    }

    static final class ProviderEntry {
        public IProvider<?> provider = null;
        ServiceConnection connection = null;
    }
}
