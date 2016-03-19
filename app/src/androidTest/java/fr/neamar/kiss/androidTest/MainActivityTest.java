
package fr.neamar.kiss.androidTest;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@LargeTest
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public MainActivityTest() {
        super(MainActivity.class);
    }
    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        getActivity();
    }

    public void testCanTypeTextIntoSearchBox() {
        onView(withId(R.id.searchEditText)).perform(typeText("Test"));
        onView(withId(R.id.searchEditText)).check(matches(withText("Test")));
    }

    public void testSearchResultAppears() {
        onView(withId(R.id.searchEditText)).perform(typeText("blahblah"));
        onView(withId(R.id.item_search_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.item_search_text)).check(matches(withText("Google Search for “blahblah”")));
    }

    public void testTorchToggleAppears() {
        onView(withId(R.id.searchEditText)).perform(typeText("torch"));
        onView(withId(R.id.item_toggle_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.item_toggle_name)).check(matches(withText("Toggle: Torch")));
    }

    public void testBatterySettingAppears() {
        onView(withId(R.id.searchEditText)).perform(typeText("batter"));
        onView(withId(R.id.item_setting_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.item_setting_name)).check(matches(withText("Setting: Battery")));
    }
}
