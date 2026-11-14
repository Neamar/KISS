package fr.neamar.kiss.androidTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.Manifest;
import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.io.IOException;
import java.io.InputStream;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;

abstract class AbstractMainActivityTest {
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS);

    protected ActivityScenario<MainActivity> scenario;
    private final IdlingResource providersLoaded = new ProvidersLoadedIdlingResource();

    @BeforeClass
    public static void setDefaultLauncher() throws IOException {
        // Only the default launcher can read other apps shortcuts
        Context context = ApplicationProvider.getApplicationContext();
        ParcelFileDescriptor output = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "cmd package set-home-activity " + context.getPackageName() + "/" + MainActivity.class.getName());
        // Wait for the command to complete
        try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(output)) {
            while (in.read() != -1) ;
        }
    }

    @Before
    public void setUp() {
        // Reset to default preferences before launching, so every test starts from the same state
        Context context = ApplicationProvider.getApplicationContext();
        DBHelper.clearHistory(context);
        assertThat(PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit(), is(true));
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);

        IdlingRegistry.getInstance().register(providersLoaded);
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        scenario.close();
        IdlingRegistry.getInstance().unregister(providersLoaded);
    }

    /**
     * Providers load through service binding and can reload at any time (e.g. shortcuts changed),
     * which Espresso can't see. Meanwhile the launcher button is hidden behind the loader.
     */
    private static class ProvidersLoadedIdlingResource implements IdlingResource {
        private ResourceCallback callback;

        @Override
        public String getName() {
            return "ProvidersLoaded";
        }

        @Override
        public boolean isIdleNow() {
            Context context = ApplicationProvider.getApplicationContext();
            boolean idle = KissApplication.getApplication(context).getDataHandler().isAllProvidersLoaded();
            if (idle && callback != null) {
                callback.onTransitionToIdle();
            }
            return idle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            this.callback = callback;
        }
    }
}
