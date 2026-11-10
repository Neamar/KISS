package fr.neamar.kiss.settings;

import static fr.neamar.kiss.dataprovider.ProviderName.*;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.SwitchPreference;

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

        setOnDialogClosedListener("reset-excluded-apps", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet("excluded-apps", null).apply();
                getDataHandler().reload(APPS);
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
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
