package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ContactsProvider;


public class Permission extends Forwarder {
    public static final int PERMISSION_READ_CONTACTS = 0;
    public static final int PERMISSION_CALL_PHONE = 1;

    // Weak reference to the main activity, this is sadly required for permissions to work correctly.
    public static WeakReference<MainActivity> currentMainActivity;

    /**
     * Sometimes, we need to wait for the user to give us permission before we can start an intent.
     * Store the intent here for later use.
     * Ideally, we'd want to use MainActivty to store this, but MainActivity has stateNotNeeded=true
     * which means it's always rebuild from scratch, we can't store any state in it.
     */
    public static Intent pendingIntent = null;

    Permission(MainActivity mainActivity) {
        super(mainActivity);
        currentMainActivity = new WeakReference<>(mainActivity);
    }

    void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if(grantResults.length == 0) {
            return;
        }

        if (requestCode == PERMISSION_READ_CONTACTS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Great! Reload the contact provider. We're done :)
            ContactsProvider contactsProvider = KissApplication.getApplication(mainActivity).getDataHandler().getContactsProvider();
            if (contactsProvider != null) {
                contactsProvider.reload();
            }
        } else if (requestCode == PERMISSION_CALL_PHONE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Great! Start the intent we stored for later use.
                KissApplication kissApplication = KissApplication.getApplication(mainActivity);
                mainActivity.startActivity(pendingIntent);
                pendingIntent = null;

                // Record launch to clear search results
                mainActivity.launchOccurred();
            } else {
                Toast.makeText(mainActivity, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }
}