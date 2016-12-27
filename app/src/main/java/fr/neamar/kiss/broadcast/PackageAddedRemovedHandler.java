package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.AppProvider;

/**
 * This class gets called when an application is created or removed on the
 * system
 * <p/>
 * We then recreate our data set.
 *
 * @author dorvaryn
 */
public class PackageAddedRemovedHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {

        if (PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("enable-app-history", true)) {
            // Insert into history new packages (not updated ones)
            if ("android.intent.action.PACKAGE_ADDED".equals(intent.getAction()) && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                // Add new package to history
                String packageName = intent.getData().getSchemeSpecificPart();

                Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent == null) {//for some plugin app
                    return;
                }

                String className = launchIntent.getComponent().getClassName();
                if (className != null) {
                    KissApplication.getDataHandler(ctx)
                            .addToHistory("app://" + packageName + "/" + className);
                }
            }
        }

        if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
            // Removed all installed shortcuts
            String packageName = intent.getData().getSchemeSpecificPart();

            KissApplication.getDataHandler(ctx).removeShortcuts(packageName);

            KissApplication.getDataHandler(ctx).removeFromExcluded(packageName);
        }

        // Reload application list
        final AppProvider provider = KissApplication.getDataHandler(ctx).getAppProvider();
        if (provider != null) {
            provider.reload();
        }

    }

}
