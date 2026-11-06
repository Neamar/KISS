package fr.neamar.kiss;

import static fr.neamar.kiss.dataprovider.ProviderName.*;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.dataprovider.simpleprovider.TagsProvider;
import fr.neamar.kiss.forwarder.ExperienceTweaks;
import fr.neamar.kiss.forwarder.InterfaceTweaks;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.searcher.QuerySearcher;
import fr.neamar.kiss.settings.SettingsFragment;
import fr.neamar.kiss.utils.SystemUiVisibilityHelper;

/**
 * Hosts the settings screens (see {@link fr.neamar.kiss.settings.BaseSettingsFragment}),
 * and reacts to settings changes that impact the rest of the app, whichever screen they come from (including settings import).
 */
public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    // Those settings require the app to restart
    private static final List<String> SETTINGS_REQUIRING_RESTART = Arrays.asList("primary-color", "transparent-search", "transparent-favorites",
            "pref-rounded-list", "pref-rounded-bars", "pref-swap-kiss-button-with-menu", "pref-hide-circle", "history-hide",
            "enable-favorites-bar", "notification-bar-color", "black-notification-icons", "icons-pack", "theme-shadow",
            "theme-separator", "theme-result-color", "large-favorites-bar", "pref-hide-search-bar-hint", "theme-wallpaper",
            "theme-bar-color", "results-size", "large-result-list-margins", "themed-icons", "icons-hide",
            "pref-fav-tags-drawable", null);
    // Those settings require a restart of the settings
    private static final List<String> SETTINGS_REQUIRING_RESTART_FOR_SETTINGS_ACTIVITY = Arrays.asList("theme", "force-portrait", "night-mode", null);

    private boolean requireFullRestart = false;

    private SharedPreferences prefs;

    private SystemUiVisibilityHelper systemUiVisibilityHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        InterfaceTweaks.applySettingsTheme(this, prefs);

        systemUiVisibilityHelper = new SystemUiVisibilityHelper(this, true);

        // Lock launcher into portrait mode
        // Do it here to make the transition as smooth as possible
        ExperienceTweaks.setRequestedOrientation(this, prefs);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_toolbar_content);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // When recreated, the fragment manager restores the screens itself
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.help) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://help.kisslauncher.com"));
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            onSettingChanged(sharedPreferences, key);
        }

        if (SETTINGS_REQUIRING_RESTART.contains(key) || SETTINGS_REQUIRING_RESTART_FOR_SETTINGS_ACTIVITY.contains(key)) {
            requireFullRestart = true;

            if (SETTINGS_REQUIRING_RESTART_FOR_SETTINGS_ACTIVITY.contains(key)) {
                // Kill this activity too, and restart
                recreate();
            }
        }
    }

    /**
     * Update the rest of the app after a setting changed.
     * UI changes in the settings themselves are handled by each screen.
     */
    private void onSettingChanged(SharedPreferences sharedPreferences, @NonNull String key) {
        KissApplication.getApplication(this).getIconsHandler().onPrefChanged(sharedPreferences, key);

        switch (key) {
            case "available-search-providers":
            case "selected-search-provider-names":
            case "default-search-provider":
                getDataHandler().reload(SEARCH);
                break;
            case "primary-color":
                UIColors.clearColorCache();
                break;
            case "number-of-display-elements":
                QuerySearcher.clearMaxResultCountCache();
                break;
            case "pref-fav-tags-list":
                getDataHandler().reload(TAGS);

                // after we edit the fav tags list update DataHandler
                Set<String> favTags = sharedPreferences.getStringSet(key, Collections.emptySet());
                DataHandler dh = getDataHandler();
                List<Pojo> favoritesPojo = dh.getFavorites();
                for (Pojo pojo : favoritesPojo)
                    if (pojo instanceof TagDummyPojo && !favTags.contains(pojo.getName()))
                        dh.removeFromFavorites(pojo.id);
                for (String tagName : favTags)
                    dh.addToFavorites(TagsProvider.generateUniqueId(tagName));
                break;
            case "exclude-favorites-apps":
                getDataHandler().reload(APPS);
                break;
            case "selected-contact-mime-types":
                getDataHandler().reload(CONTACTS);
                break;
            case "enable-settings":
                getDataHandler().reload(SETTINGS);
                break;
            case "theme":
                // AMOLED theme is always dark
                if ("amoled-dark".equals(sharedPreferences.getString("theme", "transparent"))) {
                    sharedPreferences.edit().putString("night-mode", "yes").apply();
                }
                break;
            case "night-mode":
                InterfaceTweaks.setDefaultNightMode(KissApplication.getApplication(this));
                break;
        }
    }

    private DataHandler getDataHandler() {
        return KissApplication.getApplication(this).getDataHandler();
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        // Some settings require a full UI refresh,
        // Flag this, so that MainActivity get the information onResume().
        if (requireFullRestart) {
            prefs.edit().putBoolean("require-layout-update", true).apply();
            requireFullRestart = false;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        systemUiVisibilityHelper.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(pref.getExtras());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
}
