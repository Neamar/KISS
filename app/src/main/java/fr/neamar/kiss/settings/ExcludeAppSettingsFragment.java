package fr.neamar.kiss.settings;

import static fr.neamar.kiss.dataprovider.ProviderName.SHORTCUTS;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.NameComparator;
import fr.neamar.kiss.utils.Utilities;

public class ExcludeAppSettingsFragment extends BaseSettingsFragment {
    /**
     * Which exclusion list to edit, set as an extra in the preferences XML
     */
    private static final String ARG_EXCLUSION = "exclusion";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences_exclude_app);

        String exclusion = requireArguments().getString(ARG_EXCLUSION);
        if (exclusion != null) {
            switch (exclusion) {
                case "edit-excluded-apps":
                    addExcludedAppSettings();
                    break;
                case "edit-excluded-from-history-apps":
                    addExcludedFromHistoryAppSettings();
                    break;
                case "edit-excluded-app-shortcuts":
                    addExcludedShortcutAppSettings();
                    break;
            }
        }
    }

    private void addExcludedAppSettings() {
        addExcludedApps(
                app -> getDataHandler().addToExcluded(app),
                app -> getDataHandler().removeFromExcluded(app),
                AppPojo::isExcluded,
                "excluded-apps",
                app -> getDataHandler().getExcluded().contains(app.getComponentName()),
                AppPojo::setExcluded
        );
    }

    private void addExcludedFromHistoryAppSettings() {
        addExcludedApps(
                app -> getDataHandler().addToExcludedFromHistory(app),
                app -> getDataHandler().removeFromExcludedFromHistory(app),
                AppPojo::isExcludedFromHistory,
                "excluded-apps-from-history",
                app -> getDataHandler().getExcludedFromHistory().contains(app.id),
                AppPojo::setExcludedFromHistory
        );
    }

    private void addExcludedShortcutAppSettings() {
        addExcludedApps(
                app -> getDataHandler().addToExcludedShortcutApps(app),
                app -> getDataHandler().removeFromExcludedShortcutApps(app),
                AppPojo::isExcludedShortcuts,
                DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS,
                app -> getDataHandler().getExcludedShortcutApps().contains(app.packageName),
                AppPojo::setExcludedShortcuts
        );
    }

    private void addExcludedApps(
            @NonNull Consumer<AppPojo> onExcludedListener,
            @NonNull Consumer<AppPojo> onIncludedListener,
            @NonNull Function<AppPojo, Boolean> isExcludedCallback,
            @NonNull String prefKey,
            @NonNull Function<AppPojo, Boolean> isExcludedInPrefs,
            @NonNull BiConsumer<AppPojo, Boolean> setExcluded
    ) {
        List<AppPojo> appList = getDataHandler().getApplications();
        IconsHandler iconsHandler = KissApplication.getApplication(requireContext()).getIconsHandler();

        AppPojo[] apps;
        if (appList != null) {
            apps = appList.toArray(new AppPojo[0]);
        } else {
            apps = new AppPojo[0];
        }
        Arrays.sort(apps, new NameComparator());

        final PreferenceScreen excludedAppsScreen = getPreferenceScreen();
        final SwitchPreference[] switches = new SwitchPreference[apps.length];
        for (int i = 0; i < apps.length; i++) {
            switches[i] = createExcludeAppSwitch(requireContext(), iconsHandler, isExcludedCallback, apps[i], onExcludedListener, onIncludedListener);

            excludedAppsScreen.addPreference(switches[i]);
        }

        setOnDialogClosedListener("reset-excluded-list", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet(prefKey, null).apply();
                // Sync apps and switches with the default list, which may not be empty (KISS itself)
                for (int i = 0; i < apps.length; i++) {
                    boolean excluded = isExcludedInPrefs.apply(apps[i]);
                    setExcluded.accept(apps[i], excluded);
                    switches[i].setChecked(excluded);
                }
                // Shortcuts depend on both the excluded apps and the apps excluded from shortcuts
                getDataHandler().reload(SHORTCUTS);
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
            }
        });
    }

    private SwitchPreference createExcludeAppSwitch(
            @NonNull Context context,
            @NonNull IconsHandler iconsHandler,
            @NonNull Function<AppPojo, Boolean> isExcludedCallback,
            final @NonNull AppPojo app,
            @NonNull Consumer<AppPojo> onExcludedListener,
            @NonNull Consumer<AppPojo> onIncludedListener
    ) {
        final SwitchPreference switchPreference = new SwitchPreference(context);

        switchPreference.setIcon(R.drawable.ic_launcher_white);
        Utilities.runAsync((task) -> {
            final ComponentName componentName = new ComponentName(app.packageName, app.activityName);
            return iconsHandler.getDrawableIconForPackage(componentName, app.userHandle);
        }, (task, result) -> {
            if (!task.isCancelled()) {
                switchPreference.setIcon((Drawable) result);
            }
        });

        switchPreference.setTitle(app.getName());
        switchPreference.setSummary(app.getComponentName());

        switchPreference.setChecked(isExcludedCallback.apply(app));
        switchPreference.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean becameExcluded = newValue != null && (boolean) newValue;

                    if (becameExcluded) {
                        onExcludedListener.accept(app);
                    } else {
                        onIncludedListener.accept(app);
                    }

                    return true;
                }
        );
        return switchPreference;
    }
}
