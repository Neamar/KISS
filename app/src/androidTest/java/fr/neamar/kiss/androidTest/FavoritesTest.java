package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

import org.junit.Test;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;

@LargeTest
public class FavoritesTest extends AbstractMainActivityTest {
    private void enableInternalBar() {
        scenario.onActivity(activity -> activity.prefs.edit().putBoolean("enable-favorites-bar", false).commit());
        scenario.recreate();
    }

    // Apps are launched through LauncherApps, which espresso-intents can't stub: use a setting instead
    private void useBatterySettingAsOnlyFavorite() {
        scenario.onActivity(activity -> activity.prefs.edit()
                .putString("favorite-apps-list", "setting://" + Intent.ACTION_POWER_USAGE_SUMMARY.toLowerCase(Locale.ENGLISH) + ";").commit());
        scenario.recreate();
        waitForSearch();
    }

    @Test
    public void testClickLaunchesFavoriteAndAddsItToHistory() {
        useBatterySettingAsOnlyFavorite();
        onView(withId(R.id.favorite)).perform(click());
        intended(hasAction(Intent.ACTION_POWER_USAGE_SUMMARY));
        waitForSearch();
        // Favorites are excluded from the displayed history by default
        assertThat(DBHelper.getHistoryLength(ApplicationProvider.getApplicationContext()), is(1));
    }

    @Test
    public void testLongClickMenuRemovesFavorite() {
        useBatterySettingAsOnlyFavorite();
        onView(withId(R.id.favorite)).perform(longClick());
        onView(withText(R.string.menu_favorites_remove)).inRoot(isPlatformPopup()).perform(click());
        onView(withId(R.id.favorite)).check(matches(not(isDisplayed())));
    }

    // The bar may be hidden, and then not laid out: check its content instead of its children
    private void checkFavoritesCount(int barId, int count) {
        onView(withId(barId)).check((view, noViewFoundException) -> {
            RecyclerView bar = (RecyclerView) view;
            assertThat(bar.getAdapter().getItemCount(), is(count));
            // Favorites are evenly spaced by having one column each
            assertThat(((GridLayoutManager) bar.getLayoutManager()).getSpanCount(), is(count));
        });
    }

    private void toggleFavorite(int menuItem) {
        onView(withText(menuItem)).inRoot(isPlatformPopup()).perform(click());
    }

    @Test
    public void testAddAndRemoveFavoriteFromSearch() {
        useBatterySettingAsOnlyFavorite();
        search("0123456789");
        onView(withId(R.id.item_phone_text)).perform(longClick());
        toggleFavorite(R.string.menu_favorites_add);
        // The bar is hidden while searching
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));
        onView(withId(R.id.searchEditText)).check(matches(withText("0123456789")));
        checkFavoritesCount(R.id.externalFavoriteBar, 2);

        onView(withId(R.id.item_phone_text)).perform(longClick());
        toggleFavorite(R.string.menu_favorites_remove);
        checkFavoritesCount(R.id.externalFavoriteBar, 1);
    }

    @Test
    public void testAddFavoriteFromAppList() {
        enableInternalBar();
        useBatterySettingAsOnlyFavorite();
        onView(withId(R.id.launcherButton)).perform(click());
        waitForSearch();
        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0).perform(longClick());
        toggleFavorite(R.string.menu_favorites_add);
        onView(withId(R.id.mainKissbar)).check(matches(isDisplayed()));
        checkFavoritesCount(R.id.embeddedFavoritesBar, 2);
    }

    @Test
    public void testDefaultFavoritesOnFirstRun() {
        // Each test starts from a first run, with an empty history
        Context context = ApplicationProvider.getApplicationContext();
        List<String> favorites = KissApplication.getApplication(context).getDataHandler().getFavorites()
                .stream().map(pojo -> pojo.id).collect(Collectors.toList());
        for (Intent intent : new Intent[]{
                new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0000")),
                new Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI),
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://"))}) {
            ResolveInfo app = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            assertThat(favorites, hasItem(containsString("app://" + app.activityInfo.packageName + "/")));
        }
    }

    @Test
    public void testNoDefaultFavoritesWhenUpdating() {
        Context context = ApplicationProvider.getApplicationContext();
        DBHelper.insertHistory(context, "", "phone://0123456789");
        scenario.onActivity(activity -> activity.prefs.edit()
                .putBoolean("first-run-favorites", true)
                .putString("favorite-apps-list", "").commit());
        scenario.recreate();
        assertThat(KissApplication.getApplication(context).getDataHandler().getFavorites(), empty());
    }

    @Test
    public void testExternalBarDisplayed() {
        onView(withId(R.id.externalFavoriteBar)).check(matches(isDisplayed()));
    }

    @Test
    public void testExternalBarHiddenWhenViewingAllApps() {
        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.whiteLauncherButton)).perform(click());
        onView(withId(R.id.externalFavoriteBar)).check(matches(isDisplayed()));
    }

    @Test
    public void testInternalBarHiddenWhenViewingAllAppsWithExternalModeOn() {
        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testExternalBarHiddenOnSearch() {
        onView(withId(R.id.searchEditText)).perform(typeText("Test"));
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.clearButton)).perform(click());
        onView(withId(R.id.externalFavoriteBar)).check(matches(isDisplayed()));
    }

    @Test
    public void testInternalBarHidden() {
        enableInternalBar();

        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testInternalBarHiddenOnSearch() {
        enableInternalBar();
        onView(withId(R.id.searchEditText)).perform(typeText("Test"));
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.clearButton)).perform(click());
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testInternalBarDisplayedWhenViewingAllApps() {
        enableInternalBar();
        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(isDisplayed()));
    }

    @Test
    public void testInternalBarFavoriteClickable() {
        enableInternalBar();
        useBatterySettingAsOnlyFavorite();
        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.favorite)).perform(click());
        intended(hasAction(Intent.ACTION_POWER_USAGE_SUMMARY));
    }

    @Test
    public void testInternalBarFavoriteLongClickable() {
        enableInternalBar();
        useBatterySettingAsOnlyFavorite();
        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.favorite)).perform(longClick());
        onView(withText(R.string.menu_favorites_remove)).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
    }

    @Test
    public void testInternalBarExternalBarHiddenAfterLaunchingResult() {
        enableInternalBar();
        search("0123456789");
        onView(withId(R.id.item_phone_text)).perform(click());
        waitForLaunchOccurred();
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
    }

    private void enableMinimalisticFavorites() {
        scenario.onActivity(activity -> activity.prefs.edit()
                .putBoolean("history-hide", true)
                .putBoolean("history-onclick", true)
                .putBoolean("favorites-hide", true).commit());
        startAsHome();
    }

    private void touchEmptyArea() {
        onView(withId(android.R.id.content)).perform(click());
        waitForSearch();
    }

    @Test
    public void testMinimalisticFavoritesHiddenUntilTouched() {
        DBHelper.insertHistory(ApplicationProvider.getApplicationContext(), "", "phone://0123456789");
        enableMinimalisticFavorites();
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(android.R.id.list)).check(matches(not(isDisplayed())));

        touchEmptyArea();
        onView(withId(R.id.externalFavoriteBar)).check(matches(isDisplayed()));
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));

        pressHome();
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testMinimalisticFavoritesHiddenOnSearch() {
        enableMinimalisticFavorites();
        touchEmptyArea();
        search("Test");
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.clearButton)).perform(click());
        waitForSearch();
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testMinimalisticFavoritesHiddenAfterSearchThenHome() {
        enableMinimalisticFavorites();
        touchEmptyArea();
        search("Test");
        pressHome();
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testMinimalisticFavoritesHiddenAfterLaunchingResult() {
        enableMinimalisticFavorites();
        search("0123456789");
        onView(withId(R.id.item_phone_text)).perform(click());
        waitForLaunchOccurred();
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
    }
}
