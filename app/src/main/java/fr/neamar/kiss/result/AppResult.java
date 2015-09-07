package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
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
        if (appPojo.packageImage == null) {
            // Do actions on a message queue to avoid performance issues on main thread
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    appPojo.packageImage = getDrawable(context);
                    appIcon.setImageDrawable(appPojo.packageImage);
                }
            });
        } else {
            appIcon.setImageDrawable(appPojo.packageImage);
        }

        return v;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected PopupMenu buildPopupMenu(Context context, final RecordAdapter parent, View parentView) {
        PopupMenu menu = new PopupMenu(context, parentView);
        menu.getMenuInflater().inflate(R.menu.menu_item_app, menu.getMenu());

        return menu;
    }

    @Override
    protected Boolean popupMenuClickHandler(Context context, RecordAdapter parent, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_app_details:
                launchAppDetails(context, appPojo);
                return true;
            case R.id.item_app_uninstall:
                launchUninstall(context, appPojo);
                // Also remove item, since it will be uninstalled
                parent.removeResult(this);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, item);
    }

    /**
     * Open an activity displaying details regarding the current package
     */
    private void launchAppDetails(Context context, AppPojo app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", app.packageName, null));
        context.startActivity(intent);
    }

    /**
     * Open an activity to uninstall the current package
     */
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
