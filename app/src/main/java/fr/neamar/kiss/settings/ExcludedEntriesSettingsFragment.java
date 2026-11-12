package fr.neamar.kiss.settings;

import static fr.neamar.kiss.dataprovider.ProviderName.*;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.R;

public class ExcludedEntriesSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_excluded, rootKey);

        setOnDialogClosedListener("reset-excluded-apps", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet("excluded-apps", null).apply();
                getDataHandler().reload(APPS);
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
            }
        });
        setOnDialogClosedListener("reset-excluded-from-history-apps", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet("excluded-apps-from-history", null).apply();
                getDataHandler().reload(APPS); // reload because it's cached in AppPojo#excludedFromHistory
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
            }
        });
        setOnDialogClosedListener("reset-excluded-app-shortcuts", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet(DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS, null).apply();
                getDataHandler().reload(APPS, SHORTCUTS);  // reload because it's cached in AppPojo#isExcludedShortcuts
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
            }
        });
    }
}
