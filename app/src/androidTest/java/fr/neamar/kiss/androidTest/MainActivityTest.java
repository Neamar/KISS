package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

import org.junit.Test;

import java.util.Collections;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ProviderName;
import fr.neamar.kiss.db.DBHelper;

@LargeTest
public class MainActivityTest extends AbstractMainActivityTest {
    @Test
    public void testHintDisplayed() {
        onView(withId(R.id.searchEditText)).check(matches(withHint(R.string.ui_search_hint)));
    }

    @Test
    public void testCanTypeTextIntoSearchBox() {
        search("Test");
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
        search("Test");

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
        search("blahblah");
        onView(withId(R.id.item_search_icon)).check(matches(isDisplayed()));
        String expected = ApplicationProvider.getApplicationContext().getString(R.string.ui_item_search, "Google", "blahblah");
        onView(withId(R.id.item_search_text)).check(matches(withText(expected)));
    }

    @Test
    public void testCalculatorAppears() {
        search("1 + 1");
        onView(allOf(withId(R.id.item_search_text), withText("1+1 = 2"))).check(matches(isDisplayed()));
    }

    @Test
    public void testShortcutsInAppMenu() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        // Publish a shortcut on KISS itself, so the test doesn't depend on the apps of the image
        Context context = ApplicationProvider.getApplicationContext();
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        shortcutManager.setDynamicShortcuts(Collections.singletonList(new ShortcutInfo.Builder(context, "test")
                .setShortLabel("Test shortcut")
                .setIntent(new Intent(Intent.ACTION_MAIN, null, context, MainActivity.class))
                .build()));
        // KISS excludes itself by default
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", Collections.emptySet()).commit();
        try {
            // Don't wait for the system callback, the idling resource then waits for this reload
            scenario.onActivity(activity -> KissApplication.getApplication(activity).getDataHandler().reload(ProviderName.APPS, ProviderName.SHORTCUTS));
            String appName = context.getString(R.string.app_name);
            search(appName);
            // Unpinned shortcuts are not searchable, they are listed in the menu of their app
            onView(withText(appName + ": Test shortcut")).check(doesNotExist());
            onView(allOf(withId(R.id.item_app_action_shortcuts), hasSibling(hasDescendant(allOf(withId(R.id.item_app_name), withText(appName)))))).perform(click());
            onView(withText("Test shortcut")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
            pressBack();

            // Long press shows the shortcuts too, with the usual app menu behind "App options"
            onView(allOf(withId(R.id.item_app_name), withText(appName))).perform(longClick());
            onView(withText("Test shortcut")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
            onView(withText(R.string.menu_app_options)).inRoot(isPlatformPopup()).perform(click());
            onView(withText(R.string.menu_app_details)).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
        } finally {
            // Providers outlive the test: don't leave KISS and its shortcut in them
            shortcutManager.removeAllDynamicShortcuts();
            PreferenceManager.getDefaultSharedPreferences(context).edit().remove("excluded-apps").commit();
            scenario.onActivity(activity -> KissApplication.getApplication(activity).getDataHandler().reload(ProviderName.APPS, ProviderName.SHORTCUTS));
        }
    }

    @Test
    public void testBatterySettingAppears() {
        search("batter");
        // "Battery saver" matches too, pick the Battery row
        onView(allOf(withId(R.id.item_setting_icon), hasSibling(withText("Battery")))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.item_setting_name), withText("Battery"))).check(matches(isDisplayed()));
    }

    @Test
    public void testEnterLaunchesFirstResult() {
        search("0123456789");
        onView(withId(R.id.searchEditText)).perform(pressImeActionButton());
        intended(allOf(hasAction(Intent.ACTION_CALL), hasData("tel:0123456789")));
    }

    @Test
    public void testKissBarDisplayed() {
        // Sanity check
        onView(withId(R.id.mainKissbar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.item_app_name)).check(doesNotExist());

        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.mainKissbar)).check(matches(isDisplayed()));
        // Installed apps differ between images, so only check the list starts with an app
        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0)
                .onChildView(withId(R.id.item_app_name)).check(matches(isDisplayed()));
    }

    @Test
    public void testHistoryDisplayedWhenSearchIsEmpty() {
        DBHelper.insertHistory(ApplicationProvider.getApplicationContext(), "", "phone://0123456789");
        scenario.recreate();
        waitForSearch();
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));

        search("blahblah");
        onView(withId(R.id.item_phone_text)).check(doesNotExist());

        onView(withId(R.id.clearButton)).perform(click());
        waitForSearch();
        onView(withId(R.id.item_phone_text)).check(matches(isDisplayed()));
    }

    @Test
    public void testMenuOpensAndBackDismissesIt() {
        onView(withId(R.id.menuButton)).perform(click());
        onView(withText(R.string.menu_settings)).check(matches(isDisplayed()));

        pressBack();
        onView(withText(R.string.menu_settings)).check(doesNotExist());
        onView(withId(R.id.menuButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testMenuOpensOnLongClick() {
        onView(withId(R.id.menuButton)).perform(longClick());
        onView(withText(R.string.menu_settings)).check(matches(isDisplayed()));
    }

    @Test
    public void testResultLongClickDisplaysContextMenu() {
        search("blahblah");
        onView(withId(R.id.item_search_text)).perform(longClick());
        onView(withText(R.string.share)).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
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
        search("Test");
        onView(withId(R.id.searchEditText)).check(matches(withText("Test")));

        onView(withId(R.id.launcherButton)).perform(click());

        onView(withId(R.id.mainKissbar)).check(matches(isDisplayed()));

        onView(withId(R.id.whiteLauncherButton)).perform(click());

        onView(withId(R.id.searchEditText)).check(matches(withText("")));
    }
}
