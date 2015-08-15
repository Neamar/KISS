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

        fixSummaries(prefs);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        // Reload the DataHandler since Providers preferences have changed
        KissApplication.resetDataHandler(this);
        fixSummaries(prefs);
    }

    @Override
    public void onPause() {
        super.onPause();
        // We need to finish the Activity now, else the user may get back to the settings screen the next time he'll press home.
        finish();
    }

    private void fixSummaries(SharedPreferences prefs) {
        int historyLength = KissApplication.getDataHandler(this).getHistoryLength(this);
        if (historyLength > 5) {
            findPreference("reset").setSummary(getString(R.string.reset_desc) + " (" + historyLength + " items)");
        }

        findPreference("auto-spellcheck").setSummary(
            String.format(getString(R.string.autospellcheck_desc),
                prefs.getInt("auto-spellcheck", 5)));
    }
}
