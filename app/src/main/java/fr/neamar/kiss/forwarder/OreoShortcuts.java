package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.utils.ShortcutUtil;

class OreoShortcuts extends Forwarder {
    OreoShortcuts(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Shortcuts in Android O
        if (ShortcutUtil.areShortcutsEnabled(mainActivity)) {
            // On first run save all shortcuts
            if (prefs.getBoolean("first-run-shortcuts", true)) {
                if(getHomePackage().equals(mainActivity.getPackageName())) {
                    // Save all shortcuts
                    ShortcutUtil.addAllShortcuts(mainActivity);
                    // Set flag to falseX
                    prefs.edit().putBoolean("first-run-shortcuts", false).apply();
                }
            }

            Intent intent = mainActivity.getIntent();
            if (intent != null) {
                final String action = intent.getAction();
                if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(action)) {
                    // Save single shortcut via a pin request
                    ShortcutUtil.addShortcut(mainActivity, intent);
                }
            }
        }

    }

    private String getHomePackage() {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = mainActivity.getPackageManager();
            final ResolveInfo mInfo = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            return mInfo.activityInfo.packageName;
        }
        catch(Exception e)
        {
            return "unknown";
        }
    }
}
