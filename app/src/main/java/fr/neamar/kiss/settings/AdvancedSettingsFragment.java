package fr.neamar.kiss.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import fr.neamar.kiss.R;

public class AdvancedSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            removePreference("enable-notifications");
        }

        setOnDialogClosedListener("enable-notifications", (pref, positiveResult) -> {
            if (positiveResult && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });
        Preference defaultLauncher = findPreference("default-launcher");
        if (defaultLauncher != null) {
            defaultLauncher.setOnPreferenceClickListener(preference -> {
                try {
                    startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    // some devices don't expose the home app picker directly
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
                return true;
            });
        }
        setOnDialogClosedListener("restart", (pref, positiveResult) -> {
            if (positiveResult) {
                System.exit(0);
            }
        });
    }
}
