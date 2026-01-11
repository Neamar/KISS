package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import androidx.test.filters.LargeTest;

import org.junit.Test;

import fr.neamar.kiss.R;

@LargeTest
public class FavoritesTest extends AbstractMainActivityTest {
    @SuppressWarnings("CatchAndPrintStackTrace")
    private void enableInternalBar() {
        mActivityRule.getScenario().onActivity(activity -> {
            activity.prefs.edit().putBoolean("enable-favorites-bar", false).apply();
            try {
                activity.runOnUiThread(activity::recreate);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });
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
}
