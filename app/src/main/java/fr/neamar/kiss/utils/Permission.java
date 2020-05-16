package fr.neamar.kiss.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;

import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ContactsProvider;


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

    /**
     * Sometimes, we need to wait for the user to give us permission before we can start an intent.
     * Store the intent here for later use.
     * Ideally, we'd want to use MainActivity to store this, but MainActivity has stateNotNeeded=true
     * which means it's always rebuild from scratch, we can't store any state in it.
     * This means that when we use pendingIntent, it's highly likely taht by the time we end up using it,
     * currentMainActivity will have changed
     */
    private static Intent pendingIntent = null;

    private static ArrayList<PermissionResultListener> permissionListeners;

    /**
     * Try to start the dialer with specified intent, if we have permission already
     * Otherwise, ask for permission and store the intent for future use;
     *
     * @return true if we do have permission already, false if we're asking for permission now and will handle dispatching the intent in the Forwarder
     */
    public static boolean ensureCallPhonePermission(Intent pendingIntent) {
        Activity activity = Permission.currentActivity.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity != null && activity.checkSelfPermission(android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE},
                    Permission.PERMISSION_CALL_PHONE);
            Permission.pendingIntent = pendingIntent;

            return false;
        }

        return true;
    }

    public static boolean checkContactPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void askContactPermission() {
        // If we don't have permission to list contacts, ask for it.
        Activity activity = Permission.currentActivity.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity != null) {
            activity.requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},
                    Permission.PERMISSION_READ_CONTACTS);
        }
    }

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

        Activity activity = Permission.currentActivity.get();

        if (requestCode == PERMISSION_READ_CONTACTS && grantResults[0] == PackageManager.PERMISSION_GRANTED && activity != null) {
            // Great! Reload the contact provider. We're done :)
            ContactsProvider contactsProvider = KissApplication.getApplication(activity).getDataHandler().getContactsProvider();
            if (contactsProvider != null) {
                contactsProvider.reload();
            }
        } else if (requestCode == PERMISSION_CALL_PHONE && activity != null) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Great! Start the intent we stored for later use.
                activity.startActivity(pendingIntent);
                pendingIntent = null;

                if (activity instanceof MainActivity) {
                    // Record launch to clear search results
                    ((MainActivity) activity).launchOccurred();
                }
            } else {
                Toast.makeText(activity, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
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
