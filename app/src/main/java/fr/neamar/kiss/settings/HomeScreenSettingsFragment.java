package fr.neamar.kiss.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;

import fr.neamar.kiss.R;

public class HomeScreenSettingsFragment extends BaseSettingsFragment {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_home, rootKey);
    }
}
