package fr.neamar.kiss.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import androidx.annotation.NonNull;


public class Permission {
    public static final int PERMISSION_READ_CONTACTS = 0;
    public static final int PERMISSION_CALL_PHONE = 1;
    public static final int PERMISSION_READ_PHONE_STATE = 2;

    private static final String[] permissions = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
    };

    // Static weak reference to the linked activity, this is sadly required
    // to ensure classes requesting permission can access activity.requestPermission()
    private static WeakReference<Activity> currentActivity = new WeakReference<Activity>(null);

    private static ArrayList<PermissionResultListener> permissionListeners;

    public static boolean checkPermission(Context context, int permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || context.checkSelfPermission(permissions[permission]) == PackageManager.PERMISSION_GRANTED;
    }

    public static void askPermission(int permission, PermissionResultListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (listener != null) {
            listener.permission = permission;
            if (permissionListeners == null) {
                permissionListeners = new ArrayList<PermissionResultListener>();
            }
            permissionListeners.add(listener);
        }

        Activity activity = Permission.currentActivity.get();
        if (activity != null) {
            activity.requestPermissions(new String[]{permissions[permission]}, permission);
        }
    }

    public Permission(Activity activity) {
        // Store the latest reference to a MainActivity
        currentActivity = new WeakReference<>(activity);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return;
        }

        // Iterator allows to remove while iterating
        ListIterator<PermissionResultListener> it = permissionListeners.listIterator();
        PermissionResultListener permissionListener;
        while (it.hasNext()) {
            permissionListener = it.next();
            if (permissionListener.permission == requestCode) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionListener.onGranted();
                } else {
                    permissionListener.onDenied();
                }
                it.remove();
            }
        }
    }

    public static class PermissionResultListener {
        public int permission = 0;

        public void onGranted() {};
        public void onDenied() {};
    }
}
