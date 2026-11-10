package fr.neamar.kiss.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import fr.neamar.kiss.R;
import fr.neamar.kiss.preference.LaunchPojoSelectPreference;

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
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (PREF_LISTS_WITH_DEPENDENCY.contains(key)) {
            updateItemToRun(key);
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
