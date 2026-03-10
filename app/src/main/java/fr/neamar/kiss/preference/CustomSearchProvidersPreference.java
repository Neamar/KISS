package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceManager;

import java.util.Set;

import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;

public abstract class CustomSearchProvidersPreference extends MultiSelectListPreference {
    public CustomSearchProvidersPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setEntries();
    }

    public CustomSearchProvidersPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEntries();
    }

    public CustomSearchProvidersPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEntries();
    }

    public CustomSearchProvidersPreference(@NonNull Context context) {
        super(context);
        setEntries();
    }

    protected void setEntries() {
        //get stored search providers or default hard-coded values
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Set<String> availableSearchProviders = SearchProvider.getAvailableSearchProviders(getContext(), prefs);
        String[] searchProvidersArray = new String[availableSearchProviders.size()];
        int pos = 0;
        //get names of search providers
        for (String searchProvider : availableSearchProviders) {
            searchProvidersArray[pos++] = searchProvider.split("\\|")[0];
        }
        setEntries(searchProvidersArray);
        setEntryValues(searchProvidersArray);

        setEnabled(!availableSearchProviders.isEmpty());
    }


}
