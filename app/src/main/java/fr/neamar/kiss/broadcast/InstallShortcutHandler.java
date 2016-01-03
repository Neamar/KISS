package fr.neamar.kiss.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.Bitmap;
import android.util.Log;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.pojo.ShortcutsPojo;

public class InstallShortcutHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent data) {

        DataHandler dh = KissApplication.getDataHandler(context);
        ShortcutsProvider sp = dh.getShortcutsProvider();

        if (sp == null)
            return;

        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Log.d("onReceive", "Received shortcut " + name);
        
        //avoid duplicates
        if (sp != null && sp.findByName(name) != null || dh.getContactsProvider().findByName(name) != null || dh.getAppProvider().findByName(name) != null ) {
            Log.d("onReceive", "Duplicated shortcut " + name + ", ignoring");
            return;
        }
        
        Intent target = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        if (target.getAction() == null) {
            target.setAction(Intent.ACTION_VIEW);
        }

        ShortcutsPojo pojo = createPojo(name);
        
        // convert target intent to parsable uri
        pojo.intentUri = target.toUri(0);
        
        //get embedded icon
        Bitmap icon = (Bitmap) data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (icon != null) {
            Log.d("onReceive", "Shortcut " + name + " has embedded icon");
            pojo.icon = icon;
        } else {        
            ShortcutIconResource sir = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
    
            if (sir != null) {
                Log.d("onReceive", "Received icon package name " + sir.packageName);
                Log.d("onReceive", "Received icon resource name " + sir.resourceName);
    
                pojo.packageName = sir.packageName;
                pojo.resourceName = sir.resourceName;                
            } else { //invalid sourtcut
                Log.d("onReceive", "Invalid shortcut " + name + ", ignoring");
                return;
            }
        }
                
        dh.addShortcut(pojo);

    }
    
    public ShortcutsPojo createPojo(String name) {
        ShortcutsPojo pojo = new ShortcutsPojo();

        pojo.id = ShortcutsPojo.SCHEME + name.toLowerCase();
        pojo.setName(name);

        return pojo;
    }   

}
