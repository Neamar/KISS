package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.AppPojo;

public class AppResult extends Result {
    private final AppPojo appPojo;

    private final ComponentName className;

    private Drawable icon = null;

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

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("icons-hide", false)) {
            // Display icon directy for first icons, and also for phones above lollipop
            // (fix a weird recycling issue with ListView on Marshmallow,
            // where the recycling occurs synchronously, before the handler)
            if (position < 15 || Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
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
        }
        else {
            appIcon.setVisibility(View.INVISIBLE);
        }
        return v;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected PopupMenu buildPopupMenu(Context context, final RecordAdapter parent, View parentView) {
        PopupMenu menu = inflatePopupMenu(R.menu.menu_item_app, context, parentView);

        try {
            // app installed under /system can't be uninstalled
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(this.appPojo.packageName, 0);
            // Need to AND the flags with SYSTEM:
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                menu.getMenuInflater().inflate(R.menu.menu_item_app_uninstall, menu.getMenu());
            }
        } catch (NameNotFoundException e) {
            // should not happen
        }

        //append root menu if available
        if (KissApplication.getRootHandler(context).isRootActivated() && KissApplication.getRootHandler(context).isRootAvailable()) {
            menu.getMenuInflater().inflate(R.menu.menu_item_app_root, menu.getMenu());
        }
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
            case R.id.item_app_hibernate:
                hibernate(context, appPojo);
                return true;
            case R.id.item_exclude:
                // remove item since it will be hiddden
                parent.removeResult(this);
                excludeFromAppList(context, appPojo);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, item);
    }

    private void excludeFromAppList(Context context, AppPojo appPojo) {
            String excludedAppList = PreferenceManager.getDefaultSharedPreferences(context).
                    getString("excluded-apps-list", context.getPackageName() + ";");
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("excluded-apps-list", excludedAppList + appPojo.packageName + ";").commit();
        //remove app pojo from appProvider results - no need to reset handler
        KissApplication.getDataHandler(context).getAppProvider().removeApp(appPojo);
        KissApplication.getDataHandler(context).removeFromFavorites(appPojo, context);
        Toast.makeText(context, R.string.excluded_app_list_added, Toast.LENGTH_LONG).show();

    }

    /**
     * Open an activity displaying details regarding the current package
     */
    private void launchAppDetails(Context context, AppPojo app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", app.packageName, null));
        context.startActivity(intent);
    }

    private void hibernate(Context context, AppPojo app) {
        String msg = context.getResources().getString(R.string.toast_hibernate_completed);
        if (!KissApplication.getRootHandler(context).hibernateApp(appPojo.packageName)) {
            msg = context.getResources().getString(R.string.toast_hibernate_error);
        }

        Toast.makeText(context, String.format(msg, app.name), Toast.LENGTH_SHORT).show();
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
            if (icon == null)
                icon = context.getPackageManager().getActivityIcon(className);
            return icon;
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
