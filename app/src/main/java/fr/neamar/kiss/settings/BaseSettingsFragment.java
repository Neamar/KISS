package fr.neamar.kiss.settings;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.preference.AddSearchProviderPreference;
import fr.neamar.kiss.preference.AddSearchProviderPreferenceDialogFragment;
import fr.neamar.kiss.preference.ColorPreference;
import fr.neamar.kiss.preference.ColorPreferenceDialogFragment;
import fr.neamar.kiss.preference.DialogShowingPreference;
import fr.neamar.kiss.preference.DialogShowingPreferenceDialogFragment;

/**
 * Base class for each settings screen.
 * Each screen has its own preferences XML, and handles the UI logic of the preferences it displays.
 * Side effects that must happen regardless of the screen (e.g. after importing settings) are in {@link fr.neamar.kiss.SettingsActivity}.
 */
public abstract class BaseSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String DIALOG_FRAGMENT_TAG = "fr.neamar.kiss.settings.DIALOG";

    protected SharedPreferences prefs;

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(getPreferenceScreen().getTitle());
        getPrefs().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPrefs().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        // Override to update the preferences displayed on this screen
    }

    protected SharedPreferences getPrefs() {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        }
        return prefs;
    }

    protected DataHandler getDataHandler() {
        return KissApplication.getApplication(requireContext()).getDataHandler();
    }

    /**
     * Remove a preference from this screen, if it exists
     */
    protected void removePreference(@NonNull String key) {
        Preference preference = findPreference(key);
        if (preference != null) {
            PreferenceGroup parent = preference.getParent();
            if (parent != null) {
                parent.removePreference(preference);
            }
        }
    }

    /**
     * Run some code when the dialog of a {@link DialogShowingPreference} on this screen is closed
     */
    protected void setOnDialogClosedListener(@NonNull String key, @NonNull DialogShowingPreference.OnDialogClosedListener listener) {
        DialogShowingPreference preference = findPreference(key);
        if (preference != null) {
            preference.setOnDialogClosedListener(listener);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        DialogFragment dialogFragment = null;
        if (preference instanceof DialogShowingPreference) {
            dialogFragment = DialogShowingPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof ColorPreference) {
            dialogFragment = ColorPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof AddSearchProviderPreference) {
            dialogFragment = AddSearchProviderPreferenceDialogFragment.newInstance(preference.getKey());
        }

        if (dialogFragment == null) {
            super.onDisplayPreferenceDialog(preference);
            return;
        }

        // check if dialog is already showing
        if (getParentFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }
        // PreferenceDialogFragmentCompat requires its target fragment to retrieve the preference
        dialogFragment.setTargetFragment(this, 0);
        dialogFragment.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
    }
}
