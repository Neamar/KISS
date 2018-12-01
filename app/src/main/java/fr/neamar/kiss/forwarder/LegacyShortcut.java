package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.MenuItem;

import java.util.List;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;

public class LegacyShortcut extends Forwarder {
    LegacyShortcut(MainActivity mainActivity) {
        super(mainActivity);
    }

    boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.shortcut) {
            PackageManager packageManager = mainActivity.getPackageManager();
            Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
            List<ResolveInfo> shortcuts = packageManager.queryIntentActivities(shortcutsIntent, 0);
            for (ResolveInfo shortcut : shortcuts) {
                Log.e("WTF", "" + shortcut.activityInfo.applicationInfo.loadLabel(packageManager));
                Log.e("WTF", shortcut.activityInfo.packageName);
                Log.e("WTF", "" + shortcut.loadLabel(packageManager));
            }
            return true;
        }

        return false;
    }
}
