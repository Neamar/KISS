package fr.neamar.kiss.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;
import fr.neamar.kiss.preference.CustomSearchProvidersPreference;
import fr.neamar.kiss.preference.DefaultSearchProviderSelectPreference;

public class WebSearchSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_search_web, rootKey);

        if (getPrefs().getStringSet("selected-search-provider-names", null) == null) {
            // If null, it means this setting has never been accessed before
            // In this case, null != [] ([] happens when the user manually unselected every single option)
            // So, when null, we know it's the first time opening this setting and we can write the default value.
            // note: other preferences are initialized automatically in MainActivity.onCreate() from the preferences XML,
            // but this preference isn't defined in the XML so can't be initialized that easily.
            getPrefs().edit().putStringSet("selected-search-provider-names", SearchProvider.getSelectedSearchProviders(getPrefs())).apply();
        }

        setOnDialogClosedListener("reset-search-providers", (pref, positiveResult) -> {
            if (positiveResult) {
                // Search providers are reloaded by SettingsActivity
                getPrefs().edit().remove("available-search-providers").apply();
                Toast.makeText(getContext(), R.string.search_provider_reset_done_desc, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if ("available-search-providers".equals(key)) {
            for (String listKey : new String[]{"selected-search-provider-names", "deleting-search-providers-names"}) {
                CustomSearchProvidersPreference preference = findPreference(listKey);
                if (preference != null) {
                    preference.refresh();
                }
            }
            refreshDefaultSearchProvider();
        } else if ("selected-search-provider-names".equals(key)) {
            refreshDefaultSearchProvider();
        }
    }

    private void refreshDefaultSearchProvider() {
        DefaultSearchProviderSelectPreference preference = findPreference("default-search-provider");
        if (preference != null) {
            preference.refresh();
        }
    }
}
