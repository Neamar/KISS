package fr.neamar.kiss;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        prefs.registerOnSharedPreferenceChangeListener(this);

        int historyLength = KissApplication.getDataHandler(this).getHistoryLength(this);
        if (historyLength > 5) {
            findPreference("reset").setSummary(getString(R.string.reset_desc) + " (" + historyLength + " items)");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Reload the DataHandler since Providers preferences have changed
        KissApplication.resetDataHandler(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // We need to finish the Activity now, else the user may get back to the settings screen the next time he'll press home.
        finish();
    }
}
