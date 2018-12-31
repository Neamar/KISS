package fr.neamar.kiss.shortcut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InstallShortcutHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent data) {
        new SaveShortcutAsync(context, data).execute();
    }
}
