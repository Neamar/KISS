package fr.neamar.kiss.androidTest;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.os.Build;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Before;
import org.junit.Rule;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;

abstract class AbstractMainActivityTest {
    @Rule
    public ActivityScenarioRule<MainActivity> mActivityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        mActivityRule.getScenario().onActivity(activity -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldHaveContactPermission()) {
                    getInstrumentation().getUiAutomation().executeShellCommand(
                            "pm grant " + activity.getPackageName()
                                    + " android.permission.READ_CONTACTS");
                } else {
                    getInstrumentation().getUiAutomation().executeShellCommand(
                            "pm revoke " + activity.getPackageName()
                                    + " android.permission.READ_CONTACTS");
                }
            }

            // Initialize to default preferences
            KissApplication.getApplication(activity).getDataHandler().clearHistory();
            assertThat(PreferenceManager.getDefaultSharedPreferences(activity).edit().clear().commit(), is(true));
            PreferenceManager.setDefaultValues(activity, R.xml.preferences, true);

            // Remove lock screen
            Runnable wakeUpDevice = () -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            activity.runOnUiThread(wakeUpDevice);
        });
    }

    protected boolean shouldHaveContactPermission() {
        return true;
    }
}
