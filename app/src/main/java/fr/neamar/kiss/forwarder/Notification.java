package fr.neamar.kiss.forwarder;

import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.notification.NotificationListener;

import static android.content.Context.MODE_PRIVATE;

class Notification extends Forwarder {
    private final SharedPreferences notificationPreferences;

    private SharedPreferences.OnSharedPreferenceChangeListener onNotificationDisplayed = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.e("WTF", "Invalidated dataset: " + key);
            mainActivity.adapter.notifyDataSetChanged();
        }
    };

    Notification(MainActivity mainActivity) {
        super(mainActivity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            notificationPreferences = mainActivity.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, MODE_PRIVATE);
        } else {
            notificationPreferences = null;
        }
    }

    void onCreate() {
        if (notificationPreferences != null) {
            notificationPreferences.registerOnSharedPreferenceChangeListener(onNotificationDisplayed);
        }
    }

    void onStop() {
        if (notificationPreferences != null) {
            notificationPreferences.unregisterOnSharedPreferenceChangeListener(onNotificationDisplayed);
        }
    }
}
