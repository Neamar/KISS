package fr.neamar.kiss.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.neamar.kiss.SettingsActivity;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.preference.LaunchPojoSelectPreference;

/**
 * Lists the settings matching a query, from all screens.
 * Only settings declared in preferences XML are found, not the ones added in code (e.g. each search provider).
 */
public class SettingsSearchFragment extends PreferenceFragmentCompat {
    private List<SearchableSetting> settings;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(requireContext()));
    }

    // inflateFromResource is restricted to androidx, but stable: it is what setPreferencesFromResource uses
    @SuppressLint("RestrictedApi")
    public void search(@NonNull String query) {
        if (settings == null) {
            settings = new ArrayList<>();
            for (Map.Entry<Integer, Class<? extends BaseSettingsFragment>> entry : SettingsFragment.PREFERENCES_XML.entrySet()) {
                PreferenceScreen screen = getPreferenceManager().inflateFromResource(requireContext(), entry.getKey(), null);
                addSettings(screen, screen.getTitle(), entry.getValue().getName());
            }
        }

        PreferenceScreen results = getPreferenceScreen();
        results.removeAll();
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return;
        }
        for (SearchableSetting setting : settings) {
            if (setting.text.contains(normalizedQuery)) {
                results.addPreference(setting.createResult());
            }
        }
    }

    private void addSettings(@NonNull PreferenceGroup group, @Nullable CharSequence screenTitle, @NonNull String screenFragment) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (preference.isVisible() && !TextUtils.isEmpty(preference.getTitle())) {
                settings.add(new SearchableSetting(preference, screenTitle, screenFragment));
            }
            if (preference instanceof PreferenceGroup) {
                addSettings((PreferenceGroup) preference, screenTitle, screenFragment);
            }
        }
    }

    @NonNull
    private static String normalize(@Nullable CharSequence text) {
        return StringNormalizer.normalizeWithResult(text, true).toString();
    }

    private class SearchableSetting {
        private final CharSequence title;
        private final CharSequence screenTitle;
        private final String fragment;
        @Nullable
        private final String key;
        // Normalized title, summary and choices
        private final String text;

        SearchableSetting(@NonNull Preference preference, @Nullable CharSequence screenTitle, @NonNull String screenFragment) {
            this.title = preference.getTitle();
            this.screenTitle = screenTitle;
            // Links to another screen open that screen
            this.fragment = preference.getFragment() != null ? preference.getFragment() : screenFragment;
            this.key = preference.getFragment() != null ? null : preference.getKey();

            StringBuilder text = new StringBuilder();
            text.append(normalize(title)).append('\n').append(normalize(preference.getSummary()));
            CharSequence[] entries = null;
            // Choices of LaunchPojoSelectPreference are all apps, matching most queries
            if (preference instanceof ListPreference && !(preference instanceof LaunchPojoSelectPreference)) {
                entries = ((ListPreference) preference).getEntries();
            } else if (preference instanceof MultiSelectListPreference) {
                entries = ((MultiSelectListPreference) preference).getEntries();
            }
            if (entries != null) {
                for (CharSequence entry : entries) {
                    text.append('\n').append(normalize(entry));
                }
            }
            this.text = text.toString();
        }

        Preference createResult() {
            Preference result = new Preference(requireContext());
            result.setPersistent(false);
            result.setIconSpaceReserved(false);
            result.setTitle(title);
            result.setSummary(screenTitle);
            result.setOnPreferenceClickListener(preference -> {
                ((SettingsActivity) requireActivity()).openSearchResult(fragment, key);
                return true;
            });
            return result;
        }
    }
}
