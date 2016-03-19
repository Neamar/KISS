package fr.neamar.kiss;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;

import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.broadcast.IncomingSmsHandler;
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

        fixSummaries(prefs);

        addSearchProvidersSelector(prefs);
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
            multiPreference.setDefaultValue(new HashSet<String>(Arrays.asList("Google")));
            PreferenceCategory category = (PreferenceCategory) findPreference("user_interface_category");
            category.addPreference(multiPreference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        
        if (key.equalsIgnoreCase("icons-pack")) {            
            KissApplication.getIconsHandler(this).loadIconsPack(sharedPreferences.getString(key, "default"));
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

        if("enable-sms".equals(key) || "enable-phone".equals(key)) {
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

    private void fixSummaries(SharedPreferences prefs) {
        int historyLength = KissApplication.getDataHandler(this).getHistoryLength();
        if (historyLength > 5) {
            findPreference("reset").setSummary(historyLength + " " + getString(R.string.items_title));
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
