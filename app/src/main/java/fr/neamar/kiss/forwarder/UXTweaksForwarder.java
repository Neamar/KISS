package fr.neamar.kiss.forwarder;

import android.content.SharedPreferences;

import fr.neamar.kiss.MainActivity;

/**
 * Created by neamar on 20/02/18.
 */

class UXTweaksForwarder extends Forwarder {
    UXTweaksForwarder(MainActivity mainActivity, SharedPreferences prefs) {
        super(mainActivity, prefs);
    }
}
