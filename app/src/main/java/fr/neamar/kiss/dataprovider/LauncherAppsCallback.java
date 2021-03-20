package fr.neamar.kiss.dataprovider;

import android.annotation.SuppressLint;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.UserHandle;

import androidx.annotation.RequiresApi;

import java.util.List;

/**
 * Empty implementation of LauncherApps.Callback so we do not need to override all methods when
 * only parts of LauncherApps.Callback are needed.
 */
@SuppressLint("NewApi")
public class LauncherAppsCallback extends LauncherApps.Callback {
    @Override
    public void onPackageRemoved(String packageName, UserHandle user) {

    }

    @Override
    public void onPackageAdded(String packageName, UserHandle user) {

    }

    @Override
    public void onPackageChanged(String packageName, UserHandle user) {

    }

    @Override
    public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {

    }

    @Override
    public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {

    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, UserHandle user) {
    }

}
