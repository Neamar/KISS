package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;

public class DefaultSearchProviderSelectPreference extends ListPreference {
    public DefaultSearchProviderSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setEntries(false);
    }

    public DefaultSearchProviderSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEntries(false);
    }

    public DefaultSearchProviderSelectPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEntries(false);
    }

    public DefaultSearchProviderSelectPreference(@NonNull Context context) {
        super(context);
        setEntries(false);
    }

    private void setEntries(boolean canChangeValue) {
        // Get selected providers to choose from
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Set<String> availableProviderNames = SearchProvider.getAvailableSearchProviders(getContext(), prefs).stream()
                .map(searchProviderName -> searchProviderName.split("\\|")[0])
                .collect(Collectors.toSet());
        List<String> selectedProviderNames = SearchProvider.getSelectedSearchProviders(prefs).stream()
                .filter(availableProviderNames::contains)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        String[] selectedProviderArray = new String[selectedProviderNames.size()];
        int pos = 0;
        //get names of search providers
        for (String searchProviderName : selectedProviderNames) {
            selectedProviderArray[pos++] = searchProviderName.split("\\|")[0];
        }

        setEntries(selectedProviderArray);
        setEntryValues(selectedProviderArray);

        String defaultValue = "Google"; // Google is standard on install
        if (!selectedProviderNames.isEmpty() && !selectedProviderNames.contains("Google")) {
            defaultValue = selectedProviderNames.get(0);
        }
        setDefaultValue(defaultValue);

        if (canChangeValue && !selectedProviderNames.contains(getValue())) {
            setValue(defaultValue);
        }

        setEnabled(!selectedProviderNames.isEmpty());
    }

    public void refresh() {
        setEntries(true);
    }
}