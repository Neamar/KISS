package fr.neamar.kiss;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import fr.neamar.kiss.BadgeHandler;
import fr.neamar.kiss.KissApplication;

public class SamsungBadgeObserver extends ContentObserver {

    private Context context;

    public SamsungBadgeObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange, Uri pUri) {
        // query badge status on content provider
        loadBadges(context);

        if(MainActivity.getInstance() != null){
            MainActivity.getInstance().reloadBadges();
        }
    }

    public static boolean providerExists(Context context) {
        Uri uri = Uri.parse("content://com.sec.badge/apps");
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        boolean exists = cursor != null;

        if(cursor != null)
            cursor.close();

        return exists;
    }

    /** Queries current badge status on ContentResolver for all packages on it
     * Updates the badges count on BadgeHandler
     */
    public static void loadBadges(Context context) {
        Uri uri = Uri.parse("content://com.sec.badge/apps");
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        // Return if cursor is null. Means provider does not exists
        if (cursor == null) {
            return;
        }

        try {
            if (!cursor.moveToFirst()) {
                // No results. Nothing to query
                return;
            }
            BadgeHandler badgeHandler = KissApplication.getDataHandler(context).getBadgeHandler();

            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                String packageName = cursor.getString(1);
                int badgeCount = cursor.getInt(3);
                badgeHandler.setBadgeCount(packageName, badgeCount);
            }
        } finally {
            cursor.close();
        }
    }
}
