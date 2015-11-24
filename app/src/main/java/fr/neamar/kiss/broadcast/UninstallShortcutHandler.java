package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ShortcutProvider;
import fr.neamar.kiss.pojo.ShortcutPojo;

public class UninstallShortcutHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent data) {

        DataHandler dh = KissApplication.getDataHandler(context);
        ShortcutProvider sp = dh.getShortcutProvider();
        
        if (sp == null)
            return;
      
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Log.d("onReceive", "Uninstall shortcut " + name);
        
        ShortcutPojo pojo = (ShortcutPojo) sp.findByName(name); 
        if (pojo == null) {
            Log.d("onReceive", "Shortcut " + name + " not found");
            return;
        }
        
        dh.removeShortcut(context, pojo);        

    }

}
