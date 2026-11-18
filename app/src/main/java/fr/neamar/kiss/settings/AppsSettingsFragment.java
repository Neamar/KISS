package fr.neamar.kiss.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.SwitchPreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import fr.neamar.kiss.R;

public class AppsSettingsFragment extends BaseSettingsFragment {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_search_apps, rootKey);

        // The switch mirrors the system setting (see onResume), which can only be changed from there
        SwitchPreference notifications = findPreference("enable-notifications");
        if (notifications != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notifications.setOnPreferenceChangeListener((pref, newValue) -> {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                if ((boolean) newValue) {
                    new MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.notification_dialog)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(intent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                } else {
                    startActivity(intent);
                }
                return false;
            });
        }
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
