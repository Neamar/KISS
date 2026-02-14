package fr.neamar.kiss;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.NameComparator;
import fr.neamar.kiss.utils.Utilities;

public class ExcludeAppSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences_exclude_app);

        if (rootKey != null) {
            switch (rootKey) {
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
                AppPojo::isExcluded
        );
    }

    private void addExcludedFromHistoryAppSettings() {
        addExcludedApps(
                app -> getDataHandler().addToExcludedFromHistory(app),
                app -> getDataHandler().removeFromExcludedFromHistory(app),
                AppPojo::isExcludedFromHistory
        );
    }

    private void addExcludedShortcutAppSettings() {
        addExcludedApps(
                app -> getDataHandler().addToExcludedShortcutApps(app),
                app -> getDataHandler().removeFromExcludedShortcutApps(app),
                AppPojo::isExcludedShortcuts
        );
    }

    private void addExcludedApps(
            @NonNull Consumer<AppPojo> onExcludedListener,
            @NonNull Consumer<AppPojo> onIncludedListener,
            @NonNull Function<AppPojo, Boolean> isExcludedCallback
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
        for (AppPojo app : apps) {
            SwitchPreference pref = createExcludeAppSwitch(requireContext(), iconsHandler, isExcludedCallback, app, onExcludedListener, onIncludedListener);

            excludedAppsScreen.addPreference(pref);
        }
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

    private DataHandler getDataHandler() {
        return KissApplication.getApplication(requireContext()).getDataHandler();
    }

}
