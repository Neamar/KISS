package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.content.res.Configuration;
import android.os.RemoteException;

import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import fr.neamar.kiss.R;

@LargeTest
public class OrientationTest extends AbstractMainActivityTest {
    @After
    public void restoreOrientation() throws RemoteException {
        device.setOrientationNatural();
        device.unfreezeRotation();
    }

    private void allowRotation() {
        scenario.onActivity(activity -> activity.prefs.edit().putBoolean("force-portrait", false).commit());
        scenario.recreate();
    }

    private int getOrientation() {
        AtomicInteger orientation = new AtomicInteger();
        onView(withId(R.id.searchEditText)).check((view, noViewFoundException) ->
                orientation.set(view.getResources().getConfiguration().orientation));
        return orientation.get();
    }

    private void rotate() throws RemoteException {
        device.setOrientationLeft();
        // The activity is recreated
        assertThat(device.wait(d -> getOrientation() == Configuration.ORIENTATION_LANDSCAPE, 5000), is(true));
        waitForSearch();
    }

    @Test
    public void testRotationKeepsSearch() throws RemoteException {
        allowRotation();
        search("blahblah");
        rotate();
        onView(withId(R.id.searchEditText)).check(matches(withText("blahblah")));
        onView(withId(R.id.item_search_text)).check(matches(isDisplayed()));
    }

    @Test
    public void testRotationKeepsAppList() throws RemoteException {
        allowRotation();
        onView(withId(R.id.launcherButton)).perform(click());
        rotate();
        // The activity is recreated with the app list hidden, but it can be displayed again
        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.mainKissbar)).check(matches(isDisplayed()));
    }

    @Test
    public void testPortraitLocked() throws RemoteException {
        device.setOrientationLeft();
        device.waitForIdle();
        assertThat(getOrientation(), is(Configuration.ORIENTATION_PORTRAIT));
    }
}
