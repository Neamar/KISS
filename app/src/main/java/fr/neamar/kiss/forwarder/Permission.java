package fr.neamar.kiss.forwarder;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.dataprovider.ContactsProvider;


class Permission extends Forwarder {

    Permission(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if(grantResults.length == 0) {
            return;
        }

        if (requestCode == MainActivity.PERMISSION_READ_CONTACTS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Great! Reload the contact provider. We're done :)
            ContactsProvider contactsProvider = KissApplication.getApplication(mainActivity).getDataHandler().getContactsProvider();
            if (contactsProvider != null) {
                contactsProvider.reload();
            }
        } else if (requestCode == MainActivity.PERMISSION_CALL_PHONE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Great! Start the intent we stored for later use.
            KissApplication kissApplication = KissApplication.getApplication(mainActivity);
            mainActivity.startActivity(kissApplication.pendingIntent);
            kissApplication.pendingIntent = null;
        }
    }
}