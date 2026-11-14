package fr.neamar.kiss.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.Arrays;
import java.util.List;

import fr.neamar.kiss.R;
import fr.neamar.kiss.preference.LaunchPojoSelectPreference;
import fr.neamar.kiss.preference.TipPreference;

public class GesturesSettingsFragment extends BaseSettingsFragment {
    private static final List<String> PREF_LISTS_WITH_DEPENDENCY = Arrays.asList(
            "gesture-up", "gesture-down",
            "gesture-left", "gesture-right",
            "gesture-long-press"
    );

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_gestures, rootKey);

        for (String key : PREF_LISTS_WITH_DEPENDENCY) {
            updateItemToRun(key);
        }
        updateGesturesEnabled();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (PREF_LISTS_WITH_DEPENDENCY.contains(key)) {
            updateItemToRun(key);
        } else if ("history-hide".equals(key)) {
            updateGesturesEnabled();
        }
    }

    /**
     * Gestures need the empty area of the minimalistic UI, so disable everything (except the tip) without it
     */
    private void updateGesturesEnabled() {
        boolean enabled = getPrefs().getBoolean("history-hide", false);
        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference preference = screen.getPreference(i);
            if (!(preference instanceof TipPreference)) {
                preference.setEnabled(enabled);
            }
        }
    }

    /**
     * Only display the item to launch when the gesture is set to launch an item
     */
    private void updateItemToRun(String key) {
        LaunchPojoSelectPreference preference = findPreference(key + "-launch-id");
        if (preference != null) {
            String value = getPrefs().getString(key, null);
            boolean isLaunchEnabled = "launch-pojo".equals(value);
            preference.setEnabled(isLaunchEnabled);
            preference.setVisible(isLaunchEnabled);
        }
    }
}
