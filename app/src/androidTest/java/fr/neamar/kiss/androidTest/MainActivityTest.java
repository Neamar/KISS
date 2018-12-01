package fr.neamar.kiss.androidTest;

import androidx.test.filters.LargeTest;

import org.junit.Test;

import fr.neamar.kiss.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@LargeTest
public class MainActivityTest extends AbstractMainActivityTest {
    @Test
    public void testHintDisplayed() {
        onView(withId(R.id.searchEditText)).check(matches(withHint(R.string.ui_search_hint)));
    }

    @Test
    public void testCanTypeTextIntoSearchBox() {
        onView(withId(R.id.searchEditText)).perform(typeText("Test"));
        onView(withId(R.id.searchEditText)).check(matches(withText("Test")));
    }

    @Test
    public void testSearchBoxTrimming() {
        // Spaces on the left are not recorded
        onView(withId(R.id.searchEditText)).perform(typeText(" "));
        onView(withId(R.id.searchEditText)).check(matches(withText("")));
    }

    @Test
    public void testMenuAndClearButtonsDisplayed() {
        // menu by default, no X
        onView(withId(R.id.menuButton)).check(matches(isDisplayed()));
        onView(withId(R.id.clearButton)).check(matches(not(isDisplayed())));

        // X when search isn't empty, no menu
        onView(withId(R.id.searchEditText)).perform(typeText("Test"));

        onView(withId(R.id.menuButton)).check(matches(not(isDisplayed())));
        onView(withId(R.id.clearButton)).check(matches(isDisplayed()));

        // when X touched, query reset and menu displayed again
        onView(withId(R.id.clearButton)).perform(click());

        onView(withId(R.id.menuButton)).check(matches(isDisplayed()));
        onView(withId(R.id.clearButton)).check(matches(not(isDisplayed())));
        onView(withId(R.id.searchEditText)).check(matches(withText("")));
    }

    @Test
    public void testSearchResultAppears() {
        onView(withId(R.id.searchEditText)).perform(typeText("blahblah"));
        onView(withId(R.id.item_search_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.item_search_text)).check(matches(withText("Search Google for “blahblah”")));
    }

    @Test
    public void testBatterySettingAppears() {
        onView(withId(R.id.searchEditText)).perform(typeText("batter"));
        onView(withId(R.id.item_setting_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.item_setting_name)).check(matches(withText("Battery")));
    }

    @Test
    public void testKissBarDisplayed() {
        // Sanity check
        onView(withId(R.id.mainKissbar)).check(matches(not(isDisplayed())));
        onView(allOf(withId(R.id.item_app_name), withText("Settings"))).check(doesNotExist());

        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.mainKissbar)).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.item_app_name), withText("Settings"))).check(matches(isDisplayed()));
    }

    @Test
    public void testKissBarHidden() {
        onView(withId(R.id.launcherButton)).perform(click());

        onView(withId(R.id.mainKissbar)).check(matches(isDisplayed()));

        onView(withId(R.id.whiteLauncherButton)).perform(click());
        onView(withId(R.id.mainKissbar)).check(matches(not(isDisplayed())));
    }


    // Searching for something, then displaying and hiding the search bar, should empty query
    @Test
    public void testKissBarEmptiesSearch() {
        // Sanity check
        onView(withId(R.id.searchEditText)).perform(typeText("Test"));
        onView(withId(R.id.searchEditText)).check(matches(withText("Test")));

        onView(withId(R.id.launcherButton)).perform(click());

        onView(withId(R.id.mainKissbar)).check(matches(isDisplayed()));

        onView(withId(R.id.whiteLauncherButton)).perform(click());

        onView(withId(R.id.searchEditText)).check(matches(withText("")));
    }
}
