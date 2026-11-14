package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

import org.junit.Test;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.AppPojo;

@LargeTest
public class HistoryTest extends AbstractMainActivityTest {
    private void launchBatterySetting() {
        search("batter");
        onView(withText("Battery")).perform(click());
        waitForLaunchOccurred();
    }

    @Test
    public void testClickedResultIsLaunchedAndAddedToHistory() {
        launchBatterySetting();
        intended(hasAction(Intent.ACTION_POWER_USAGE_SUMMARY));
        onView(withId(R.id.searchEditText)).check(matches(withText("")));
        onView(withId(R.id.item_setting_name)).check(matches(withText("Battery")));
    }

    @Test
    public void testFreezeHistory() {
        scenario.onActivity(activity -> activity.prefs.edit().putBoolean("freeze-history", true).commit());
        launchBatterySetting();
        // The list is hidden, not emptied, when there is no history
        onView(withId(R.id.item_setting_name)).check(matches(not(isDisplayed())));

        scenario.onActivity(activity -> activity.prefs.edit().putBoolean("freeze-history", false).commit());
        launchBatterySetting();
        onView(withId(R.id.item_setting_name)).check(matches(withText("Battery")));
    }

    @Test
    public void testMaxHistoryResults() {
        Context context = ApplicationProvider.getApplicationContext();
        for (String phone : new String[]{"0123456781", "0123456782", "0123456783"}) {
            DBHelper.insertHistory(context, "", "phone://" + phone);
        }
        scenario.onActivity(activity -> activity.prefs.edit().putString("number-of-display-elements", "2").commit());
        scenario.recreate();
        waitForSearch();
        onView(withId(android.R.id.list)).check(matches(hasChildCount(2)));

        // Search is not impacted
        search("e");
        onView(withId(android.R.id.list)).check(matches(hasMinimumChildCount(3)));
    }

    @Test
    public void testExcludedAppHiddenFromAppListAndSearch() {
        Context context = ApplicationProvider.getApplicationContext();
        DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
        AppPojo settings = dataHandler.getApplications().stream()
                .filter(app -> app.packageName.equals("com.android.settings")).findFirst().get();
        scenario.onActivity(activity -> dataHandler.addToExcluded(settings));
        try {
            onView(withId(R.id.launcherButton)).perform(click());
            waitForSearch();
            scenario.onActivity(activity -> {
                assertThat(activity.adapter.isEmpty(), is(false));
                for (int i = 0; i < activity.adapter.getCount(); i++) {
                    assertThat(activity.adapter.getItem(i).getPojo().id, not(settings.id));
                }
            });
            onView(withId(R.id.whiteLauncherButton)).perform(click());

            search(settings.getName());
            onView(withId(R.id.item_search_text)).check(matches(isDisplayed()));
            onView(allOf(withId(R.id.item_app_name), withText(settings.getName()))).check(doesNotExist());
        } finally {
            // The provider outlives the activity, restore it for the next tests
            scenario.onActivity(activity -> dataHandler.removeFromExcluded(settings));
        }
    }
}
