package fr.neamar.kiss;

import static fr.neamar.kiss.dataprovider.ProviderName.*;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.dataprovider.simpleprovider.TagsProvider;
import fr.neamar.kiss.forwarder.ExperienceTweaks;
import fr.neamar.kiss.forwarder.InterfaceTweaks;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.searcher.QuerySearcher;
import fr.neamar.kiss.settings.BaseSettingsFragment;
import fr.neamar.kiss.settings.SettingsFragment;
import fr.neamar.kiss.settings.SettingsSearchFragment;
import fr.neamar.kiss.utils.SystemUiVisibilityHelper;

/**
 * Hosts the settings screens (see {@link fr.neamar.kiss.settings.BaseSettingsFragment}),
 * and reacts to settings changes that impact the rest of the app, whichever screen they come from (including settings import).
 */
public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private static final String SEARCH_TAG = "search";

    private boolean requireFullRestart = false;

    private SharedPreferences prefs;

    private SystemUiVisibilityHelper systemUiVisibilityHelper;

    @ColorInt
    private int toolbarTextColor;

    private MenuItem searchItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        InterfaceTweaks.applySettingsTheme(this, prefs);

        systemUiVisibilityHelper = new SystemUiVisibilityHelper(this, true);

        // Lock launcher into portrait mode
        // Do it here to make the transition as smooth as possible
        ExperienceTweaks.setRequestedOrientation(this, prefs);

        super.onCreate(savedInstanceState);

        // Theme the toolbar for the primary color, so views it creates later (search field, collapse button) match too
        toolbarTextColor = UIColors.getTextColorOnPrimary(this);
        getTheme().applyStyle(toolbarTextColor == Color.BLACK ? R.style.OverlayToolbarDarkText : R.style.OverlayToolbarLightText, true);

        setContentView(R.layout.activity_toolbar_content);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        toolbar.setTitleTextColor(toolbarTextColor);
        setSupportActionBar(toolbar);
        InterfaceTweaks.applySystemBarInsets(findViewById(R.id.settings_root), toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        Drawable navigationIcon = toolbar.getNavigationIcon();
        if (navigationIcon != null) {
            navigationIcon.mutate().setTint(toolbarTextColor);
        }

        // When recreated, the fragment manager restores the screens itself
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_container, new SettingsFragment())
                    .commit();
        } else {
            // The search field is collapsed when recreated
            getSupportFragmentManager().popBackStack(SEARCH_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
        MenuItemCompat.setIconTintList(menu.findItem(R.id.help), ColorStateList.valueOf(toolbarTextColor));

        searchItem = menu.findItem(R.id.search);
        MenuItemCompat.setIconTintList(searchItem, ColorStateList.valueOf(toolbarTextColor));
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.settings_search_hint));
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_container, new SettingsSearchFragment(), SEARCH_TAG)
                        .addToBackStack(SEARCH_TAG)
                        .commit();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                getSupportFragmentManager().popBackStack(SEARCH_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                getSupportFragmentManager().executePendingTransactions();
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(SEARCH_TAG);
                if (fragment instanceof SettingsSearchFragment) {
                    ((SettingsSearchFragment) fragment).search(newText);
                }
                return true;
            }
        });
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
    }

    /**
     * Some settings are only applied when restarting the launcher (on resume),
     * or also these settings, e.g. for the theme.
     * Set in preferences XML with a "restart" extra, either "launcher" or "settings".
     */
    public void requireRestart(boolean restartSettings) {
        requireFullRestart = true;
        if (restartSettings) {
            recreate();
        }
    }

    /**
     * Update the rest of the app after a setting changed.
     * UI changes in the settings themselves are handled by each screen.
     */
    private void onSettingChanged(SharedPreferences sharedPreferences, @NonNull String key) {
        KissApplication.getApplication(this).getIconsHandler().onPrefChanged(sharedPreferences, key);

        if (key.startsWith("contact-action-")) {
            getDataHandler().reload(CONTACTS);
        }

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
            case "enable-settings":
                getDataHandler().reload(SETTINGS);
                break;
            case "enable-phone-history":
                // The permission is requested from the history screen
                IncomingCallHandler.setEnabled(this, sharedPreferences.getBoolean(key, false));
                break;
            case "theme":
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
        openScreen(pref.getFragment(), pref.getExtras());
        return true;
    }

    /**
     * Close the search, and open the screen of the setting found, scrolled to that setting
     */
    public void openSearchResult(@NonNull String fragment, @Nullable String key, @Nullable Bundle extras) {
        searchItem.collapseActionView();
        // Copy: the extras belong to the preference
        Bundle args = extras == null ? new Bundle() : new Bundle(extras);
        args.putString(BaseSettingsFragment.ARG_SCROLL_TO_KEY, key);
        openScreen(fragment, args);
    }

    private void openScreen(String fragmentName, Bundle args) {
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                fragmentName);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
