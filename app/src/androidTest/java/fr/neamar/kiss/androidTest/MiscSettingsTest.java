package fr.neamar.kiss.androidTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.filters.LargeTest;

import org.junit.Test;

import fr.neamar.kiss.SettingsActivity;
import fr.neamar.kiss.db.DBHelper;

@LargeTest
public class MiscSettingsTest extends AbstractMainActivityTest {
    private void checkRateAppDisplayed(boolean displayed) {
        // The stub would also catch the settings activity: it must not be active while launching it
        Intents.release();
        try (ActivityScenario<SettingsActivity> settings = ActivityScenario.launch(SettingsActivity.class)) {
            settings.onActivity(activity -> {
                for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
                    if (fragment instanceof PreferenceFragmentCompat) {
                        // It is the last preference: it may not be bound to a view, so don't look for one
                        assertThat(((PreferenceFragmentCompat) fragment).findPreference("rate-app"), displayed ? notNullValue() : nullValue());
                        return;
                    }
                }
                throw new AssertionError("No preference fragment");
            });
        } finally {
            // Released again by tearDown()
            Intents.init();
        }
    }

    @Test
    public void testRateAppHiddenWithShortHistory() {
        checkRateAppDisplayed(false);
    }

    @Test
    public void testRateAppDisplayedWithLongHistory() {
        Context context = ApplicationProvider.getApplicationContext();
        for (int i = 0; i < 301; i++) {
            DBHelper.insertHistory(context, "", "phone://" + i);
        }
        checkRateAppDisplayed(true);
    }
}
