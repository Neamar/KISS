package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

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
    public void testKeyboardOnStartEnabled() {
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
                .edit().putBoolean("display-keyboard", true).commit();
        startAsHome();
        closeSoftKeyboard();
        assertKeyboardVisible(false);

        pressHome();
        assertKeyboardVisible(true);
    }
}
