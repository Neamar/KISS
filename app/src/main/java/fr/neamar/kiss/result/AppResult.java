package fr.neamar.kiss.result;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.icons.IconPack;
import fr.neamar.kiss.notification.NotificationListener;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;

public class AppResult extends Result<AppPojo> {

    private static final String TAG = AppResult.class.getSimpleName();
    private volatile Drawable icon = null;

    AppResult(@NonNull AppPojo pojo) {
        super(pojo);
    }

    @NonNull
    @Override
    public View display(final Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null) {
            view = inflateFromId(context, R.layout.item_app, parent);
        }

        TextView appName = view.findViewById(R.id.item_app_name);

        displayHighlighted(pojo.normalizedName, pojo.getName(), fuzzyScore, appName, context);

        final ImageView appIcon = view.findViewById(R.id.item_app_icon);
        if (!isHideIcons(context)) {
            this.setAsyncDrawable(appIcon);
        } else {
            appIcon.setImageDrawable(null);
        }

        displayNotificationDot(context, view, false);

        return view;
    }

    @Override
    public void inflateFavorite(@NonNull Context context, @NonNull View favoriteView) {
        super.inflateFavorite(context, favoriteView);
        displayNotificationDot(context, favoriteView, true);
    }

    private void displayNotificationDot(Context context, View view, boolean isFavorite) {
        String packageKey = getPackageKey();

        SharedPreferences notificationPrefs = context.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
        ImageView notificationView = view.findViewById(R.id.item_notification_dot);
        notificationView.setVisibility(notificationPrefs.contains(packageKey) ? View.VISIBLE : View.GONE);
        notificationView.setTag(packageKey);
        int dotColor = UIColors.getNotificationDotColor(context, isFavorite);
        notificationView.setColorFilter(dotColor);
    }

    private String getPackageKey() {
        return pojo.getPackageKey();
    }

    /**
     * Context menu (long click or mouse right click): only the actions that are useful on an emulator
     */
    @Override
    protected void buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter) {
        if (!pojo.isDisabled()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_details));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_exclude));
        if (canUninstall(context)) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_uninstall));
        }
    }

    private boolean canUninstall(Context context) {
        if (pojo.isDisabled()) {
            return false;
        }
        // app installed under /system can't be uninstalled
        ApplicationInfo ai = PackageManagerUtils.getApplicationInfo(context, this.pojo.packageName, this.pojo.userHandle);
        if (ai != null && (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return false;
        }
        UserManager userManager = ContextCompat.getSystemService(context, UserManager.class);
        Bundle restrictions = userManager.getUserRestrictions(pojo.userHandle.getRealHandle());
        return !restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL, false)
                && !restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false);
    }

    @Override
    protected boolean popupMenuClickHandler(final Context context, final RecordAdapter parent, int stringId, View parentView) {
        if (stringId == R.string.menu_app_details) {
            launchAppDetails(context);
            return true;
        } else if (stringId == R.string.menu_app_uninstall) {
            launchUninstall(context);
            return true;
        } else if (stringId == R.string.menu_exclude) {
            excludeFromKiss(context, pojo, parent);
            return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private void excludeFromKiss(Context context, AppPojo pojo, final RecordAdapter parent) {
        // remove item since it will be hidden
        parent.removeResult(AppResult.this);

        KissApplication.getApplication(context).getDataHandler().addToExcluded(pojo);
        Toast.makeText(context, R.string.excluded_app_list_added, Toast.LENGTH_LONG).show();
    }

    /**
     * Open an activity displaying details regarding the current package
     */
    private void launchAppDetails(Context context) {
        if (pojo.userHandle.isCurrentUser()) {
            // Straight to the system page: clear cache / force stop in one click
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pojo.packageName, null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
                return;
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Unable to open application details", e);
            }
        }
        LauncherApps launcher = ContextCompat.getSystemService(context, LauncherApps.class);
        assert launcher != null;
        launcher.startAppDetailsActivity(getClassName(), pojo.userHandle.getRealHandle(), null, null);
    }

    /**
     * Open an activity to uninstall the current package
     */
    private void launchUninstall(Context context) {
        Intent intent = new Intent(Intent.ACTION_DELETE,
                Uri.fromParts("package", pojo.packageName, null));
        intent.putExtra(Intent.EXTRA_USER, pojo.userHandle.getRealHandle());
        context.startActivity(intent);
    }

    @Override
    boolean isDrawableCached() {
        return icon != null;
    }

    @Override
    void setDrawableCache(Drawable drawable) {
        icon = drawable;
    }

    @Override
    public Drawable getDrawable(Context context) {
        if (icon == null) {
            synchronized (this) {
                if (icon == null) {
                    IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
                    icon = iconsHandler.getDrawableIconForPackage(context, getClassName(), this.pojo.userHandle);
                }
            }
        }
        DrawableUtils.setDisabled(icon, this.pojo.isDisabled());
        return icon;
    }

    @Override
    public void doLaunch(Context context, View v) {
        try {
            LauncherApps launcher = ContextCompat.getSystemService(context, LauncherApps.class);
            assert launcher != null;
            // No reveal animation: games should start as fast as possible
            View potentialIcon = v == null ? null : v.findViewById(R.id.item_app_icon);
            if (potentialIcon == null && v != null) {
                // If favorite, find the icon
                potentialIcon = v.findViewById(R.id.favorite);
            }
            Rect sourceBounds = getViewBounds(potentialIcon);

            launcher.startMainActivity(getClassName(), pojo.userHandle.getRealHandle(), sourceBounds, null);
        } catch (ActivityNotFoundException | NullPointerException | SecurityException e) {
            Log.w(TAG, "Unable to launch activity", e);
            // Application was just removed?
            // (null pointer exception can be thrown on Lollipop+ when app is missing)
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    protected Rect getViewBounds(@Nullable View view) {
        if (view == null) {
            return null;
        }

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    public String getComponentName() {
        return pojo.getComponentName();
    }

    public ComponentName getClassName() {
        return pojo.getComponent();
    }

    @Override
    protected boolean isAllowedAsFavorite() {
        return true;
    }

    @Override
    protected boolean canHaveCustomIcon(Context context, IconPack iconPack) {
        return true;
    }
}
