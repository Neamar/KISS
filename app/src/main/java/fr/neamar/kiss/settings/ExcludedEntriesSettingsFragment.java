package fr.neamar.kiss.settings;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.R;
import fr.neamar.kiss.utils.ShortcutUtil;

public class ExcludedEntriesSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_excluded, rootKey);

        if (!ShortcutUtil.canDeviceShowShortcuts()) {
            removePreference("edit-excluded-app-shortcuts");
            removePreference("reset-excluded-app-shortcuts");
        }

        setOnDialogClosedListener("reset-excluded-apps", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet("excluded-apps", null).apply();
                getDataHandler().reloadApps();
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
            }
        });
        setOnDialogClosedListener("reset-excluded-from-history-apps", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet("excluded-apps-from-history", null).apply();
                getDataHandler().reloadApps(); // reload because it's cached in AppPojo#excludedFromHistory
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
            }
        });
        setOnDialogClosedListener("reset-excluded-app-shortcuts", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet(DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS, null).apply();
                DataHandler dataHandler = getDataHandler();
                // Reload shortcuts to refresh the shortcuts shown in KISS
                dataHandler.reloadShortcuts();
                // Reload apps since the `AppPojo.isExcludedShortcuts` value also needs to be refreshed
                dataHandler.reloadApps();
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
            }
        });
    }
}
