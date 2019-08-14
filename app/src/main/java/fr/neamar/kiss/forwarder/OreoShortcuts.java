package fr.neamar.kiss.forwarder;

import android.os.Build;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.shortcut.SaveOreoShortcutAsync;

public class OreoShortcuts extends Forwarder {
    OreoShortcuts(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Shortcuts in Android O
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (prefs.getBoolean("firstRunShortcuts", true)) {
                new SaveOreoShortcutAsync(mainActivity).execute();
                // set flag to false
                prefs.edit().putBoolean("firstRunShortcuts", false).apply();
            }
        }
    }

}
