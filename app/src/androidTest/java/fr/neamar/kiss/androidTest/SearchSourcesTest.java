package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.text.format.DateUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

import org.junit.Test;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ProviderName;

@LargeTest
public class SearchSourcesTest extends AbstractMainActivityTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    // Simple providers read their preference on every query, so no recreate is needed
    private void disableSource(String key) {
        scenario.onActivity(activity -> activity.prefs.edit().putBoolean(key, false).commit());
    }

    // Proves the search did run, so a missing result is caused by the disabled source
    private void checkWebSearchDisplayed(String query) {
        onView(withText(context.getString(R.string.ui_item_search, "Google", query))).check(matches(isDisplayed()));
    }

    @Test
    public void testCalculatorDisabled() {
        disableSource("enable-calculator");
        search("2+2");
        checkWebSearchDisplayed("2+2");
        onView(withText("2+2 = 4")).check(doesNotExist());
    }

    @Test
    public void testTimerAppears() {
        search("5m");
        String timer = context.getString(R.string.ui_item_timer, DateUtils.formatElapsedTime(300));
        onView(withText(timer)).check(matches(isDisplayed()));
    }

    @Test
    public void testTimerDisabled() {
        disableSource("enable-timer");
        search("5m");
        checkWebSearchDisplayed("5m");
        String timer = context.getString(R.string.ui_item_timer, DateUtils.formatElapsedTime(300));
        onView(withText(timer)).check(doesNotExist());
    }

    @Test
    public void testWebSearchDisabled() {
        disableSource("enable-search");
        search("blahblah");
        onView(withId(R.id.item_search_text)).check(doesNotExist());
    }

    @Test
    public void testPhoneAppears() {
        search("0123456789");
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));
    }

    @Test
    public void testPhoneDisabled() {
        disableSource("enable-phone");
        search("0123456789");
        checkWebSearchDisplayed("0123456789");
        onView(withId(R.id.item_phone_text)).check(doesNotExist());
    }

    @Test
    public void testSettingsDisabled() {
        // The settings provider caches its items, the settings screen reloads it on change
        setSettingsEnabled(false);
        try {
            search("batter");
            checkWebSearchDisplayed("batter");
            onView(withId(R.id.item_setting_name)).check(doesNotExist());
        } finally {
            // The provider outlives the activity, restore it for the next tests
            setSettingsEnabled(true);
        }
    }

    private void setSettingsEnabled(boolean enabled) {
        scenario.onActivity(activity -> {
            activity.prefs.edit().putBoolean("enable-settings", enabled).commit();
            KissApplication.getApplication(activity).getDataHandler().reload(ProviderName.SETTINGS);
        });
    }

    @Test
    public void testMaxSearchResults() {
        scenario.onActivity(activity -> activity.prefs.edit().putString("number-of-search-results", "2").commit());
        search("e");
        onView(withId(android.R.id.list)).check(matches(hasChildCount(2)));
    }
}
