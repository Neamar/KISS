package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.pm.LauncherApps;
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
            Intent intent = mainActivity.getIntent();
            if (intent != null) {
                final String action = intent.getAction();
                if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(action)) {
                    new SaveOreoShortcutAsync(mainActivity, intent).execute();
                }
            }
        }
    }

}
