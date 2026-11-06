package fr.neamar.kiss.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import fr.neamar.kiss.R;

/**
 * Root settings screen, linking to all the other screens
 */
public class SettingsFragment extends BaseSettingsFragment {
    /**
     * All preference XMLs, to initialize default values: they are not read when navigating between screens.
     * SettingsFragmentTest checks that no XML is missing.
     */
    static final int[] PREFERENCES_XML = {
            R.xml.preferences,
            R.xml.preferences_history,
            R.xml.preferences_favorites,
            R.xml.preferences_ui,
            R.xml.preferences_ui_theme,
            R.xml.preferences_ux,
            R.xml.preferences_ux_tags_menu,
            R.xml.preferences_ux_wallpaper,
            R.xml.preferences_ux_contact_actions,
            R.xml.preferences_search,
            R.xml.preferences_excluded,
            R.xml.preferences_import_export,
            R.xml.preferences_advanced,
    };

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Only display "rate the app" preference if the user has been using KISS long enough to enjoy it ;)
        Preference rateApp = findPreference("rate-app");
        if (rateApp != null) {
            if (getDataHandler().getHistoryLength() < 300) {
                getPreferenceScreen().removePreference(rateApp);
            } else {
                rateApp.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=" + requireContext().getApplicationContext().getPackageName()));
                    startActivity(intent);

                    return true;
                });
            }
        }
    }

    /**
     * Write the default value of all settings in the default shared preferences.
     * Same as {@link PreferenceManager#setDefaultValues(Context, int, boolean)}, for all preference XMLs.
     */
    public static void setDefaultValues(@NonNull Context context, boolean readAgain) {
        if (!readAgain && context.getSharedPreferences(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, Context.MODE_PRIVATE)
                .getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            return;
        }
        for (int xml : PREFERENCES_XML) {
            PreferenceManager.setDefaultValues(context, xml, true);
        }
    }

    /**
     * Write the default value of all settings in the given shared preferences.
     */
    public static void setDefaultValues(@NonNull Context context, @NonNull String sharedPreferencesName) {
        for (int xml : PREFERENCES_XML) {
            PreferenceManager.setDefaultValues(context, sharedPreferencesName, Context.MODE_PRIVATE, xml, true);
        }
    }
}
