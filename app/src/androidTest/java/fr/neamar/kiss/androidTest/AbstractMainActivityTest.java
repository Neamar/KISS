package fr.neamar.kiss.androidTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.Manifest;
import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;

abstract class AbstractMainActivityTest {
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS);

    protected ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() {
        // Reset to default preferences before launching, so every test starts from the same state
        Context context = ApplicationProvider.getApplicationContext();
        DBHelper.clearHistory(context);
        assertThat(PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit(), is(true));
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);

        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        scenario.close();
    }
}
