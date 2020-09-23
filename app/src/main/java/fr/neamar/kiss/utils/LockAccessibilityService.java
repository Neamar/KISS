package fr.neamar.kiss.utils;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class LockAccessibilityService extends AccessibilityService {
    public static String ACTION_LOCK = "fr.neamar.kiss.LOCK";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_LOCK.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
            }
        }
        return Service.START_STICKY;
    }
}
