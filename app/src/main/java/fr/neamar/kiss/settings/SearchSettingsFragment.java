package fr.neamar.kiss.settings;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.R;

public class SearchSettingsFragment extends BaseSettingsFragment {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_search, rootKey);

        // Display selected values as summary, there is no simple summary provider for multi select
        MultiSelectListPreference highlighting = findPreference("pref-result-highlighting");
        highlighting.setSummaryProvider((Preference.SummaryProvider<MultiSelectListPreference>) preference -> {
            List<CharSequence> selected = new ArrayList<>();
            CharSequence[] values = preference.getEntryValues();
            for (int i = 0; i < values.length; i++) {
                if (preference.getValues().contains(values[i].toString())) {
                    selected.add(preference.getEntries()[i]);
                }
            }
            return TextUtils.join(", ", selected);
        });
    }
}
