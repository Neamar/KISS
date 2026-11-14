package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

import org.junit.Test;

import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;

@LargeTest
public class MinimalisticModeTest extends AbstractMainActivityTest {
    private void enableMinimalisticMode(boolean historyOnTouch) {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean("history-hide", true)
                .putBoolean("history-onclick", historyOnTouch)
                .commit();
        DBHelper.insertHistory(context, "", "phone://0123456789");
        startAsHome();
    }

    private void assertHistoryHidden() {
        onView(withId(android.R.id.list)).check(matches(not(isDisplayed())));
    }

    private void touchEmptyArea() {
        onView(withId(android.R.id.content)).perform(click());
        waitForSearch();
    }

    @Test
    public void testHistoryNotDisplayed() {
        enableMinimalisticMode(false);
        pressHome();
        assertHistoryHidden();

        touchEmptyArea();
        assertHistoryHidden();

        onView(withId(R.id.searchEditText)).perform(click());
        waitForSearch();
        assertHistoryHidden();
    }

    @Test
    public void testHistoryOnTouchThenHome() {
        enableMinimalisticMode(true);
        touchEmptyArea();
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));

        pressHome();
        assertHistoryHidden();
    }

    @Test
    public void testHistoryOnTouchThenBack() {
        enableMinimalisticMode(true);
        touchEmptyArea();
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));

        device.pressBack();
        waitForSearch();
        assertHistoryHidden();
    }
}
