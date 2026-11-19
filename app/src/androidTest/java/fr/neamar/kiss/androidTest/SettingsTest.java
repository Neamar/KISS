package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.RemoteException;

import androidx.annotation.StringRes;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ProviderName;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.settings.SettingsFragment;

@LargeTest
public class SettingsTest extends AbstractMainActivityTest {
    private static final String PROVIDER = "zzqprovider";
    private static final String PROVIDER_URL = "https://example.com/?q=%s";

    private final Context context = ApplicationProvider.getApplicationContext();
    private final DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();

    /**
     * Scrolls to the preference on the current screen, as it may be below the fold, then clicks it
     */
    private static void clickPreference(String key) {
        CharSequence title = fromSettings(activity -> {
            PreferenceFragmentCompat screen = getScreen(activity);
            Preference preference = screen.findPreference(key);
            screen.scrollToPreference(preference);
            return preference.getTitle();
        });
        onView(allOf(withId(android.R.id.title), withText(title.toString()))).perform(click());
    }

    private static String getSettingsTitle() {
        return fromSettings(activity -> activity.getTitle().toString());
    }

    private static void clickDialogButton(int button) {
        onView(withId(button)).inRoot(isDialog()).perform(click());
    }

    @Test
    public void testResetHistory() {
        for (int i = 0; i < 6; i++) {
            DBHelper.insertHistory(context, "", "phone://" + i);
        }
        String historyLength = context.getString(R.string.items_title, 6);
        inSettings(() -> {
            clickPreference("history_category");
            clickPreference("reset-history");
            clickDialogButton(android.R.id.button2);
            assertThat(DBHelper.getHistoryLength(context), is(6));
            onView(withText(historyLength)).check(matches(isDisplayed()));

            clickPreference("reset-history");
            clickDialogButton(android.R.id.button1);
            assertThat(DBHelper.getHistoryLength(context), is(0));
            onView(withText(historyLength)).check(doesNotExist());
            onView(withText(R.string.history_erased)).check(matches(isDisplayed()));
        });
    }

    @Test
    public void testResetExcludedApps() {
        AppPojo settingsApp = dataHandler.getApplications().stream()
                .filter(app -> app.packageName.equals("com.android.settings")).findFirst().get();
        scenario.onActivity(activity -> dataHandler.addToExcluded(settingsApp));
        try {
            inSettings(() -> {
                clickPreference("providers");
                clickPreference("search-apps");
                clickPreference("edit-excluded-apps");
                clickPreference("reset-excluded-list");
                clickDialogButton(android.R.id.button1);
            });
            assertThat(dataHandler.getExcluded(), not(hasItem(settingsApp.id)));

            onView(withId(R.id.launcherButton)).perform(click());
            waitForSearch();
            scenario.onActivity(activity -> {
                Set<String> ids = new HashSet<>();
                for (int i = 0; i < activity.adapter.getCount(); i++) {
                    ids.add(activity.adapter.getItem(i).getPojo().id);
                }
                assertThat(ids, hasItem(settingsApp.id));
            });
        } finally {
            // The provider outlives the activity, restore it for the next tests
            scenario.onActivity(activity -> dataHandler.removeFromExcluded(settingsApp));
        }
    }

    private void openWebSearchSettings() {
        clickPreference("providers");
        clickPreference("search-web");
    }

    private void addProvider() {
        Set<String> available = new HashSet<>(SearchProvider.getDefaultSearchProviders(context));
        available.add(PROVIDER + "|" + PROVIDER_URL);
        scenario.onActivity(activity -> {
            activity.prefs.edit()
                    .putStringSet("available-search-providers", available)
                    .putStringSet("selected-search-provider-names", new HashSet<>(Arrays.asList("Google", PROVIDER)))
                    .commit();
            dataHandler.reload(ProviderName.SEARCH);
        });
        checkProviderInSearch(true);
        onView(withId(R.id.clearButton)).perform(click());
    }

    private void checkProviderInSearch(boolean available) {
        search("blahblah");
        if (available) {
            onView(withText(context.getString(R.string.ui_item_search, PROVIDER, "blahblah"))).check(matches(isDisplayed()));
        } else {
            // Proves the search did run, so the missing result is caused by the provider
            onView(withText(context.getString(R.string.ui_item_search, "Google", "blahblah"))).check(matches(isDisplayed()));
            onView(withText(context.getString(R.string.ui_item_search, PROVIDER, "blahblah"))).check(doesNotExist());
        }
    }

    // The provider outlives the activity, while preferences are only reset before the next test
    private void restoreSearchProviders() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove("available-search-providers")
                .remove("selected-search-provider-names")
                .commit();
        scenario.onActivity(activity -> dataHandler.reload(ProviderName.SEARCH));
    }

    @Test
    public void testAddAndSelectSearchProvider() {
        try {
            inSettings(() -> {
                openWebSearchSettings();
                clickPreference("custom-search-provider-add");
                // Typing would show the keyboard, which sometimes dismisses the dialog
                onView(withId(R.id.search_provider_name)).perform(replaceText(PROVIDER));
                onView(withId(R.id.search_provider_url)).perform(replaceText(PROVIDER_URL));
                clickDialogButton(android.R.id.button1);

                clickPreference("selected-search-provider-names");
                onData(hasToString(PROVIDER)).inRoot(isDialog()).perform(click());
                clickDialogButton(android.R.id.button1);
            });
            checkProviderInSearch(true);
        } finally {
            restoreSearchProviders();
        }
    }

    @Test
    public void testResetSearchProviders() {
        try {
            addProvider();
            inSettings(() -> {
                openWebSearchSettings();
                clickPreference("reset-search-providers");
                clickDialogButton(android.R.id.button1);

                Set<String> defaults = SearchProvider.getDefaultSearchProviders(context);
                String[] defaultNames = defaults.stream().map(provider -> provider.split("\\|")[0]).toArray(String[]::new);
                CharSequence[] entries = fromSettings(activity ->
                        ((MultiSelectListPreference) getScreen(activity).findPreference("selected-search-provider-names")).getEntries());
                assertThat(entries, arrayContainingInAnyOrder(defaultNames));
            });
            checkProviderInSearch(false);
        } finally {
            restoreSearchProviders();
        }
    }

    @Test
    public void testDeleteSearchProvider() {
        try {
            addProvider();
            inSettings(() -> {
                openWebSearchSettings();
                clickPreference("deleting-search-providers-names");
                onData(hasToString(PROVIDER)).inRoot(isDialog()).perform(click());
                clickDialogButton(android.R.id.button1);
            });
            checkProviderInSearch(false);
        } finally {
            restoreSearchProviders();
        }
    }

    private static void searchSettings(String query) {
        onView(withId(R.id.search)).perform(click());
        // Typed at once: each change updates the results, and removed ones linger in the list for a while
        onView(withId(androidx.appcompat.R.id.search_src_text)).perform(replaceText(query));
    }

    private static void clickSearchResult(@StringRes int title) {
        // The search field contains the same text
        onView(allOf(withId(android.R.id.title), withText(title))).perform(click());
    }

    private static void checkMainScreenDisplayed() {
        // Its items may be below the fold, e.g. in landscape
        assertThat(fromSettings(activity -> getScreen(activity) instanceof SettingsFragment), is(true));
        onView(withId(androidx.appcompat.R.id.search_src_text)).check((view, noViewFoundException) ->
                assertThat(view == null || !view.isShown(), is(true)));
    }

    @Test
    public void testSettingsSearch() {
        inSettings(() -> {
            onView(withId(R.id.search)).perform(click());
            onView(withId(androidx.preference.R.id.recycler_view)).check(matches(hasChildCount(0)));

            onView(withId(androidx.appcompat.R.id.search_src_text)).perform(replaceText(context.getString(R.string.shape_hexagon)));
            onView(allOf(withText(R.string.adaptive_shape_name), hasSibling(withText(R.string.title_appearance)))).check(matches(isDisplayed()));
            onView(allOf(withText(R.string.contacts_shape), hasSibling(withText(R.string.contacts_name)))).check(matches(isDisplayed()));

            clickSearchResult(R.string.adaptive_shape_name);
            // Far down the screen: only displayed when scrolled to
            onView(allOf(withId(android.R.id.title), withText(R.string.adaptive_shape_name))).check(matches(isDisplayed()));
            assertThat(getSettingsTitle(), is(context.getString(R.string.title_appearance)));

            pressBack();
            checkMainScreenDisplayed();
        });
    }

    @Test
    public void testSettingsSearchOpensLinkedScreen() {
        inSettings(() -> {
            searchSettings(context.getString(R.string.ui_excluded_from_history_apps));
            clickSearchResult(R.string.ui_excluded_from_history_apps);
            assertThat(getSettingsTitle(), is(context.getString(R.string.ui_excluded_from_history_apps)));
            onView(withText(R.string.reset_excluded_list_name)).check(matches(isDisplayed()));
            // The apps to exclude are listed below the reset button
            onView(withId(androidx.preference.R.id.recycler_view)).check(matches(hasMinimumChildCount(2)));
        });
    }

    @Test
    public void testSettingsSearchClosedOnRotation() throws RemoteException {
        scenario.onActivity(activity -> activity.prefs.edit().putBoolean("force-portrait", false).commit());
        try {
            inSettings(() -> {
                searchSettings(context.getString(R.string.shape_hexagon));
                try {
                    device.setOrientationLeft();
                } catch (RemoteException e) {
                    throw new AssertionError(e);
                }
                // The activity is recreated
                assertThat(device.wait(d -> Integer.valueOf(Configuration.ORIENTATION_LANDSCAPE).equals(
                        fromSettings(activity -> activity.getResources().getConfiguration().orientation)), 5000), is(true));
                checkMainScreenDisplayed();
            });
        } finally {
            device.setOrientationNatural();
            device.unfreezeRotation();
        }
    }
}
