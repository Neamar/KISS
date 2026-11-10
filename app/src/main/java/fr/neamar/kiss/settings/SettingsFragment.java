package fr.neamar.kiss.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import java.util.LinkedHashMap;
import java.util.Map;

import fr.neamar.kiss.R;

/**
 * Root settings screen, linking to all the other screens
 */
public class SettingsFragment extends BaseSettingsFragment {
    /**
     * All preference XMLs, with the screen displaying them.
     * Used to initialize default values (they are not read when navigating between screens) and to search settings.
     * SettingsFragmentTest checks that no XML is missing.
     */
    static final Map<Integer, Class<? extends BaseSettingsFragment>> PREFERENCES_XML = new LinkedHashMap<>();

    static {
        PREFERENCES_XML.put(R.xml.preferences, SettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_appearance, AppearanceSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_home, HomeScreenSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_home_wallpaper, WallpaperSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_search_bar, SearchBarSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_search, SearchSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_search_apps, AppsSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_search_contacts, ContactsSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_search_shortcuts, ShortcutsSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_search_web, WebSearchSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_history, HistorySettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_favorites, FavoritesSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_favorites_tags_menu, TagsMenuSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_gestures, GesturesSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_import_export, ImportExportSettingsFragment.class);
        PREFERENCES_XML.put(R.xml.preferences_system, SystemSettingsFragment.class);
    }

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
        for (int xml : PREFERENCES_XML.keySet()) {
            PreferenceManager.setDefaultValues(context, xml, true);
        }
    }

    /**
     * Write the default value of all settings in the given shared preferences.
     */
    public static void setDefaultValues(@NonNull Context context, @NonNull String sharedPreferencesName) {
        for (int xml : PREFERENCES_XML.keySet()) {
            PreferenceManager.setDefaultValues(context, sharedPreferencesName, Context.MODE_PRIVATE, xml, true);
        }
    }
}
