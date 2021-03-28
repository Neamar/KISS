package fr.neamar.kiss.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {
    public static final String TAG = "NotifListener";
    public static final String NOTIFICATION_PREFERENCES_NAME = "notifications";

    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getBaseContext().getSharedPreferences(NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.i(TAG, "Notification listener connected");

        refreshAllNotifications();
    }

    private void refreshAllNotifications() {
        // Build a map of notifications currently displayed,
        // ordered per package
        StatusBarNotification[] sbns = getActiveNotifications();
        Map<String, Set<String>> notificationsByPackage = new HashMap<>();
        for (StatusBarNotification sbn : sbns) {
            if (isNotificationTrivial(sbn)) {
                continue;
            }

            String packageName = sbn.getPackageName();
            if (!notificationsByPackage.containsKey(packageName)) {
                notificationsByPackage.put(packageName, new HashSet<>());
            }

            notificationsByPackage.get(packageName).add(Integer.toString(sbn.getId()));
        }

        // And synchronise this map with our SharedPreferences
        // (an easier option would have been to .clear() the SharedPreferences,
        // but then the listeners on SharedPreferences are not properly triggered)
        SharedPreferences.Editor editor = prefs.edit();
        // allKeys contains all the package names either in preferences or in the current notifications
        Set<String> allKeys = new HashSet<>(prefs.getAll().keySet());
        allKeys.addAll(notificationsByPackage.keySet());
        for (String packageName : allKeys) {
            if (notificationsByPackage.containsKey(packageName)) {
                editor.putStringSet(packageName, notificationsByPackage.get(packageName));
            } else {
                editor.remove(packageName);
            }
        }

        editor.apply();

        Log.v(TAG, "Refreshed all notifications for " + allKeys.toString());
    }

    @Override
    public void onListenerDisconnected() {
        Log.i(TAG, "Notification listener disconnected");

        // Clean up everything we have in memory to ensure we don't keep displaying trailing dots.
        // We don't use .clear() to ensure listeners are properly called.
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> packages = prefs.getAll().keySet();

        for (String packageName : packages) {
            editor.remove(packageName);
        }

        editor.apply();

        Log.v(TAG, "Removed all notifications for " + packages.toString());

        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationRankingUpdate(RankingMap rankingMap) {
        super.onNotificationRankingUpdate(rankingMap);

        refreshAllNotifications();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (isNotificationTrivial(sbn)) {
            return;
        }

        Set<String> currentNotifications = getCurrentNotificationsForPackage(sbn.getPackageName());
        if (currentNotifications.add(Integer.toString(sbn.getId()))) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(sbn.getPackageName(), currentNotifications);
            editor.apply();

            Log.v(TAG, "Added notification for " + sbn.getPackageName() + ": " + currentNotifications.toString());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Set<String> currentNotifications = getCurrentNotificationsForPackage(sbn.getPackageName());

        if (currentNotifications.remove(Integer.toString(sbn.getId()))) {
            SharedPreferences.Editor editor = prefs.edit();
            if (currentNotifications.isEmpty()) {
                // Clean up!
                editor.remove(sbn.getPackageName());
            } else {
                editor.putStringSet(sbn.getPackageName(), currentNotifications);
            }
            editor.apply();

            Log.v(TAG, "Removed notification for " + sbn.getPackageName() + ": " + currentNotifications.toString());
        }
    }

    public Set<String> getCurrentNotificationsForPackage(String packageName) {
        Set<String> currentNotifications = prefs.getStringSet(packageName, null);
        if (currentNotifications == null) {
            return new HashSet<>();
        } else {
            // The set returned by getStringSet() should NOT be modified
            // see https://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet(java.lang.String,%2520java.util.Set%3Cjava.lang.String%3E)
            return new HashSet<>(currentNotifications);
        }
    }

    /**
     * Check for trivial notifications.
     *
     * From Android O notification channels controls if badges should be displayed.
     * For older versions and legacy notification channel low priority notifications, ongoing notifications
     * and group summaries should not be displayed.
     *
     * @param sbn
     * @return true if badge should not be displayed
     */
    public boolean isNotificationTrivial(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final Ranking mTempRanking = new Ranking();
            getCurrentRanking().getRanking(sbn.getKey(), mTempRanking);
            if (!mTempRanking.canShowBadge()) {
                return true;
            }
            if (!mTempRanking.getChannel().getId().equals(NotificationChannel.DEFAULT_CHANNEL_ID)) {
                return isGroupHeader(notification);
            }
        }

        return notification.priority <= Notification.PRIORITY_MIN || (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0 || isGroupHeader(notification);
    }

    private boolean isGroupHeader(Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0;
        } else {
            return false;
        }
    }
}
