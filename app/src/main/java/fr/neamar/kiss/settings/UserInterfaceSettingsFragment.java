package fr.neamar.kiss.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import fr.neamar.kiss.R;

public class UserInterfaceSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_ui, rootKey);

        updateNightMode();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if ("theme".equals(key)) {
            updateNightMode();
        }
    }

    /**
     * Night mode can't be changed with the AMOLED theme, which is always dark
     */
    private void updateNightMode() {
        boolean isAmoledTheme = "amoled-dark".equals(getPrefs().getString("theme", "transparent"));

        Preference darkMode = findPreference("night-mode");
        if (darkMode != null) {
            darkMode.setEnabled(!isAmoledTheme);
            darkMode.setVisible(!isAmoledTheme);
        }
    }
}
