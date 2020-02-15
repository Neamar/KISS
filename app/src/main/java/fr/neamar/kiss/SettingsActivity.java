package fr.neamar.kiss;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.TagsProvider;
import fr.neamar.kiss.forwarder.TagsMenu;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.preference.ExcludePreferenceScreen;
import fr.neamar.kiss.preference.PreferenceScreenHelper;
import fr.neamar.kiss.preference.SwitchPreference;
import fr.neamar.kiss.searcher.QuerySearcher;
import fr.neamar.kiss.utils.PackageManagerUtils;

@SuppressWarnings("FragmentInjection")
public class SettingsActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int PERMISSION_READ_PHONE_STATE = 1;

    // Those settings require the app to restart
    final static private String settingsRequiringRestart = "primary-color transparent-search transparent-favorites pref-rounded-list pref-rounded-bars pref-swap-kiss-button-with-menu pref-hide-circle history-hide enable-favorites-bar notification-bar-color black-notification-icons";
    final static private String settingsRequiringRestartForSettingsActivity = "theme force-portrait require-settings-update";
    private boolean requireFullRestart = false;

    private SharedPreferences prefs;

    /**
     * Get tags that should be in the favorites bar
     *
     * @param context to get the data handler with the actual favorites
     * @return what we find in DataHandler
     */
    @NonNull
    private static Set<String> getFavTags(Context context) {
        ArrayList<Pojo> favoritesPojo = KissApplication.getApplication(context).getDataHandler()
                .getFavorites();
        Set<String> set = new HashSet<>();
        for (Pojo pojo : favoritesPojo) {
            if (pojo instanceof TagDummyPojo)
                set.add(pojo.getName());
        }
        return set;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "light");
        assert theme != null;
        if (theme.equals("amoled-dark")) {
            setTheme(R.style.SettingThemeAmoledDark);
        } else if (theme.contains("dark")) {
            setTheme(R.style.SettingThemeDark);
        }

        if (prefs.contains("require-settings-update")) {
            // This flag will be used when the settings activity needs to restart,
            // but the value will be set to true
            // and the sharedpreferencesListener only triggers on value change
            // so we ensure it doesn't have a value before we display the settings
            prefs.edit().remove("require-settings-update").apply();
        }

        // Lock launcher into portrait mode
        // Do it here to make the transition as smooth as possible
        if (prefs.getBoolean("force-portrait", true)) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            removePreference("colors-section", "black-notification-icons");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            removePreference("history-hide-section", "pref-hide-navbar");
            removePreference("history-hide-section", "pref-hide-statusbar");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            removePreference("advanced", "enable-notifications");
        }

        final ListPreference iconsPack = (ListPreference) findPreference("icons-pack");
        iconsPack.setEnabled(false);

        Runnable runnable = () -> {
            SettingsActivity.this.fixSummaries();

            SettingsActivity.this.setListPreferenceIconsPacksData(iconsPack);
            SettingsActivity.this.runOnUiThread(() -> iconsPack.setEnabled(true));

            SettingsActivity.this.addExcludedAppSettings();
            SettingsActivity.this.addExcludedFromHistoryAppSettings();

            SettingsActivity.this.addCustomSearchProvidersPreferences(prefs);

            SettingsActivity.this.addHiddenTagsTogglesInformation(prefs);
            SettingsActivity.this.addTagsFavInformation();
        };

        if (savedInstanceState == null) {
            // Run asynchronously to open settings fast
            AsyncTask.execute(runnable);
        } else {
            // Run synchronously to ensure preferences can be restored from state
            runnable.run();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.help) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://help.kisslauncher.com"));
            startActivity(intent);
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        // If the user has clicked on a preference screen, set up the action bar
        if (preference instanceof PreferenceScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Dialog dialog = ((PreferenceScreen) preference).getDialog();
            Toolbar toolbar = PreferenceScreenHelper.findToolbar((PreferenceScreen) preference);

            if (toolbar != null) {
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        }

        return false;
    }

    private void removePreference(String parent, String category) {
        PreferenceGroup p = (PreferenceGroup) findPreference(parent);
        Preference c = findPreference(category);
        p.removePreference(c);
    }

    private void addExcludedAppSettings() {
        final DataHandler dataHandler = KissApplication.getApplication(this).getDataHandler();

        PreferenceScreen excludedAppsScreen = ExcludePreferenceScreen.getInstance(
                this,
                new ExcludePreferenceScreen.IsExcludedCallback() {
                    @Override
                    public boolean isExcluded(@NonNull AppPojo app) {
                        return app.isExcluded();
                    }
                },
                new ExcludePreferenceScreen.OnExcludedListener() {
                    @Override
                    public void onExcluded(final @NonNull AppPojo app) {
                        dataHandler.addToExcluded(app);
                    }

                    @Override
                    public void onIncluded(final @NonNull AppPojo app) {
                        dataHandler.removeFromExcluded(app);
                    }
                },
                R.string.ui_excluded_apps,
                R.string.ui_excluded_apps_dialog_title
        );

        PreferenceGroup category = (PreferenceGroup) findPreference("exclude_apps_category");
        category.addPreference(excludedAppsScreen);
    }

    private void addExcludedFromHistoryAppSettings() {
        final DataHandler dataHandler = KissApplication.getApplication(this).getDataHandler();

        PreferenceScreen excludedAppsScreen = ExcludePreferenceScreen.getInstance(
                this,
                new ExcludePreferenceScreen.IsExcludedCallback() {
                    @Override
                    public boolean isExcluded(@NonNull AppPojo app) {
                        return app.isExcludedFromHistory();
                    }
                },
                new ExcludePreferenceScreen.OnExcludedListener() {
                    @Override
                    public void onExcluded(final @NonNull AppPojo app) {
                        dataHandler.addToExcludedFromHistory(app);
                    }

                    @Override
                    public void onIncluded(final @NonNull AppPojo app) {
                        dataHandler.removeFromExcludedFromHistory(app);
                    }
                },
                R.string.ui_excluded_from_history_apps,
                R.string.ui_excluded_apps_dialog_title
        );

        PreferenceGroup category = (PreferenceGroup) findPreference("exclude_apps_category");
        category.addPreference(excludedAppsScreen);
    }

    private void addCustomSearchProvidersPreferences(SharedPreferences prefs) {
        if (prefs.getStringSet("selected-search-provider-names", null) == null) {
            // If null, it means this setting has never been accessed before
            // In this case, null != [] ([] happens when the user manually unselected every single option)
            // So, when null, we know it's the first time opening this setting and we can write the default value.
            // note: other preferences are initialized automatically in MainActivity.onCreate() from the preferences XML,
            // but this preference isn't defined in the XML so can't be initialized that easily.
            prefs.edit().putStringSet("selected-search-provider-names", new TreeSet<>(Collections.singletonList("Google"))).apply();
        }

        removeSearchProviderSelect();
        removeSearchProviderDelete();
        removeSearchProviderDefault();
        addCustomSearchProvidersSelect(prefs);
        addCustomSearchProvidersDelete(prefs);
        addDefaultSearchProvider(prefs);
    }

    private void removeSearchProviderSelect() {
        PreferenceGroup category = (PreferenceGroup) findPreference("providers");
        Preference pref = findPreference("selected-search-provider-names");
        if (pref != null) {
            category.removePreference(pref);
        }
    }

    private void removeSearchProviderDelete() {
        PreferenceGroup category = (PreferenceGroup) findPreference("providers");
        Preference pref = findPreference("deleting-search-providers-names");
        if (pref != null) {
            category.removePreference(pref);
        }
    }

    private void removeSearchProviderDefault() {
        PreferenceGroup category = (PreferenceGroup) findPreference("providers");
        Preference pref = findPreference("default-search-provider");
        if (pref != null) {
            category.removePreference(pref);
        }
    }

    @SuppressWarnings("StringSplitter")
    private void addCustomSearchProvidersSelect(SharedPreferences prefs) {
        MultiSelectListPreference multiPreference = new MultiSelectListPreference(this);
        //get stored search providers or default hard-coded values
        Set<String> availableSearchProviders = new TreeSet<>(prefs.getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(this)));
        String[] searchProvidersArray = new String[availableSearchProviders.size()];
        int pos = 0;
        //get names of search providers
        for (String searchProvider : availableSearchProviders) {
            searchProvidersArray[pos++] = searchProvider.split("\\|")[0];
        }
        String search_providers_title = this.getString(R.string.search_providers_title);
        multiPreference.setTitle(search_providers_title);
        multiPreference.setDialogTitle(search_providers_title);
        multiPreference.setKey("selected-search-provider-names");
        multiPreference.setEntries(searchProvidersArray);
        multiPreference.setEntryValues(searchProvidersArray);
        multiPreference.setOrder(10);

        PreferenceGroup category = (PreferenceGroup) findPreference("web-providers");
        category.addPreference(multiPreference);
    }

    @SuppressWarnings("StringSplitter")
    private void addCustomSearchProvidersDelete(final SharedPreferences prefs) {
        MultiSelectListPreference multiPreference = new MultiSelectListPreference(this);

        Set<String> availableSearchProviders = new TreeSet<>(prefs.getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(this)));
        String[] searchProvidersArray = new String[availableSearchProviders.size()];
        int pos = 0;
        //get names of search providers
        for (String searchProvider : availableSearchProviders) {
            searchProvidersArray[pos++] = searchProvider.split("\\|")[0];
        }
        multiPreference.setEnabled(availableSearchProviders.size() > 0);
        String search_providers_title = this.getString(R.string.search_providers_delete);
        multiPreference.setTitle(search_providers_title);
        multiPreference.setDialogTitle(search_providers_title);
        multiPreference.setKey("deleting-search-providers-names");
        multiPreference.setEntries(searchProvidersArray);
        multiPreference.setEntryValues(searchProvidersArray);
        multiPreference.setOrder(20);
        PreferenceGroup category = (PreferenceGroup) findPreference("web-providers");

        multiPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Set<String> searchProvidersToDelete = (Set<String>) newValue;
                Set<String> availableSearchProviders = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(SettingsActivity.this));

                Set<String> updatedProviders = new TreeSet<>(PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(SettingsActivity.this)));

                for (String searchProvider : availableSearchProviders) {
                    for (String providerToDelete : searchProvidersToDelete) {
                        if (searchProvider.startsWith(providerToDelete + "|")) {
                            updatedProviders.remove(searchProvider);
                        }
                    }
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putStringSet("available-search-providers", updatedProviders);
                editor.putStringSet("deleting-search-providers-names", updatedProviders);
                editor.apply();

                if (searchProvidersToDelete.size() > 0) {
                    Toast.makeText(SettingsActivity.this, R.string.search_provider_deleted, Toast.LENGTH_LONG).show();
                }

                // Reload search list
                final SearchProvider provider = KissApplication.getApplication(SettingsActivity.this).getDataHandler().getSearchProvider();
                if (provider != null) {
                    provider.reload();
                }
                return true;
            }
        });

        category.addPreference(multiPreference);
    }

    @SuppressWarnings("StringSplitter")
    private void addDefaultSearchProvider(final SharedPreferences prefs) {
        ListPreference standardPref = new ListPreference(this);

        // Get selected providers to choose from
        Set<String> selectedProviders = new TreeSet<>(prefs.getStringSet("selected-search-provider-names", new TreeSet<>(Collections.singletonList("Google"))));
        String[] selectedProviderArray = new String[selectedProviders.size()];
        int pos = 0;
        //get names of search providers
        for (String searchProvider : selectedProviders) {
            selectedProviderArray[pos++] = searchProvider.split("\\|")[0];
        }

        String searchProvidersTitle = this.getString(R.string.search_provider_default);
        standardPref.setTitle(searchProvidersTitle);
        standardPref.setDialogTitle(searchProvidersTitle);
        standardPref.setKey("default-search-provider");
        standardPref.setEntries(selectedProviderArray);
        standardPref.setEntryValues(selectedProviderArray);
        standardPref.setOrder(0);
        standardPref.setDefaultValue("Google"); // Google is standard on install

        PreferenceGroup category = (PreferenceGroup) findPreference("web-providers");
        category.addPreference(standardPref);
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase("available-search-providers")) {
            addCustomSearchProvidersPreferences(prefs);
        } else if (key.equalsIgnoreCase("selected-search-provider-names")) {
            final SearchProvider provider = KissApplication.getApplication(SettingsActivity.this).getDataHandler().getSearchProvider();
            if (provider != null) {
                provider.reload();
            }
            removeSearchProviderDefault(); // in order to refresh default search engine choices
            addDefaultSearchProvider(prefs);
        } else if (key.equalsIgnoreCase("icons-pack")) {
            KissApplication.getApplication(this).getIconsHandler().loadIconsPack(sharedPreferences.getString(key, "default"));
        } else if (key.equalsIgnoreCase("enable-phone-history")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_PHONE_STATE},
                        SettingsActivity.PERMISSION_READ_PHONE_STATE);
                return;
            }
            PackageManagerUtils.enableComponent(this, IncomingCallHandler.class, sharedPreferences.getBoolean(key, false));
        } else if (key.equalsIgnoreCase("primary-color")) {
            UIColors.clearPrimaryColorCache(this);
        } else if (key.equalsIgnoreCase("number-of-display-elements")) {
            QuerySearcher.clearMaxResultCountCache();
        } else if (key.equalsIgnoreCase("default-search-provider")) {
            final SearchProvider provider = KissApplication.getApplication(SettingsActivity.this).getDataHandler().getSearchProvider();
            if (provider != null) {
                provider.reload();
            }
        } else if ("pref-fav-tags-list".equals(key)) {
            // after we edit the fav tags list update DataHandler
            Set<String> favTags = sharedPreferences.getStringSet(key, Collections.<String>emptySet());
            DataHandler dh = KissApplication.getApplication(this).getDataHandler();
            ArrayList<Pojo> favoritesPojo = dh.getFavorites();
            for (Pojo pojo : favoritesPojo)
                if (pojo instanceof TagDummyPojo && !favTags.contains(pojo.getName()))
                    dh.removeFromFavorites(pojo.id);
            for (String tagName : favTags)
                dh.addToFavorites(TagsProvider.generateUniqueId(tagName));
        }

        if (settingsRequiringRestart.contains(key) || settingsRequiringRestartForSettingsActivity.contains(key)) {
            requireFullRestart = true;

            if (settingsRequiringRestartForSettingsActivity.contains(key)) {
                // Kill this activity too, and restart
                recreate();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        // Some settings require a full UI refresh,
        // Flag this, so that MainActivity get the information onResume().
        if (requireFullRestart) {
            prefs.edit().putBoolean("require-layout-update", true).apply();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return;
        }

        if (requestCode == PERMISSION_READ_PHONE_STATE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PackageManagerUtils.enableComponent(this, IncomingCallHandler.class, prefs.getBoolean("enable-phone-history", false));
            } else {
                // You don't want to give us permission, that's fine. Revert the toggle.
                SwitchPreference p = (SwitchPreference) findPreference("enable-phone-history");
                p.setChecked(false);
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fixSummaries() {
        int historyLength = KissApplication.getApplication(this).getDataHandler().getHistoryLength();
        if (historyLength > 5) {
            findPreference("reset").setSummary(String.format(getString(R.string.items_title), historyLength));
        }

        // Only display "rate the app" preference if the user has been using KISS long enough to enjoy it ;)
        Preference rateApp = findPreference("rate-app");
        if (historyLength < 300) {
            getPreferenceScreen().removePreference(rateApp);
        } else {
            rateApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=" + getApplicationContext().getPackageName()));
                    startActivity(intent);

                    return true;
                }
            });
        }
    }

    private void setListPreferenceIconsPacksData(ListPreference lp) {
        IconsHandler iph = KissApplication.getApplication(this).getIconsHandler();

        CharSequence[] entries = new CharSequence[iph.getIconsPacks().size() + 1];
        CharSequence[] entryValues = new CharSequence[iph.getIconsPacks().size() + 1];

        int i = 0;
        entries[0] = this.getString(R.string.icons_pack_default_name);
        entryValues[0] = "default";
        for (String packageIconsPack : iph.getIconsPacks().keySet()) {
            entries[++i] = iph.getIconsPacks().get(packageIconsPack);
            entryValues[i] = packageIconsPack;
        }

        lp.setEntries(entries);
        lp.setDefaultValue("default");
        lp.setEntryValues(entryValues);
    }

    private void addHiddenTagsTogglesInformation(SharedPreferences prefs) {
        Set<String> menuTags = TagsMenu.getPrefTags(prefs, getApplicationContext());
        MultiSelectListPreference selectListPreference = (MultiSelectListPreference) findPreference("pref-toggle-tags-list");
        Set<String> tagsSet = KissApplication.getApplication(this)
                .getDataHandler()
                .getTagsHandler()
                .getAllTagsAsSet();

        // append tags that are available to toggle now
        tagsSet.addAll(menuTags);

        String[] tagArray = tagsSet.toArray(new String[0]);
        Arrays.sort(tagArray);
        selectListPreference.setEntries(tagArray);
        selectListPreference.setEntryValues(tagArray);
        selectListPreference.setValues(menuTags);
    }

    private void addTagsFavInformation() {
        Set<String> favTags = getFavTags(getApplicationContext());
        MultiSelectListPreference selectListPreference = (MultiSelectListPreference) findPreference("pref-fav-tags-list");

        Set<String> tagsSet = KissApplication.getApplication(this)
                .getDataHandler()
                .getTagsHandler()
                .getAllTagsAsSet();

        // make sure we can toggle off the tags that are in the favs now
        tagsSet.addAll(favTags);

        String[] tagArray = tagsSet.toArray(new String[0]);
        Arrays.sort(tagArray);
        selectListPreference.setEntries(tagArray);
        selectListPreference.setEntryValues(tagArray);
        selectListPreference.setValues(favTags);
    }
}
