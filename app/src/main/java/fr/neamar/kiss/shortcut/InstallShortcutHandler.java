package fr.neamar.kiss.shortcut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.Bitmap;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.Locale;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.pojo.ShortcutsPojo;

public class InstallShortcutHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent data) {
        new SaveShortcutAsync(context, data).execute();
    }
}
