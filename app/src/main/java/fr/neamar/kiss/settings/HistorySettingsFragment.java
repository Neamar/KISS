package fr.neamar.kiss.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import fr.neamar.kiss.R;

public class HistorySettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_history, rootKey);

        int historyLength = getDataHandler().getHistoryLength();
        if (historyLength > 5) {
            Preference resetHistory = findPreference("reset-history");
            if (resetHistory != null) {
                resetHistory.setSummary(getString(R.string.items_title, historyLength));
            }
        }

        setOnDialogClosedListener("reset-history", (pref, positiveResult) -> {
            if (positiveResult) {
                getDataHandler().clearHistory();
                pref.setSummary(requireContext().getString(R.string.history_erased));
                Toast.makeText(getContext(), R.string.history_erased, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if ("enable-notification-history".equals(key)) {
            boolean enabled = sharedPreferences.getBoolean(key, false);
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        }
    }
}
