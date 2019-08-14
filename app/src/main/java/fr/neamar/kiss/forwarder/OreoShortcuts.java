package fr.neamar.kiss.forwarder;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.utils.ShortcutUtil;

public class OreoShortcuts extends Forwarder {
    OreoShortcuts(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Shortcuts in Android O
        if (prefs.getBoolean("first-run-shortcuts", true) &&
                prefs.getBoolean("enable-shortcuts", true)) {

            ShortcutUtil.buildShortcuts(mainActivity);
            // Set flag to false
            prefs.edit().putBoolean("first-run-shortcuts", false).apply();
        }
    }

}
