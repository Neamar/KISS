
package fr.neamar.kiss;

import fr.neamar.R;

import android.support.test.rule.ActivityTestRule;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.action.ViewActions.typeText;

@LargeTest
public class DeckardEspressoTest  {


    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testActivityShouldHaveText() {
        onView(withId(R.id.searchEditText)).perform(typeText("Test"));
        onView(withId(R.id.searchEditText)).check(matches(withText("Test")));
    }
}