package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.ShortcutsPojo;

public class ShortcutsResult extends Result {
    private final ShortcutsPojo shortcutPojo;

    public ShortcutsResult(ShortcutsPojo shortcutPojo) {
        super();
        this.pojo = this.shortcutPojo = shortcutPojo;
    }

    @Override
    public View display(final Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_app);

        TextView appName = (TextView) v.findViewById(R.id.item_app_name);
        appName.setText(enrichText(shortcutPojo.displayName));

        final ImageView appIcon = (ImageView) v.findViewById(R.id.item_app_icon);
        appIcon.setImageDrawable(this.getDrawable(context));
        
        return v;
    }

    @Override
    protected void doLaunch(Context context, View v) {

        try {
            Intent intent = Intent.parseUri(shortcutPojo.intentUri, 0);
            context.startActivity(intent);
        } catch (Exception e) {
            // Application was just removed?
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public Drawable getDrawable(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        Resources resources;
        
        if (shortcutPojo.icon != null) {
            return new BitmapDrawable(shortcutPojo.icon);
        }
        
        try {           
            resources = packageManager.getResourcesForApplication(shortcutPojo.packageName);
            final int id = resources.getIdentifier(shortcutPojo.resourceName, null, null);
            return resources.getDrawable(id);
        } catch (NameNotFoundException e) {

        }
        return null;
    }
    
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    PopupMenu buildPopupMenu(Context context, RecordAdapter parent, View parentView) {
        PopupMenu menu = super.buildPopupMenu(context, parent, parentView);
        
        //add uninstall menu
        menu.getMenuInflater().inflate(R.menu.menu_item_app_uninstall, menu.getMenu());

        inflateBaseMenu(context, menu);
        return menu;
    }
    
    @Override
    Boolean popupMenuClickHandler(Context context, RecordAdapter parent, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.item_app_uninstall:
            launchUninstall(context, shortcutPojo);
            // Also remove item, since it will be uninstalled
            parent.removeResult(this);
            return true;
        
        }
        return super.popupMenuClickHandler(context, parent, item);
    }
    
    private void launchUninstall(Context context, ShortcutsPojo shortcutPojo) {
        DataHandler dh = KissApplication.getDataHandler(context);
        if (dh != null) {
            dh.removeShortcut(shortcutPojo);
        } 
    }

}
