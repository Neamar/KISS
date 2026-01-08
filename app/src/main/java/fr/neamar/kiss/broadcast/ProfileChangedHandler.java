package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.utils.UserHandle;

public class ProfileChangedHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_MANAGED_PROFILE_REMOVED.equals(intent.getAction())) {
            // Try to clean up app-related data when profile is removed
            android.os.UserHandle profile = intent.getParcelableExtra(Intent.EXTRA_USER);

            // Package installation/uninstallation events for the main
            // profile are still handled using PackageAddedRemovedHandler
            UserHandle user = new UserHandle(context, profile);

            DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
            dataHandler.removeFromExcluded(user);
            dataHandler.removeFromFavorites(user);
        }

        if (Intent.ACTION_MANAGED_PROFILE_ADDED.equals(intent.getAction()) ||
                Intent.ACTION_MANAGED_PROFILE_REMOVED.equals(intent.getAction()) ||
                Intent.ACTION_USER_UNLOCKED.equals(intent.getAction()) ||
                Intent.ACTION_PROFILE_ACCESSIBLE.equals(intent.getAction()) ||
                Intent.ACTION_PROFILE_INACCESSIBLE.equals(intent.getAction()) ||
                Intent.ACTION_MANAGED_PROFILE_AVAILABLE.equals(intent.getAction()) ||
                Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE.equals(intent.getAction())) {
            DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();

            dataHandler.reloadApps();
            dataHandler.reloadShortcuts();
        }
    }

    public void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            filter.addAction(Intent.ACTION_USER_UNLOCKED);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            filter.addAction(Intent.ACTION_PROFILE_ACCESSIBLE);
            filter.addAction(Intent.ACTION_PROFILE_INACCESSIBLE);
        }

        context.registerReceiver(this, filter);
    }
}
