package fr.neamar.kiss.result;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.AppPojo;

public class AppResult extends Result {
    private final AppPojo appPojo;

    private final ComponentName className;

    public AppResult(AppPojo appPojo) {
        super();
        this.pojo = this.appPojo = appPojo;

        className = new ComponentName(appPojo.packageName, appPojo.activityName);
    }

    @Override
    public View display(final Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_app);

        TextView appName = (TextView) v.findViewById(R.id.item_app_name);
        appName.setText(enrichText(appPojo.displayName));

        final ImageView appIcon = (ImageView) v.findViewById(R.id.item_app_icon);
        if (position < 15) {
            appIcon.setImageDrawable(this.getDrawable(context));
        } else {
            // Do actions on a message queue to avoid performance issues on main thread
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    appIcon.setImageDrawable(getDrawable(context));
                }
            });
        }

        return v;
    }

    @Override
    public PopupMenu getPopupMenu(final Context context, final RecordAdapter parent, View parentView) {
        PopupMenu menu = new PopupMenu(context, parentView);
        menu.getMenuInflater().inflate(R.menu.menu_app, menu.getMenu());

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                popupMenuClickHandler(context, parent, appPojo, item);
                return true;
            }
        });

        return menu;
    }

    private void popupMenuClickHandler(Context context, RecordAdapter parent, AppPojo appPojo, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_remove:
                removeItem(context, parent);
                break;
            case R.id.item_app_details:
                launchAppDetails(context, appPojo);
                break;
            case R.id.item_app_uninstall:
                launchUninstall(context, appPojo);
                break;
        }
    }

    private void removeItem(Context context, RecordAdapter parent) {
        parent.removeResult(this);
        Toast.makeText(context, R.string.removed_item, Toast.LENGTH_SHORT).show();
    }

    private void launchAppDetails(Context context, AppPojo app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", app.packageName, null));
        context.startActivity(intent);
    }

    private void launchUninstall(Context context, AppPojo app) {
        Intent intent = new Intent(Intent.ACTION_DELETE,
                Uri.fromParts("package", app.packageName, null));
        context.startActivity(intent);
    }

    @Override
    public Drawable getDrawable(Context context) {
        try {
            return context.getPackageManager().getActivityIcon(className);
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void doLaunch(Context context, View v) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(className);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Application was just removed?
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }
    }
}
