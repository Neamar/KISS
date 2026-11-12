package fr.neamar.kiss.settings;

import static fr.neamar.kiss.dataprovider.ProviderName.*;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.R;
import fr.neamar.kiss.utils.ShortcutUtil;

public class ShortcutsSettingsFragment extends BaseSettingsFragment {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_search_shortcuts, rootKey);

        setOnDialogClosedListener("reset-excluded-app-shortcuts", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet(DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS, null).apply();
                getDataHandler().reload(APPS, SHORTCUTS);  // reload because it's cached in AppPojo#isExcludedShortcuts
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
            }
        });

        setOnDialogClosedListener("reset-shortcuts", (pref, positiveResult) -> {
            if (positiveResult && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Remove all shortcuts
                ShortcutUtil.removeAllShortcuts(getContext());
                // Build all shortcuts
                ShortcutUtil.addAllShortcuts(getContext());
                Toast.makeText(getContext(), R.string.regenerate_shortcuts_done, Toast.LENGTH_LONG).show();
            }
        });
    }
}
