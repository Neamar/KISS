package fr.neamar.kiss.forwarder;

import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.dataprovider.ContactsProvider;


public class Permission extends Forwarder {
    private static final int PERMISSION_READ_CONTACTS = 0;

    Permission(MainActivity mainActivity) {
        super(mainActivity);
    }


    void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mainActivity.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            mainActivity.requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},
                    PERMISSION_READ_CONTACTS);
        }
    }

    void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Great! Reload the contact provider. We're done :)
                    ContactsProvider contactsProvider = KissApplication.getApplication(mainActivity).getDataHandler().getContactsProvider();
                    if(contactsProvider != null) {
                        contactsProvider.reload();
                    }
                }
            }
        }
    }
}
