package fr.neamar.kiss.settings;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.SettingsActivity;
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
    /**
     * Key of the preference to scroll to when opening the screen, e.g. from search
     */
    public static final String ARG_SCROLL_TO_KEY = "scroll-to-key";

    protected SharedPreferences prefs;

    // Kept as a field: SharedPreferences only keeps weak references to listeners
    private final SharedPreferences.OnSharedPreferenceChangeListener restartListener = this::requireRestartIfNeeded;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        String key = args == null ? null : args.getString(ARG_SCROLL_TO_KEY);
        if (key != null) {
            // Only once, not when coming back to this screen
            args.remove(ARG_SCROLL_TO_KEY);
            scrollToPreference(key);
        }
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != null) {
            removeUnavailablePreferences(preferenceScreen);
        }
        super.setPreferenceScreen(preferenceScreen);
    }

    private void removeUnavailablePreferences(PreferenceGroup group) {
        for (int i = group.getPreferenceCount() - 1; i >= 0; i--) {
            Preference preference = group.getPreference(i);
            if (!isAvailable(preference)) {
                group.removePreference(preference);
            } else if (preference instanceof PreferenceGroup) {
                removeUnavailablePreferences((PreferenceGroup) preference);
            }
        }
    }

    /**
     * Some settings only exist on some versions of Android.
     * Set in preferences XML with "minSdk" and "maxSdk" extras.
     */
    static boolean isAvailable(@NonNull Preference preference) {
        Bundle extras = preference.peekExtras();
        return extras == null
                || (Build.VERSION.SDK_INT >= extras.getInt("minSdk", 0)
                && Build.VERSION.SDK_INT <= extras.getInt("maxSdk", Integer.MAX_VALUE));
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(getPreferenceScreen().getTitle());
        getPrefs().registerOnSharedPreferenceChangeListener(this);
        getPrefs().registerOnSharedPreferenceChangeListener(restartListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPrefs().unregisterOnSharedPreferenceChangeListener(this);
        getPrefs().unregisterOnSharedPreferenceChangeListener(restartListener);
    }

    /**
     * Restart when a preference with a "restart" extra changes, see {@link SettingsActivity#requireRestart(boolean)}
     */
    private void requireRestartIfNeeded(SharedPreferences sharedPreferences, @Nullable String key) {
        Preference preference = key == null ? null : findPreference(key);
        Bundle extras = preference == null ? null : preference.peekExtras();
        String restart = extras == null ? null : extras.getString("restart");
        if (restart != null) {
            ((SettingsActivity) requireActivity()).requireRestart("settings".equals(restart));
        }
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
