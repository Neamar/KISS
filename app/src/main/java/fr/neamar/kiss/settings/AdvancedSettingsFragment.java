package fr.neamar.kiss.settings;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import fr.neamar.kiss.R;

public class AdvancedSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey);

        // The switch mirrors the system setting (see onResume), which can only be changed from there
        SwitchPreference notifications = findPreference("enable-notifications");
        if (notifications != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notifications.setOnPreferenceChangeListener((pref, newValue) -> {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                if ((boolean) newValue) {
                    new AlertDialog.Builder(requireContext()).setMessage(R.string.notification_dialog)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(intent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                } else {
                    startActivity(intent);
                }
                return false;
            });
        }

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

    @Override
    public void onResume() {
        super.onResume();
        SwitchPreference notifications = findPreference("enable-notifications");
        if (notifications != null) {
            notifications.setChecked(NotificationManagerCompat.getEnabledListenerPackages(requireContext()).contains(requireContext().getPackageName()));
        }
    }
}
