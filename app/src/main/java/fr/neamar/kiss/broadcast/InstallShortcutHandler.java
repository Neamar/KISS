package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.util.Log;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ShortcutProvider;
import fr.neamar.kiss.pojo.ShortcutPojo;

public class InstallShortcutHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent data) {

        DataHandler dh = KissApplication.getDataHandler(context);
        ShortcutProvider sp = dh.getShortcutProvider();

        if (sp == null)
            return;

        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Log.d("onReceive", "Received shortcut " + name);

        Intent target = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        if (target.getAction() == null) {
            target.setAction(Intent.ACTION_VIEW);
        }

        ShortcutIconResource sir = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);

        if (sir != null) {
            Log.d("onReceive", "Received icon package name " + sir.packageName);
            Log.d("onReceive", "Received icon resource name " + sir.resourceName);

            ShortcutPojo pojo = sp.createPojo(name);

            pojo.packageName = sir.packageName;
            pojo.resourceName = sir.resourceName;
            // convert target intent to parsable uri
            pojo.intentUri = target.toUri(0);

            dh.addShortcut(context, pojo);
            dh.getShortcutProvider().addShortcut(pojo);
        }

    }

}
