package fr.neamar.kiss.notification;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;

import java.util.HashSet;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {
    public static final String NOTIFICATION_PREFERENCES_NAME = "notifications";

    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getBaseContext().getSharedPreferences(NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Set<String> currentNotifications = getCurrentNotificationsForPackage(sbn.getPackageName());

        currentNotifications.add(Integer.toString(sbn.getId()));
        prefs.edit().putStringSet(sbn.getPackageName(), currentNotifications).apply();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Set<String> currentNotifications = getCurrentNotificationsForPackage(sbn.getPackageName());

        currentNotifications.remove(Integer.toString(sbn.getId()));

        SharedPreferences.Editor editor = prefs.edit();
        if(currentNotifications.size() == 0) {
            // Clean up!
            editor.remove(sbn.getPackageName());
        }
        else {
            editor.putStringSet(sbn.getPackageName(), currentNotifications);
        }
        editor.apply();
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
}