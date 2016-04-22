package fr.neamar.kiss;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.broadcast.IncomingSmsHandler;
import fr.neamar.kiss.dataprovider.AppProvider;
import fr.neamar.kiss.dataprovider.SearchProvider;

public class SettingsActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Those settings require the app to restart
    final static private String requireRestartSettings = "theme enable-spellcheck force-portrait";

    private SharedPreferences prefs;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "light");
        if (theme.contains("dark")) {
            setTheme(R.style.SettingThemeDark);
        }

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ListPreference iconsPack = (ListPreference) findPreference("icons-pack");
        setListPreferenceIconsPacksData(iconsPack);

        fixSummaries();

        addExcludedAppSettings(prefs);

        addSearchProvidersSelector(prefs);

        addSmsApplicationsSelector(prefs);
    }

    private void addSmsApplicationsSelector(SharedPreferences prefs) {
        ListPreference smsAppList = (ListPreference) findPreference("sms-apps");
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("text/plain");
        intent.setData(Uri.parse("sms:"));
        final PackageManager pm = getPackageManager();
        List<ResolveInfo> availableSmsApps = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        CharSequence[] entries = new CharSequence[availableSmsApps.size() + 1];
        CharSequence[] entryValues = new CharSequence[availableSmsApps.size() + 1];

        entries[0] = this.getString(R.string.sms_apps_default);
        entryValues[0] = "defaultSmsApp";
        int i = 1;
        for (ResolveInfo info : availableSmsApps) {
            String appName = (String) (info.activityInfo.applicationInfo != null ? pm.getApplicationLabel(info.activityInfo.applicationInfo) : "(unknown)");

            entries[i] = appName;
            entryValues[i++] =  info.activityInfo.packageName+"|"+info.activityInfo.name;
        }

        smsAppList.setEntries(entries);
        smsAppList.setEntryValues(entryValues);

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void loadExcludedAppsToPreference(MultiSelectListPreference multiSelectList) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            String excludedAppList = prefs.getString("excluded-apps-list", "").replace(this.getPackageName() + ";", "");
            String[] apps = excludedAppList.split(";");

            multiSelectList.setEntries(apps);
            multiSelectList.setEntryValues(apps);
            multiSelectList.setValues(new HashSet<String>(Arrays.asList(apps)));
        }
    }

    private boolean hasExcludedApps(final SharedPreferences prefs) {
        String excludedAppList = prefs.getString("excluded-apps-list", "").replace(this.getPackageName() + ";", "");
        if (excludedAppList.isEmpty()) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void addExcludedAppSettings(final SharedPreferences prefs) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {

            final MultiSelectListPreference multiPreference = new MultiSelectListPreference(this);
            multiPreference.setTitle(R.string.ui_excluded_apps);
            multiPreference.setDialogTitle(R.string.ui_excluded_apps_dialog_title);
            multiPreference.setKey("excluded_apps_ui");
            PreferenceCategory category = (PreferenceCategory) findPreference("history_category");
            category.addPreference(multiPreference);

            loadExcludedAppsToPreference(multiPreference);
            multiPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    HashSet<String> appListToBeExcluded = (HashSet<String>) newValue;

                    StringBuilder builder = new StringBuilder();
                    for (String s : appListToBeExcluded) {
                        builder.append(s + ";");
                    }

                    prefs.edit().putString("excluded-apps-list", builder.toString() + SettingsActivity.this.getPackageName() + ";").commit();
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
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void addSearchProvidersSelector(SharedPreferences prefs) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            MultiSelectListPreference multiPreference = new MultiSelectListPreference(this);
            String[] searchProviders = SearchProvider.getSearchProviders();
            multiPreference.setTitle("Select available search providers");
            multiPreference.setDialogTitle("Select the search providers you would like to enable");
            multiPreference.setKey("search-providers");
            multiPreference.setEntries(searchProviders);
            multiPreference.setEntryValues(searchProviders);
            multiPreference.setDefaultValue(new HashSet<>(Arrays.asList("Google")));
            PreferenceCategory category = (PreferenceCategory) findPreference("user_interface_category");
            category.addPreference(multiPreference);
        }
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

        if(key.equalsIgnoreCase("sort-apps")) {
            // Reload application list
            final AppProvider provider = KissApplication.getDataHandler(this).getAppProvider();
            if(provider != null) {
                provider.reload();
            }
        }

        if (requireRestartSettings.contains(key)) {
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

        if("enable-sms-history".equals(key) || "enable-phone-history".equals(key)) {
            ComponentName receiver;

            if("enable-sms-history".equals(key)) {
                receiver = new ComponentName(this, IncomingSmsHandler.class);
            }
            else {
                receiver = new ComponentName(this, IncomingCallHandler.class);
            }

            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(receiver,
                    sharedPreferences.getBoolean(key, false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        // We need to finish the Activity now, else the user may get back to the settings screen the next time he'll press home.
        finish();
    }

    @SuppressWarnings("deprecation")
    private void fixSummaries() {
        int historyLength = KissApplication.getDataHandler(this).getHistoryLength();
        if (historyLength > 5) {
            findPreference("reset").setSummary(String.format(getString(R.string.items_title), historyLength));
        }
    }

    protected void setListPreferenceIconsPacksData(ListPreference lp) {
        IconsHandler iph = KissApplication.getIconsHandler(this);

        CharSequence[] entries = new CharSequence[iph.getIconsPacks().size()+1];
        CharSequence[] entryValues = new CharSequence[iph.getIconsPacks().size()+1];

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
