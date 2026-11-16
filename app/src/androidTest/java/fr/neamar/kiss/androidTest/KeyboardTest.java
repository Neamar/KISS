package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Until;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import fr.neamar.kiss.R;

@LargeTest
public class KeyboardTest extends AbstractMainActivityTest {
    private boolean isKeyboardVisible() {
        AtomicBoolean visible = new AtomicBoolean();
        onView(withId(R.id.searchEditText)).check((view, noViewFoundException) -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
            visible.set(insets != null && insets.isVisible(WindowInsetsCompat.Type.ime()));
        });
        return visible.get();
    }

    private void assertKeyboardVisible(boolean expected) {
        // The keyboard is animated in and out by the system
        device.waitForIdle();
        assertThat(device.wait(d -> isKeyboardVisible() == expected, 5000), is(true));
    }

    private static String getKissPackage() {
        return ApplicationProvider.getApplicationContext().getPackageName();
    }

    private void enableKeyboardOnStart() {
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
                .edit().putBoolean("display-keyboard", true).commit();
    }

    private void openSearchResult() {
        // Actually leave KISS: drop the stub that keeps every intent inside it
        Intents.release();
        Intents.init();
        search("batter");
        onView(withText("Battery")).perform(click());
        assertThat(device.wait(Until.gone(By.pkg(getKissPackage()).depth(0)), 10_000), is(true));
    }

    private void pressBackToKiss() {
        device.pressBack();
        assertThat(device.wait(Until.hasObject(By.pkg(getKissPackage()).depth(0)), 10_000), is(true));
        waitForSearch();
    }

    @Test
    public void testKeyboardOnStartDisabled() {
        startAsHome();
        pressHome();
        assertKeyboardVisible(false);

        device.pressBack();
        assertKeyboardVisible(false);

        onView(withId(R.id.launcherButton)).perform(click());
        onView(withId(R.id.whiteLauncherButton)).perform(click());
        assertKeyboardVisible(false);

        onView(withId(R.id.searchEditText)).perform(click());
        assertKeyboardVisible(true);
    }

    @Test
    public void testKeyboardOnStartDisabledAfterResultThenHome() {
        startAsHome();
        openSearchResult();
        pressHome();
        assertKeyboardVisible(false);
    }

    @Test
    public void testKeyboardOnStartDisabledAfterResultThenBack() {
        startAsHome();
        openSearchResult();
        pressBackToKiss();
        assertKeyboardVisible(false);
    }

    @Test
    public void testKeyboardOnStartEnabledAfterResultThenHome() {
        enableKeyboardOnStart();
        startAsHome();
        openSearchResult();
        pressHome();
        assertKeyboardVisible(true);
    }

    @Test
    public void testKeyboardOnStartEnabledAfterResultThenBack() {
        enableKeyboardOnStart();
        startAsHome();
        openSearchResult();
        pressBackToKiss();
        assertKeyboardVisible(true);
    }

    @Test
    public void testKeyboardOnStartEnabled() {
        enableKeyboardOnStart();
        startAsHome();
        closeSoftKeyboard();
        assertKeyboardVisible(false);

        pressHome();
        assertKeyboardVisible(true);
    }
}
