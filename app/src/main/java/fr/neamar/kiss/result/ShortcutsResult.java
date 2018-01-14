package fr.neamar.kiss.result;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.List;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.ui.ListPopup;

public class ShortcutsResult extends Result {
    private final ShortcutsPojo shortcutPojo;

    public ShortcutsResult(ShortcutsPojo shortcutPojo) {
        super();
        this.pojo = this.shortcutPojo = shortcutPojo;
    }

    @Override
    public View display(final Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_shortcut);

        TextView appName = (TextView) v.findViewById(R.id.item_app_name);
        appName.setText(enrichText(shortcutPojo.displayName, context));

        final ImageView shortcutIcon = (ImageView) v.findViewById(R.id.item_shortcut_icon);
        final ImageView appIcon = (ImageView) v.findViewById(R.id.item_app_icon);

        // Retrieve package icon for this shortcut
        final PackageManager packageManager = context.getPackageManager();
        Drawable appDrawable = null;
        try {
            Intent intent = Intent.parseUri(shortcutPojo.intentUri, 0);
            List<ResolveInfo> packages = packageManager.queryIntentActivities(intent, 0);
            if(packages.size() > 0) {
                ResolveInfo mainPackage = packages.get(0);
                String packageName = mainPackage.activityInfo.applicationInfo.packageName;
                String activityName = mainPackage.activityInfo.name;
                ComponentName className =  new ComponentName(packageName, activityName);
                appDrawable = context.getPackageManager().getActivityIcon(className);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return v;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return v;
        }

        if (shortcutPojo.icon != null) {
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(), shortcutPojo.icon);
            shortcutIcon.setImageDrawable(drawable);
            appIcon.setImageDrawable(appDrawable);
        } else {
            // No icon for this shortcut, use app icon
            shortcutIcon.setImageDrawable(appDrawable);
            appIcon.setImageResource(android.R.drawable.ic_menu_send);
        }

        return v;
    }

    public Drawable getDrawable(Context context) {
        return new BitmapDrawable(context.getResources(), shortcutPojo.icon);
    }


    @Override
    protected void doLaunch(Context context, View v) {

        try {
            Intent intent = Intent.parseUri(shortcutPojo.intentUri, 0);
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                intent.setSourceBounds(v.getClipBounds());
            }

            context.startActivity(intent);
        } catch (Exception e) {
            // Application was just removed?
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }

    }

    @Override
	ListPopup buildPopupMenu( Context context, ArrayAdapter<ListPopup.Item> adapter, RecordAdapter parent, View parentView ) {
        adapter.add( new ListPopup.Item( context, R.string.menu_favorites_add ) );
        adapter.add( new ListPopup.Item( context, R.string.menu_favorites_remove ) );
        adapter.add( new ListPopup.Item( context, R.string.menu_shortcut_remove ) );

        return inflatePopupMenu(adapter, context );
    }

    @Override
    Boolean popupMenuClickHandler( Context context, RecordAdapter parent, int stringId ) {
        switch ( stringId ) {
            case R.string.menu_shortcut_remove:
                launchUninstall(context, shortcutPojo);
                // Also remove item, since it will be uninstalled
                parent.removeResult(this);
                return true;

        }
        return super.popupMenuClickHandler(context, parent, stringId );
    }

    private void launchUninstall(Context context, ShortcutsPojo shortcutPojo) {
        DataHandler dh = KissApplication.getDataHandler(context);
        if (dh != null) {
            dh.removeShortcut(shortcutPojo);
        }
    }

}
