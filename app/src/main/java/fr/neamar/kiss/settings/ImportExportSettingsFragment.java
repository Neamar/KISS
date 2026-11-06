package fr.neamar.kiss.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;

import fr.neamar.kiss.R;
import fr.neamar.kiss.preference.ExportSettingsPreference;
import fr.neamar.kiss.preference.ImportSettingsPreference;

public class ImportExportSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_import_export, rootKey);

        setOnDialogClosedListener("export-settings", (pref, positiveResult) ->
                new ExportSettingsPreference().onDialogClosed(getContext(), positiveResult));
        setOnDialogClosedListener("import-settings", (pref, positiveResult) ->
                new ImportSettingsPreference().onDialogClosed(getContext(), positiveResult));
    }
}
