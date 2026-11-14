package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.app.Activity;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Until;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;

@LargeTest
public class SystemNavigationTest extends AbstractMainActivityTest {
    private void addHistory() {
        DBHelper.insertHistory(ApplicationProvider.getApplicationContext(), "", "phone://0123456789");
        startAsHome();
    }

    private void assertKissBarHidden() {
        // Hidden at the end of an animation, which Espresso doesn't wait for on API 29
        String kiss = ApplicationProvider.getApplicationContext().getPackageName();
        assertThat(device.wait(Until.gone(By.res(kiss, "mainKissbar")), 5000), is(true));
    }

    @Test
    public void testHomeClearsSearchAndDisplaysHistory() {
        addHistory();
        search("blahblah");
        pressHome();
        onView(withId(R.id.searchEditText)).check(matches(withText("")));
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));
    }

    @Test
    public void testHomeHidesAppList() {
        addHistory();
        onView(withId(R.id.launcherButton)).perform(click());
        pressHome();
        assertKissBarHidden();
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackHidesAppList() {
        addHistory();
        onView(withId(R.id.launcherButton)).perform(click());
        device.pressBack();
        assertKissBarHidden();
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackClearsSearchAndDisplaysHistory() {
        addHistory();
        search("blahblah");
        closeSoftKeyboard();
        device.pressBack();
        waitForSearch();
        onView(withId(R.id.searchEditText)).check(matches(withText("")));
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackOnHistoryDoesNotQuit() {
        addHistory();
        Activity home = getResumedActivity();
        device.pressBack();
        waitForSearch();
        // As the default launcher, a closed KISS would be started again by the system
        assertThat(getResumedActivity(), is(home));
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));
    }

    private static Activity getResumedActivity() {
        AtomicReference<Activity> resumed = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> resumed.set(
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).iterator().next()));
        return resumed.get();
    }

    @Test
    public void testGoingBackToKissAfterLaunch() {
        startAsHome();
        // Actually leave KISS: drop the stub that keeps every intent inside it
        Intents.release();
        Intents.init();

        search("batter");
        onView(withText("Battery")).perform(click());
        intended(hasAction(Intent.ACTION_POWER_USAGE_SUMMARY));
        String kiss = ApplicationProvider.getApplicationContext().getPackageName();
        assertThat(device.wait(Until.gone(By.pkg(kiss).depth(0)), 10_000), is(true));

        pressHome();
        onView(withId(R.id.searchEditText)).check(matches(withText("")));
        onView(withId(R.id.item_setting_name)).check(matches(withText("Battery")));
        onView(withId(R.id.item_search_text)).check(doesNotExist());
    }
}
