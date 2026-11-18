package fr.neamar.kiss.androidTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.lifecycle.ActivityLifecycleCallback;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.SettingsActivity;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.searcher.QuerySearcher;
import fr.neamar.kiss.searcher.SearchHandler;
import fr.neamar.kiss.searcher.Searcher;

abstract class AbstractMainActivityTest {
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE);

    protected ActivityScenario<MainActivity> scenario;
    private final IdlingResource providersLoaded = new ProvidersLoadedIdlingResource();
    protected final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    @BeforeClass
    public static void setDefaultLauncher() throws IOException {
        // Only the default launcher can read other apps shortcuts
        Context context = ApplicationProvider.getApplicationContext();
        runShellCommand("cmd package set-home-activity " + context.getPackageName() + "/" + MainActivity.class.getName());
    }

    private static void runShellCommand(String command) throws IOException {
        ParcelFileDescriptor output = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
        // Wait for the command to complete
        try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(output)) {
            while (in.read() != -1) ;
        }
    }

    @Before
    public void setUp() throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // After boot, the system keeps changing packages and their shortcuts for minutes, which reloads providers.
            // Start once it has caught up, so tests are less likely to run in the middle of these reloads.
            runShellCommand("timeout 60 am wait-for-broadcast-idle");
        }
        // Reset to default preferences before launching, so every test starts from the same state
        Context context = ApplicationProvider.getApplicationContext();
        DBHelper.clearHistory(context);
        assertThat(PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit(), is(true));
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
        // The max result count is cached statically and would otherwise leak between tests
        QuerySearcher.clearMaxResultCountCache();

        IdlingRegistry.getInstance().register(providersLoaded);
        scenario = ActivityScenario.launch(MainActivity.class);

        // Initialized after the launch so it isn't stubbed: clicked results never leave KISS
        stubIntents();
    }

    private static void stubIntents() {
        Intents.init();
        intending(anyIntent()).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    /**
     * Pressing home doesn't reuse the activity started by the test, but creates a new home one:
     * close it first, so that the next presses go through onNewIntent() as for users.
     * {@link #scenario} can't be used anymore.
     */
    protected void startAsHome() {
        // The stub would also catch the activity used by ActivityScenario to close
        Intents.release();
        scenario.close();
        stubIntents();
        pressHome();
    }

    protected void pressHome() {
        CountDownLatch resumed = new CountDownLatch(1);
        ActivityLifecycleCallback callback = (activity, stage) -> {
            if (stage == Stage.RESUMED) {
                resumed.countDown();
            }
        };
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(callback);
        try {
            device.pressHome();
            assertThat(resumed.await(10, TimeUnit.SECONDS), is(true));
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } finally {
            ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(callback);
        }
        waitForSearch();
    }

    protected void search(String query) {
        onView(withId(R.id.searchEditText)).perform(typeText(query));
        // The keyboard sends the typed text asynchronously: it may reach KISS after Espresso is idle
        assertThat(device.wait(d -> query.trim().equals(getLastSearchQuery()), 5000), is(true));
        waitForSearch();
    }

    private static String getLastSearchQuery() {
        AtomicReference<String> query = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> query.set(SearchHandler.getInstance().getLastSearchQuery()));
        return query.get();
    }

    /**
     * Searches run on their own executor, which Espresso doesn't track:
     * wait for it to drain, Espresso then waits for the results to be posted to the UI.
     */
    protected static void waitForSearch() {
        try {
            Searcher.SEARCH_THREAD.submit(() -> {
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError("Search did not complete", e);
        }
    }

    /**
     * Launching a result clears the search after a delay Espresso doesn't wait for:
     * queue behind it on the main thread, then wait for the history it displays.
     */
    protected static void waitForLaunchOccurred() {
        CountDownLatch latch = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).postDelayed(latch::countDown, KissApplication.TOUCH_DELAY * 3);
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS), is(true));
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        waitForSearch();
    }

    /**
     * Opens the settings from KISS, as users do, runs the test, then comes back to KISS.
     * ActivityScenario can't be used: it would clear the task, destroying KISS.
     */
    protected void inSettings(Runnable test) {
        // The stub would also catch the settings activity: it must not be active while launching it
        Intents.release();
        try {
            scenario.onActivity(activity -> activity.startActivity(new Intent(activity, SettingsActivity.class)));
            // Espresso would look for views in KISS until the settings are focused
            assertThat(device.wait(Until.hasObject(By.res(ApplicationProvider.getApplicationContext().getPackageName(), "content_container")), 10_000), is(true));
            test.run();
        } finally {
            fromSettings(activity -> {
                activity.finish();
                return null;
            });
            // Released again by tearDown()
            Intents.init();
        }
        // Espresso could pick the closing settings window
        assertThat(device.wait(Until.hasObject(By.res(ApplicationProvider.getApplicationContext().getPackageName(), "searchEditText")), 10_000), is(true));
    }

    /**
     * Runs on the main thread with the displayed settings, which may have been recreated
     */
    protected static <T> T fromSettings(Function<SettingsActivity, T> function) {
        AtomicReference<T> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                if (activity instanceof SettingsActivity) {
                    result.set(function.apply((SettingsActivity) activity));
                }
            }
        });
        return result.get();
    }

    protected static PreferenceFragmentCompat getScreen(SettingsActivity activity) {
        return (PreferenceFragmentCompat) activity.getSupportFragmentManager().findFragmentById(R.id.content_container);
    }

    @After
    public void tearDown() {
        Intents.release();
        scenario.close();
        IdlingRegistry.getInstance().unregister(providersLoaded);
    }

    /**
     * Providers load through service binding and can reload at any time (e.g. shortcuts changed),
     * which Espresso can't see. Meanwhile the launcher button is hidden behind the loader.
     * A finished reload then refreshes the results on the search thread, which Espresso doesn't track either.
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
            boolean idle = KissApplication.getApplication(context).getDataHandler().isAllProvidersLoaded()
                    && !SearchHandler.getInstance().isSearchRunning();
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
