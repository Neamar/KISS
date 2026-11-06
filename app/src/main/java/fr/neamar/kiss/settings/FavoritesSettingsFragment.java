package fr.neamar.kiss.settings;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import fr.neamar.kiss.R;

public class FavoritesSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_favorites, rootKey);

        setOnDialogClosedListener("reset-favorites", (pref, positiveResult) -> {
            if (positiveResult) {
                getDataHandler().resetFavorites();
                Toast.makeText(getContext(), R.string.favorites_erased, Toast.LENGTH_LONG).show();
            }
        });
    }
}
