package fr.neamar.kiss.androidTest;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Process;
import android.view.WindowManager;

import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.lifecycle.ActivityLifecycleCallback;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.SettingsActivity;
import fr.neamar.kiss.SettingsFragment;
import fr.neamar.kiss.dataprovider.ProviderName;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.searcher.ApplicationsSearcher;
import fr.neamar.kiss.searcher.QuerySearcher;
import fr.neamar.kiss.searcher.SearchHandler;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.Permission;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
public class PinnedShortcutsOnlyTest {
    private static final String ENABLED = "enable-shortcuts";
    private static final String PINNED_ONLY = "pinned-shortcuts-only";
    private SharedPreferences prefs;
    private final Map<String, Boolean> originalPreferences = new HashMap<>();
    private ActivityScenario<MainActivity> launcher;
    private DataHandler data;
    // Apply before onStart, including recreated activities. A sleeping/locked phone
    // otherwise leaves Settings STOPPED and its FragmentManager state saved.
    private final ActivityLifecycleCallback keepSettingsAwake = (activity, stage) -> {
        if (!(activity instanceof SettingsActivity) && !(activity instanceof MainActivity)) {
            return;
        }
        if (stage == Stage.CREATED) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(true);
                activity.setTurnScreenOn(true);
            } else {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        }
    };

    @Before
    public void savePreferences() {
        Context context = getInstrumentation().getTargetContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (String key : Arrays.asList(ENABLED, PINNED_ONLY, "enable-app", "enable-contacts")) {
            originalPreferences.put(key, prefs.contains(key) ? prefs.getBoolean(key, false) : null);
        }
        getInstrumentation().runOnMainSync(() ->
                ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(keepSettingsAwake));
        launcher = ActivityScenario.launch(MainActivity.class);
        launcher.onActivity(activity -> {
            data = KissApplication.getApplication(activity).getDataHandler();
            // Start the shortcut service through real preference notifications while foreground.
            prefs.edit().putBoolean(ENABLED, false).apply();
            prefs.edit().putBoolean(ENABLED, true).putBoolean(PINNED_ONLY, false).apply();
        });
    }

    @After
    public void restorePreferences() {
        try {
            getInstrumentation().runOnMainSync(() -> {
                ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(keepSettingsAwake);
                if (prefs != null) {
                    SharedPreferences.Editor editor = prefs.edit();
                    for (Map.Entry<String, Boolean> entry : originalPreferences.entrySet()) {
                        if (entry.getValue() == null) {
                            editor.remove(entry.getKey());
                        } else {
                            editor.putBoolean(entry.getKey(), entry.getValue());
                        }
                    }
                    editor.apply();
                }
            });
        } finally {
            if (launcher != null) {
                launcher.close();
            }
        }
    }

    private SettingsFragment fragment(SettingsActivity activity) {
        activity.getSupportFragmentManager().executePendingTransactions();
        SettingsFragment fragment = (SettingsFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(SettingsActivity.ARG_SHOW_FRAGMENT);
        assertNotNull(fragment);
        return fragment;
    }

    private ActivityScenario<SettingsActivity> launchProvidersSettings() {
        ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class);
        try {
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> {
                assertEquals("Settings must be foreground before navigation",
                        Lifecycle.State.RESUMED, activity.getLifecycle().getCurrentState());
                assertFalse("Settings must not have saved fragment state before navigation",
                        activity.getSupportFragmentManager().isStateSaved());
                SettingsFragment root = fragment(activity);
                assertTrue("Root settings fragment must be resumed", root.isResumed());
                PreferenceScreen providers = root.findPreference("providers");
                assertNotNull(providers);
                assertTrue(activity.onPreferenceStartScreen(root, providers));
                activity.getSupportFragmentManager().executePendingTransactions();
            });
            getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                SettingsFragment visible = fragment(activity);
                assertEquals("providers", visible.getPreferenceScreen().getKey());
                assertTrue("Providers fragment must be resumed", visible.isResumed());
            });
            return scenario;
        } catch (RuntimeException | AssertionError error) {
            scenario.close();
            throw error;
        }
    }

    @Test
    public void testDisablingShortcutsClearsChildAndReenablingKeepsItOff() {
        try (ActivityScenario<SettingsActivity> scenario = launchProvidersSettings()) {
            scenario.onActivity(activity -> {
                SettingsFragment fragment = fragment(activity);
                SwitchPreference parent = fragment.findPreference(ENABLED);
                SwitchPreference child = fragment.findPreference(PINNED_ONLY);
                assertNotNull(parent);
                assertNotNull(child);
                assertTrue(parent.isChecked());
                assertFalse(child.isChecked());
                assertTrue(child.isEnabled());
                child.performClick();
                assertTrue(child.isChecked());
                assertTrue(prefs.getBoolean(PINNED_ONLY, false));
                parent.performClick();
                assertFalse(parent.isChecked());
                assertFalse(child.isChecked());
                assertFalse(child.isEnabled());
                assertFalse(prefs.getBoolean(PINNED_ONLY, true));
                child.performClick();
                assertFalse(child.isChecked());
                parent.performClick();
                assertTrue(parent.isChecked());
                assertTrue(child.isEnabled());
                assertFalse(child.isChecked());
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                SwitchPreference child = fragment(activity).findPreference(PINNED_ONLY);
                assertNotNull(child);
                assertTrue(child.isEnabled());
                assertFalse(child.isChecked());
            });
        }
    }

    private void assertPinnedOnlyState(SettingsActivity activity, boolean enabled, boolean checked) {
        SwitchPreference child = fragment(activity).findPreference(PINNED_ONLY);
        assertNotNull(child);
        assertEquals(enabled, child.isEnabled());
        assertEquals(checked, child.isChecked());
        assertEquals(checked, prefs.getBoolean(PINNED_ONLY, false));
    }

    @Test
    public void testOpeningSettingsClearsStaleChild() {
        getInstrumentation().runOnMainSync(() -> {
            prefs.edit().putBoolean(ENABLED, false).apply();
            prefs.edit().putBoolean(PINNED_ONLY, true).apply();
        });
        try (ActivityScenario<SettingsActivity> scenario = launchProvidersSettings()) {
            scenario.onActivity(activity -> assertPinnedOnlyState(activity, false, false));
        }
    }

    @Test
    public void testResumingSettingsClearsStaleChild() {
        try (ActivityScenario<SettingsActivity> scenario = launchProvidersSettings()) {
            scenario.moveToState(Lifecycle.State.STARTED);
            getInstrumentation().runOnMainSync(() -> {
                prefs.edit().putBoolean(ENABLED, false).apply();
                prefs.edit().putBoolean(PINNED_ONLY, true).apply();
            });
            assertTrue("Stale preference fixture must be present", prefs.getBoolean(PINNED_ONLY, false));
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> assertPinnedOnlyState(activity, false, false));
        }
    }

    private void assertOnMain(Runnable assertion) {
        // Keep assertion failures in the test thread instead of throwing them
        // through Android's main looper.
        FutureTask<Void> task = new FutureTask<>(assertion, null);
        getInstrumentation().runOnMainSync(task);
        try {
            task.get();
        } catch (ExecutionException failure) {
            if (failure.getCause() instanceof AssertionError) {
                throw (AssertionError) failure.getCause();
            }
            throw new AssertionError("Main-thread test operation failed", failure.getCause());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    private void awaitOnMain(String message, Runnable assertion) {
        long deadline = android.os.SystemClock.uptimeMillis() + 15000;
        AssertionError lastFailure = null;
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            getInstrumentation().waitForIdleSync();
            try {
                assertOnMain(assertion);
                return;
            } catch (AssertionError failure) {
                lastFailure = failure;
            }
            android.os.SystemClock.sleep(50);
        }
        throw new AssertionError(message, lastFailure);
    }

    private void checkLauncherResults(ShortcutFixture fixture, boolean shortcutsExpected,
                                      boolean allApplications) throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.moveToState(Lifecycle.State.RESUMED);
            if (shortcutsExpected) {
                awaitOnMain("Shortcut fixtures did not load", () -> fixture.assertLoaded(
                        KissApplication.getApplication(getInstrumentation().getTargetContext()).getDataHandler()));
            }
            CountDownLatch finished = new CountDownLatch(1);
            AtomicBoolean cancelled = new AtomicBoolean();
            scenario.onActivity(activity -> {
                SearchHandler.getInstance().cancelSearch();
                Searcher search = allApplications ? new ApplicationsSearcher(activity, false)
                        : new QuerySearcher(activity, fixture.prefix, false);
                search.setSearchDoneCallback((completed, wasCancelled) -> {
                    SearchHandler.getInstance().cancelSearch();
                    cancelled.set(wasCancelled);
                    finished.countDown();
                });
                search.executeOnExecutor(Searcher.SEARCH_THREAD);
            });
            assertTrue("Launcher search did not finish", finished.await(15, TimeUnit.SECONDS));
            assertFalse("Launcher search was cancelled", cancelled.get());
            scenario.onActivity(activity -> {
                int fixtureCount = 0;
                for (int i = 0; i < activity.adapter.getCount(); i++) {
                    if (activity.adapter.getItem(i).getPojo() instanceof ShortcutPojo) {
                        ShortcutPojo pojo = (ShortcutPojo) activity.adapter.getItem(i).getPojo();
                        if (!shortcutsExpected) {
                            throw new AssertionError("Shortcut remained visible with parent off: " + pojo.id);
                        }
                        if (pojo.isOreoShortcut() && (fixture.pinnedId.equals(pojo.getOreoId())
                                || fixture.dynamicId.equals(pojo.getOreoId()))) {
                            fixtureCount++;
                        }
                    }
                }
                if (shortcutsExpected) {
                    assertEquals("Both fixtures must actually be displayed before disabling", 2, fixtureCount);
                }
            });
        }
    }

    @Test
    public void testRapidTogglesRemoveShortcutsFromLauncherResults() throws Exception {
        try (ShortcutFixture fixture = new ShortcutFixture()) {
            checkLauncherResults(fixture, true, false);
            try (ActivityScenario<SettingsActivity> scenario = launchProvidersSettings()) {
                scenario.onActivity(activity -> {
                    SettingsFragment visible = fragment(activity);
                    SwitchPreference parent = visible.findPreference(ENABLED);
                    SwitchPreference child = visible.findPreference(PINNED_ONLY);
                    assertNotNull(parent);
                    assertNotNull(child);
                    for (int i = 0; i < 4; i++) {
                        child.performClick();
                        assertTrue(child.isChecked());
                        parent.performClick();
                        assertFalse(child.isChecked());
                        assertFalse(child.isEnabled());
                        parent.performClick();
                        assertFalse(child.isChecked());
                        assertTrue(child.isEnabled());
                    }
                    child.performClick();
                    child.performClick();
                    parent.performClick();
                    assertFalse(parent.isChecked());
                    assertFalse(child.isChecked());
                    assertFalse(child.isEnabled());
                    assertFalse(prefs.getBoolean(ENABLED, true));
                    assertFalse(prefs.getBoolean(PINNED_ONLY, true));
                });
            }
            getInstrumentation().waitForIdleSync();
            assertOnMain(() -> {
                assertNull("Shortcut provider remained accessible after disabling", data.getShortcutsProvider());
                List<ShortcutPojo> pinned = data.getPinnedShortcuts();
                assertTrue("App list still receives shortcuts", pinned == null || pinned.isEmpty());
            });
            checkLauncherResults(fixture, false, false);
            checkLauncherResults(fixture, false, true);
        }
    }

    @Test
    public void testDisablingShortcutsHidesCachedRecords() {
        try (ShortcutFixture fixture = new ShortcutFixture()) {
            awaitOnMain("Shortcut fixtures did not load", () -> fixture.assertLoaded(data));
            assertOnMain(() -> {
                ShortcutsProvider cached = data.getShortcutsProvider();
                assertNotNull(cached);
                ShortcutPojo pinned = cached.getPojos().stream()
                        .filter(p -> p.isOreoShortcut() && fixture.pinnedId.equals(p.getOreoId()))
                        .findFirst().orElse(null);
                assertNotNull(pinned);
                prefs.edit().putBoolean(ENABLED, false).apply();
                assertFalse(prefs.getBoolean(PINNED_ONLY, true));
                assertTrue("A cached provider still returns records", cached.getPojos().isEmpty());
                assertTrue(cached.getPinnedShortcuts().isEmpty());
                assertNull("History/favorites can still resolve the shortcut", cached.findById(pinned.id));
                assertNull(data.getPojo(pinned.id));
            });
        }
    }

    @Test
    public void testServiceProvidersCanBeDisabledAndReenabled() {
        Context context = getInstrumentation().getTargetContext();
        for (ProviderName name : Arrays.asList(ProviderName.APPS, ProviderName.CONTACTS, ProviderName.SHORTCUTS)) {
            // Enabling contacts without permission would open a dialog unrelated to this test.
            if (name == ProviderName.CONTACTS && !Permission.checkPermission(context, Permission.PERMISSION_READ_CONTACTS)) {
                continue;
            }
            String key = "enable-" + name.getSettingName();
            assertOnMain(() -> {
                prefs.edit().putBoolean(key, false).apply();
                prefs.edit().putBoolean(key, true).apply();
                prefs.edit().putBoolean(key, false).apply();
                assertNull("Disabled provider remained accessible: " + name, data.getProvider(name));
                prefs.edit().putBoolean(key, true).apply();
            });
            awaitOnMain("Provider failed to reconnect: " + name, () -> assertNotNull(data.getProvider(name)));
        }
    }

    private final class ShortcutFixture implements AutoCloseable {
        final Context context = getInstrumentation().getTargetContext();
        final LauncherApps launcher = context.getSystemService(LauncherApps.class);
        final ShortcutManager manager = context.getSystemService(ShortcutManager.class);
        final String packageName = context.getPackageName();
        final String prefix = "kissregression" + System.nanoTime();
        final String pinnedId = prefix + "pinned";
        final String dynamicId = prefix + "dynamic";
        final List<String> originalPinned;
        final boolean hadExcluded = prefs.contains("excluded-apps");
        final boolean hadExcludedShortcuts = prefs.contains(DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS);
        final Set<String> excluded = new HashSet<>(prefs.getStringSet("excluded-apps", Collections.emptySet()));
        final Set<String> excludedShortcuts = new HashSet<>(prefs.getStringSet(
                DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS, Collections.emptySet()));

        ShortcutFixture() {
            assertNotNull(launcher);
            assertNotNull(manager);
            assertTrue("KISS must be the default launcher", launcher.hasShortcutHostPermission());
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery()
                    .setPackage(packageName).setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
            List<ShortcutInfo> pinned = launcher.getShortcuts(query, Process.myUserHandle());
            originalPinned = pinned == null ? Collections.emptyList()
                    : pinned.stream().map(ShortcutInfo::getId).collect(Collectors.toList());
            try {
                // KISS excludes itself by default. Only allow our package for this fixture,
                // keeping all other exclusions and restoring these keys afterward.
                getInstrumentation().runOnMainSync(() -> {
                    DataHandler data = KissApplication.getApplication(context).getDataHandler();
                    Set<String> allowed = data.getExcluded();
                    allowed.remove(packageName + "/" + MainActivity.class.getName());
                    Set<String> allowedShortcuts = data.getExcludedShortcutApps();
                    allowedShortcuts.remove(packageName);
                    prefs.edit().putStringSet("excluded-apps", allowed)
                            .putStringSet(DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS, allowedShortcuts).apply();
                });
                assertTrue("Unable to publish fixture shortcuts", manager.addDynamicShortcuts(
                        Arrays.asList(shortcut(pinnedId), shortcut(dynamicId))));
                List<String> pins = new ArrayList<>(originalPinned);
                pins.add(pinnedId);
                launcher.pinShortcuts(packageName, pins, Process.myUserHandle());
                getInstrumentation().runOnMainSync(() ->
                        KissApplication.getApplication(context).getDataHandler().reload(ProviderName.SHORTCUTS));
            } catch (RuntimeException | AssertionError failure) {
                close();
                throw failure;
            }
        }

        private ShortcutInfo shortcut(String id) {
            return new ShortcutInfo.Builder(context, id).setShortLabel(id)
                    .setActivity(new ComponentName(context, MainActivity.class))
                    .setIntent(new Intent(context, MainActivity.class).setAction(Intent.ACTION_MAIN)).build();
        }

        List<String> records() {
            return ShortcutUtil.getShortcuts(context, packageName).stream().map(ShortcutInfo::getId)
                    .filter(id -> pinnedId.equals(id) || dynamicId.equals(id)).collect(Collectors.toList());
        }

        void assertLoaded(DataHandler data) {
            ShortcutsProvider provider = data.getShortcutsProvider();
            assertNotNull(provider);
            assertTrue(provider.isLoaded());
            assertTrue("Other providers must finish before testing the displayed results", data.isAllProvidersLoaded());
            assertEquals(2, provider.getPojos().stream().filter(p -> p.isOreoShortcut()
                    && (pinnedId.equals(p.getOreoId()) || dynamicId.equals(p.getOreoId()))).count());
        }

        @Override
        public void close() {
            try {
                launcher.pinShortcuts(packageName, originalPinned, Process.myUserHandle());
                manager.removeDynamicShortcuts(Arrays.asList(pinnedId, dynamicId));
            } finally {
                getInstrumentation().runOnMainSync(() -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    if (hadExcluded) editor.putStringSet("excluded-apps", excluded);
                    else editor.remove("excluded-apps");
                    if (hadExcludedShortcuts) editor.putStringSet(DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS, excludedShortcuts);
                    else editor.remove(DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS);
                    editor.apply();
                    KissApplication.getApplication(context).getDataHandler().reload(ProviderName.SHORTCUTS);
                });
            }
        }
    }

    @Test
    public void testQueryFilteringKeepsPinningLookupAvailable() {
        Context context = getInstrumentation().getTargetContext();
        try (ShortcutFixture fixture = new ShortcutFixture()) {
            awaitOnMain("Shortcut fixtures did not load", () -> fixture.assertLoaded(data));
            assertEquals(2, fixture.records().size());
            getInstrumentation().runOnMainSync(() -> prefs.edit().putBoolean(PINNED_ONLY, true).apply());
            assertEquals(Collections.singletonList(fixture.pinnedId), fixture.records());
            for (ShortcutInfo shortcut : ShortcutUtil.getAllShortcuts(context)) {
                assertTrue("Unpinned shortcut leaked from another package/profile", shortcut.isPinned());
            }
            ShortcutInfo dynamic = ShortcutUtil.getShortCut(context, Process.myUserHandle(),
                    context.getPackageName(), fixture.dynamicId);
            assertNotNull("Single shortcut lookup must still allow pinning", dynamic);
            assertFalse(ShortcutUtil.isShortcutVisible(context, dynamic, Collections.emptySet(), Collections.emptySet()));
            ShortcutInfo pinned = ShortcutUtil.getShortCut(context, Process.myUserHandle(),
                    context.getPackageName(), fixture.pinnedId);
            assertNotNull(pinned);
            assertTrue(ShortcutUtil.isShortcutVisible(context, pinned, Collections.emptySet(), Collections.emptySet()));
            getInstrumentation().runOnMainSync(() -> prefs.edit().putBoolean(PINNED_ONLY, false).apply());
            assertEquals(2, fixture.records().size());
            assertTrue(ShortcutUtil.isShortcutVisible(context, dynamic, Collections.emptySet(), Collections.emptySet()));
        }
    }
}
