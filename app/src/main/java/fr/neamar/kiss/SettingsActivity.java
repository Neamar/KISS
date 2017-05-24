package fr.neamar.kiss;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.broadcast.IncomingSmsHandler;
import fr.neamar.kiss.dataprovider.AppProvider;
import fr.neamar.kiss.dataprovider.SearchProvider;
import fr.neamar.kiss.utils.PackageManagerUtils;

public class SettingsActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Those settings require the app to restart
    final static private String requireRestartSettings = "enable-keyboard-workaround force-portrait primary-color";
    final static private String requireInstantRestart = "theme notification-bar-color";

    private boolean requireFullRestart = false;

    private SharedPreferences prefs;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "light");
        if (theme.contains("dark")) {
            setTheme(R.style.SettingThemeDark);
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

        ListPreference iconsPack = (ListPreference) findPreference("icons-pack");
        setListPreferenceIconsPacksData(iconsPack);

        fixSummaries();

        addExcludedAppSettings(prefs);

        addSearchProvidersSelector(prefs);

        UiTweaks.updateThemePrimaryColor(this);

        // Notification color can't be updated before Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            PreferenceScreen screen = (PreferenceScreen) findPreference("ui-holder");
            Preference pref = findPreference("notification-bar-color");
            screen.removePreference(pref);
        }
    }

    private void loadExcludedAppsToPreference(MultiSelectListPreference multiSelectList) {
        String excludedAppList = prefs.getString("excluded-apps-list", "").replace(this.getPackageName() + ";", "");
        String[] apps = excludedAppList.split(";");

        multiSelectList.setEntries(apps);
        multiSelectList.setEntryValues(apps);
        multiSelectList.setValues(new HashSet<String>(Arrays.asList(apps)));
    }

    private boolean hasExcludedApps(final SharedPreferences prefs) {
        String excludedAppList = prefs.getString("excluded-apps-list", "").replace(this.getPackageName() + ";", "");
        return !excludedAppList.isEmpty();
    }

    @SuppressWarnings("deprecation")
    private void addExcludedAppSettings(final SharedPreferences prefs) {
        final MultiSelectListPreference multiPreference = new MultiSelectListPreference(this);
        multiPreference.setTitle(R.string.ui_excluded_apps);
        multiPreference.setDialogTitle(R.string.ui_excluded_apps_dialog_title);
        multiPreference.setKey("excluded_apps_ui");
        multiPreference.setOrder(15);
        PreferenceGroup category = (PreferenceGroup) findPreference("history_category");
        category.addPreference(multiPreference);

        loadExcludedAppsToPreference(multiPreference);
        multiPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Set<String> appListToBeExcluded = (HashSet<String>) newValue;

                StringBuilder builder = new StringBuilder();
                for (String s : appListToBeExcluded) {
                    builder.append(s).append(";");
                }

                prefs.edit().putString("excluded-apps-list", builder.toString() + SettingsActivity.this.getPackageName() + ";").apply();
                loadExcludedAppsToPreference(multiPreference);
                if (!hasExcludedApps(prefs)) {
                    multiPreference.setDialogMessage(R.string.ui_excluded_apps_not_found);
                }
                KissApplication.getDataHandler(SettingsActivity.this).getAppProvider().reload();
                return false;
            }
        });
        if (!hasExcludedApps(prefs)) {
            multiPreference.setDialogMessage(R.string.ui_excluded_apps_not_found);
        }
    }

    private void addSearchProvidersSelector(SharedPreferences prefs) {
        MultiSelectListPreference multiPreference = new MultiSelectListPreference(this);
        String[] searchProviders = SearchProvider.getSearchProviders();
        String search_providers_title = this.getString(R.string.search_providers_title);
        multiPreference.setTitle(search_providers_title);
        multiPreference.setDialogTitle(search_providers_title);
        multiPreference.setKey("search-providers");
        multiPreference.setEntries(searchProviders);
        multiPreference.setEntryValues(searchProviders);
        multiPreference.setDefaultValue(new HashSet<>(Collections.singletonList("Google")));
        PreferenceGroup category = (PreferenceGroup) findPreference("providers");
        category.addPreference(multiPreference);
    }


    @Override
    public void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equalsIgnoreCase("icons-pack")) {
            KissApplication.getIconsHandler(this).loadIconsPack(sharedPreferences.getString(key, "default"));
        }

        if (key.equalsIgnoreCase("sort-apps")) {
            // Reload application list
            final AppProvider provider = KissApplication.getDataHandler(this).getAppProvider();
            if (provider != null) {
                provider.reload();
            }
        }

        if (requireRestartSettings.contains(key)) {
            requireFullRestart = true;
        }

        if (requireInstantRestart.contains(key)) {
            requireFullRestart = true;
            finish();
            return;
        }

        if ("enable-sms-history".equals(key) || "enable-phone-history".equals(key)) {
            if ("enable-sms-history".equals(key)) {
                PackageManagerUtils.enableComponent(this, IncomingSmsHandler.class, sharedPreferences.getBoolean(key, false));
            } else {
                PackageManagerUtils.enableComponent(this, IncomingCallHandler.class, sharedPreferences.getBoolean(key, false));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        if (requireFullRestart) {
            Toast.makeText(this, R.string.app_will_restart, Toast.LENGTH_SHORT).show();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("require-layout-update", true).apply();

            // Restart current activity to refresh view, since some
            // preferences
            // require using a new UI
            Intent intent = new Intent(this, getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
            return;
        }

    }

    @SuppressWarnings("deprecation")
    private void fixSummaries() {
        int historyLength = KissApplication.getDataHandler(this).getHistoryLength();
        if (historyLength > 5) {
            findPreference("reset").setSummary(String.format(getString(R.string.items_title), historyLength));
        }


        // Only display the "rate the app" preference if the user has been using KISS long enough to enjoy it ;)
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

    protected void setListPreferenceIconsPacksData(ListPreference lp) {
        IconsHandler iph = KissApplication.getIconsHandler(this);

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

}
