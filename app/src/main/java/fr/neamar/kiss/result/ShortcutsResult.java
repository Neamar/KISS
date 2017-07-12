package fr.neamar.kiss.result;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.List;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.utils.SpaceTokenizer;

public class ShortcutsResult extends Result {
    private final ShortcutsPojo shortcutPojo;

    ShortcutsResult(ShortcutsPojo shortcutPojo) {
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
    PopupMenu buildPopupMenu(Context context, RecordAdapter parent, View parentView) {
        return inflatePopupMenu(R.menu.menu_item_shortcut, context, parentView);
    }

    @Override
    Boolean popupMenuClickHandler(Context context, RecordAdapter parent, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_app_uninstall:
                launchUninstall(context, shortcutPojo);
                // Also remove item, since it will be uninstalled
                parent.removeResult(this);
                return true;
            case R.id.item_exclude:
                // remove item since it will be hidden
                parent.removeResult(this);
                excludeFromAppList(context, shortcutPojo);
                return true;
        }
        return super.popupMenuClickHandler(context, parent, item);
    }

    private void excludeFromAppList(Context context, ShortcutsPojo shortcutPojo) {
        KissApplication.getDataHandler(context).addToExcluded(shortcutPojo.nameNormalized, null);
        KissApplication.getDataHandler(context).getShortcutsProvider().removeShortcut(shortcutPojo);
        KissApplication.getDataHandler(context).removeFromFavorites((MainActivity) context, shortcutPojo.id);
        Toast.makeText(context, R.string.excluded_favorite_list_added, Toast.LENGTH_LONG).show();
    }

    private void launchEditTagsDialog(final Context context, final ShortcutsPojo shortcut) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.tags_add_title));

        // Create the tag dialog
        final View v = LayoutInflater.from(context).inflate(R.layout.tags_dialog, null);
        final MultiAutoCompleteTextView tagInput = (MultiAutoCompleteTextView) v.findViewById(R.id.tag_input);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_dropdown_item_1line, KissApplication.getDataHandler(context).getTagsHandler().getAllTagsAsArray());
        tagInput.setTokenizer(new SpaceTokenizer());
        tagInput.setText(shortcutPojo.tags);

        tagInput.setAdapter(adapter);
        builder.setView(v);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                KissApplication.getDataHandler(context).getTagsHandler().setTags(shortcut.id, tagInput.getText().toString());
                // Refresh tags for given app
                shortcut.setTags(tagInput.getText().toString());
                // Show toast message
                String msg = context.getResources().getString(R.string.tags_confirmation_added);
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void launchUninstall(Context context, ShortcutsPojo shortcutPojo) {
        DataHandler dh = KissApplication.getDataHandler(context);
        if (dh != null) {
            dh.removeShortcut(shortcutPojo);
        }
    }

}
