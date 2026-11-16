package fr.neamar.kiss.androidTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

import org.junit.Test;

import fr.neamar.kiss.db.DBHelper;

@LargeTest
public class MiscSettingsTest extends AbstractMainActivityTest {
    private void checkRateAppDisplayed(boolean displayed) {
        inSettings(() ->
                // It is the last preference: it may not be bound to a view, so don't look for one
                assertThat(fromSettings(activity -> getScreen(activity).findPreference("rate-app")), displayed ? notNullValue() : nullValue()));
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
