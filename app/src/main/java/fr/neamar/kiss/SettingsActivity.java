package fr.neamar.kiss;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.util.Arrays;

import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.broadcast.IncomingSmsHandler;

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

        fixSummaries(prefs);
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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
            findPreference("reset").setSummary(getString(R.string.reset_desc) + " (" + historyLength + " items)");
        }
    }
}
