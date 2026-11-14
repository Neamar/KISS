package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import android.content.Context;
import android.widget.ImageView;

import androidx.test.filters.LargeTest;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.PojoWithTags;

@LargeTest
public class AppListTest extends AbstractMainActivityTest {
    private static final String TAG = "zzqtag";

    private void openAppList() {
        onView(withId(R.id.launcherButton)).perform(click());
        waitForSearch();
    }

    private void setPref(String key, boolean value) {
        scenario.onActivity(activity -> activity.prefs.edit().putBoolean(key, value).commit());
    }

    private void checkFirstAppIconHidden(boolean hidden) {
        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0)
                .onChildView(withId(R.id.item_app_icon))
                .check((view, e) -> assertThat(((ImageView) view).getDrawable(), hidden ? nullValue() : notNullValue()));
    }

    @Test
    public void testIconsDisplayed() {
        openAppList();
        checkFirstAppIconHidden(false);
    }

    @Test
    public void testIconsHidden() {
        setPref("icons-hide", true);
        scenario.recreate();
        openAppList();
        checkFirstAppIconHidden(true);
    }

    private static void setTags(Context context, PojoWithTags app, String tags) {
        app.setTags(tags);
        KissApplication.getApplication(context).getDataHandler().getTagsHandler().setTags(app.id, tags);
    }

    /**
     * Tags the first app of the list, then runs the test with the app list closed.
     * Tags live in memory and in the database, which outlive the activity: remove it afterwards.
     */
    private void withTagOnFirstApp(boolean tagsVisible, Runnable test) {
        setPref("tags-visible", tagsVisible);
        openAppList();
        AtomicReference<PojoWithTags> app = new AtomicReference<>();
        scenario.onActivity(activity -> {
            app.set((PojoWithTags) activity.adapter.getItem(0).getPojo());
            setTags(activity, app.get(), TAG);
        });
        try {
            onView(withId(R.id.whiteLauncherButton)).perform(click());
            test.run();
        } finally {
            scenario.onActivity(activity -> setTags(activity, app.get(), null));
        }
    }

    private void checkFirstAppTagDisplayed(boolean displayed) {
        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0)
                .onChildView(withId(R.id.item_app_tag))
                .check(matches(displayed ? allOf(isDisplayed(), withText(TAG)) : not(isDisplayed())));
    }

    @Test
    public void testTagDisplayedAndSearchable() {
        withTagOnFirstApp(true, () -> {
            openAppList();
            checkFirstAppTagDisplayed(true);
            search(TAG);
            onView(allOf(withId(R.id.item_app_tag), withText(TAG))).check(matches(isDisplayed()));
        });
    }

    @Test
    public void testTagHiddenButSearchable() {
        withTagOnFirstApp(false, () -> {
            openAppList();
            checkFirstAppTagDisplayed(false);
            search(TAG);
            // Matching tags are displayed even when tags are hidden, to explain the match
            onView(allOf(withId(R.id.item_app_tag), withText(TAG))).check(matches(isDisplayed()));
        });
    }
}
