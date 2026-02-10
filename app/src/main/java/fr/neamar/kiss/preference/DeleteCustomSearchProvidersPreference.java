package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import java.util.Set;

import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;

public class DeleteCustomSearchProvidersPreference extends CustomSearchProvidersPreference implements Preference.OnPreferenceChangeListener {
    public DeleteCustomSearchProvidersPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DeleteCustomSearchProvidersPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DeleteCustomSearchProvidersPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DeleteCustomSearchProvidersPreference(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void setEntries() {
        setPersistent(false);
        super.setEntries();
        setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (newValue instanceof Set) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            @SuppressWarnings("unchecked")
            Set<String> searchProvidersToDelete = (Set<String>) newValue;

            Set<String> availableSearchProviders = SearchProvider.getAvailableSearchProviders(getContext(), prefs);
            Set<String> updatedProviders = SearchProvider.getAvailableSearchProviders(getContext(), prefs);

            for (String searchProvider : availableSearchProviders) {
                for (String providerToDelete : searchProvidersToDelete) {
                    if (searchProvider.startsWith(providerToDelete + "|")) {
                        updatedProviders.remove(searchProvider);
                    }
                }
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet("available-search-providers", updatedProviders);
            editor.apply();

            if (!searchProvidersToDelete.isEmpty()) {
                Toast.makeText(getContext(), R.string.search_provider_deleted, Toast.LENGTH_LONG).show();
            }
        }

        return true;
    }
}
