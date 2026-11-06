package fr.neamar.kiss.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;

import fr.neamar.kiss.R;

public class ThemeCustomisationSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_ui_theme, rootKey);
    }
}
