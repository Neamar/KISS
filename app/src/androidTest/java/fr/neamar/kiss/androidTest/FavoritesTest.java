package fr.neamar.kiss.androidTest;

import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.WindowManager;

import org.junit.Before;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

@LargeTest
public class FavoritesTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public FavoritesTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        getActivity();

        // Initialize to default preferences
        KissApplication.getApplication(getActivity()).getDataHandler().clearHistory();
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, true);
        getActivity().prefs.edit().putBoolean("enable-favorites-bar", true).apply();

        // Remove lock screen
        Runnable wakeUpDevice = new Runnable() {
            public void run() {
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        };
        getActivity().runOnUiThread(wakeUpDevice);
    }

    private void enableInternalBar() {
        getActivity().prefs.edit().putBoolean("enable-favorites-bar", false).apply();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getActivity().recreate();
            }
        });
        setActivity(null);
        getActivity();
    }

    public void testExternalBarDisplayed() {
        onView(withId(R.id.externalFavoriteBar)).check(matches(isDisplayed()));
    }

    public void testExternalBarHiddenWhenViewingAllApps() {
        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.whiteLauncherButton)).perform(click());
        onView(withId(R.id.externalFavoriteBar)).check(matches(isDisplayed()));
    }

    public void testInternalBarHiddenWhenViewingAllAppsWithExternalModeOn() {
        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(not(isDisplayed())));
    }

    public void testExternalBarHiddenOnSearch() {
        onView(withId(R.id.searchEditText)).perform(typeText("Test"));
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.clearButton)).perform(click());
        onView(withId(R.id.externalFavoriteBar)).check(matches(isDisplayed()));
    }

    public void testInternalBarHidden() {
        enableInternalBar();

        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(not(isDisplayed())));
    }

    public void testInternalBarHiddenOnSearch() {
        onView(withId(R.id.searchEditText)).perform(typeText("Test"));
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.clearButton)).perform(click());
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(not(isDisplayed())));
    }

    public void testInternalBarDisplayedWhenViewingAllApps() {
        enableInternalBar();
        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.externalFavoriteBar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.embeddedFavoritesBar)).check(matches(isDisplayed()));
    }
}