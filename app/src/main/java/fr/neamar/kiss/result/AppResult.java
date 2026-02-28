package fr.neamar.kiss.result;

import android.app.ActivityOptions;
import android.app.AlertDialog;
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
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import fr.neamar.kiss.CustomIconDialog;
import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.notification.NotificationListener;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.ui.GoogleCalendarIcon;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;

public class AppResult extends ResultWithTags<AppPojo> {

    private static final String TAG = AppResult.class.getSimpleName();
    private final ComponentName className;
    private volatile Drawable icon = null;

    AppResult(@NonNull AppPojo pojo) {
        super(pojo);

        className = new ComponentName(pojo.packageName, pojo.activityName);
    }

    @NonNull
    @Override
    public View display(final Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null) {
            view = inflateFromId(context, R.layout.item_app, parent);
        }

        TextView appName = view.findViewById(R.id.item_app_name);

        displayHighlighted(pojo.normalizedName, pojo.getName(), fuzzyScore, appName, context);

        TextView tagsView = view.findViewById(R.id.item_app_tag);
        displayTags(context, fuzzyScore, tagsView);

        final ImageView appIcon = view.findViewById(R.id.item_app_icon);
        if (!isHideIcons(context)) {
            this.setAsyncDrawable(appIcon);
        } else {
            appIcon.setImageDrawable(null);
        }

        String packageKey = getPackageKey();

        SharedPreferences notificationPrefs = context.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
        ImageView notificationView = view.findViewById(R.id.item_notification_dot);
        notificationView.setVisibility(notificationPrefs.contains(packageKey) ? View.VISIBLE : View.GONE);
        notificationView.setTag(packageKey);

        int dotColor = UIColors.getNotificationDotColor(context);
        notificationView.setColorFilter(dotColor);

        return view;
    }

    private String getPackageKey() {
        return pojo.getPackageKey();
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter) {
        if (!(context instanceof MainActivity) || ((MainActivity) context).isViewingSearchResults()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_exclude));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_app_rename));
        // only display this option if we're using a custom icon pack, as it is not useful otherwise
        if (KissApplication.getApplication(context).getIconsHandler().getCustomIconPack() != null) {
            adapter.add(new ListPopup.Item(context, R.string.menu_custom_icon));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_tags_edit));
        if (!pojo.isDisabled()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_details));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_app_store));

        boolean uninstallDisabled = pojo.isDisabled();
        if (!uninstallDisabled) {
            // app installed under /system can't be uninstalled
            ApplicationInfo ai = PackageManagerUtils.getApplicationInfo(context, this.pojo.packageName, this.pojo.userHandle);
            // Need to AND the flags with SYSTEM:
            uninstallDisabled = ai != null && (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }

        if (!uninstallDisabled) {
            UserManager userManager = ContextCompat.getSystemService(context, UserManager.class);
            Bundle restrictions = userManager.getUserRestrictions(pojo.userHandle.getRealHandle());
            uninstallDisabled = restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL, false)
                    || restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false);
        }

        if (!uninstallDisabled) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_uninstall));
        }

        // append root menu if available
        if (KissApplication.getApplication(context).getRootHandler().isRootActivated() && KissApplication.getApplication(context).getRootHandler().isRootAvailable()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_hibernate));
        }

        return inflatePopupMenu(adapter, context);
    }

    @Override
    protected boolean popupMenuClickHandler(final Context context, final RecordAdapter parent, int stringId, View parentView) {
        if (stringId == R.string.menu_app_details) {
            launchAppDetails(context, pojo);
            return true;
        } else if (stringId == R.string.menu_app_store) {
            launchAppStore(context, pojo);
            return true;
        } else if (stringId == R.string.menu_app_uninstall) {
            launchUninstall(context, pojo);
            return true;
        } else if (stringId == R.string.menu_app_hibernate) {
            hibernate(context, pojo);
            return true;
        } else if (stringId == R.string.menu_exclude) {
            final int EXCLUDE_HISTORY_ID = 0;
            final int EXCLUDE_KISS_ID = 1;
            PopupMenu popupExcludeMenu = new PopupMenu(context, parentView);
            //Adding menu items
            popupExcludeMenu.getMenu().add(EXCLUDE_HISTORY_ID, Menu.NONE, Menu.NONE, R.string.menu_exclude_history);
            popupExcludeMenu.getMenu().add(EXCLUDE_KISS_ID, Menu.NONE, Menu.NONE, R.string.menu_exclude_kiss);
            //registering popup with OnMenuItemClickListener
            popupExcludeMenu.setOnMenuItemClickListener(item -> {
                switch (item.getGroupId()) {
                    case EXCLUDE_HISTORY_ID:
                        excludeFromHistory(context, pojo);
                        return true;
                    case EXCLUDE_KISS_ID:
                        excludeFromKiss(context, pojo, parent);
                        return true;
                }

                return true;
            });

            popupExcludeMenu.show();
            return true;
        } else if (stringId == R.string.menu_app_rename) {
            launchRenameDialog(context, parent, pojo);
            return true;
        } else if (stringId == R.string.menu_custom_icon) {
            launchCustomIcon(context, parent);
            return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private void excludeFromHistory(Context context, AppPojo pojo) {
        // add to excluded from history app list
        KissApplication.getApplication(context).getDataHandler().addToExcludedFromHistory(pojo);
        // remove from history
        removeFromHistory(context);
        // inform user
        Toast.makeText(context, R.string.excluded_app_history_added, Toast.LENGTH_LONG).show();
    }

    private void excludeFromKiss(Context context, AppPojo pojo, final RecordAdapter parent) {
        // remove item since it will be hidden
        parent.removeResult(context, AppResult.this);

        KissApplication.getApplication(context).getDataHandler().addToExcluded(pojo);
        Toast.makeText(context, R.string.excluded_app_list_added, Toast.LENGTH_LONG).show();
    }

    private void launchRenameDialog(final Context context, RecordAdapter parent, final AppPojo app) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.app_rename_title));

        builder.setView(R.layout.rename_dialog);

        builder.setPositiveButton(R.string.custom_name_rename, (dialog, which) -> {
            EditText input = ((AlertDialog) dialog).findViewById(R.id.rename);
            dialog.dismiss();

            // Set new name
            String newName = input.getText().toString().trim();
            app.setName(newName);
            KissApplication.getApplication(context).getDataHandler().renameApp(app.getComponentName(), newName);

            // Show toast message
            String msg = context.getResources().getString(R.string.app_rename_confirmation, app.getName());
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            setTranscriptModeAlwaysScroll(parent);
        });
        builder.setNegativeButton(R.string.custom_name_set_default, (dialog, which) -> {
            dialog.dismiss();

            KissApplication.getApplication(context).getDataHandler().removeRenameApp(getComponentName());

            // Set initial name
            String name = PackageManagerUtils.getLabel(context, new ComponentName(app.packageName, app.activityName), app.userHandle);
            if (name != null) {
                app.setName(name);

                // Show toast message
                String msg = context.getResources().getString(R.string.app_rename_confirmation, app.getName());
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
            setTranscriptModeAlwaysScroll(parent);
        });
        builder.setNeutralButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
            setTranscriptModeAlwaysScroll(parent);
        });
        setTranscriptModeDisabled(parent);
        AlertDialog dialog = builder.create();
        dialog.show();
        // call after dialog got inflated (show call)
        ((TextView) dialog.findViewById(R.id.rename)).setText(app.getName());
    }

    private void launchCustomIcon(Context context, RecordAdapter parent) {
        //TODO: launch a DialogFragment or Activity
        CustomIconDialog dialog = new CustomIconDialog();

        // set args
        {
            Bundle args = new Bundle();
            args.putString("className", className.flattenToString()); // will be converted back with ComponentName.unflattenFromString()
            args.putParcelable("userHandle", pojo.userHandle);
            args.putString("componentName", pojo.getComponentName());
            args.putLong("customIcon", pojo.getCustomIconId());
            dialog.setArguments(args);
        }

        dialog.setOnConfirmListener(drawable -> {
            if (drawable == null)
                KissApplication.getApplication(context).getIconsHandler().restoreAppIcon(this);
            else
                KissApplication.getApplication(context).getIconsHandler().changeAppIcon(this, drawable);
            //TODO: force update the icon in the view
        });

        parent.showDialog(dialog);
    }

    /**
     * Open an activity displaying details regarding the current package
     */
    private void launchAppDetails(Context context, AppPojo app) {
        LauncherApps launcher = ContextCompat.getSystemService(context, LauncherApps.class);
        assert launcher != null;
        launcher.startAppDetailsActivity(className, pojo.userHandle.getRealHandle(), null, null);
    }

    private void launchAppStore(Context context, AppPojo app) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app.packageName)));
        } catch (ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app.packageName)));
        }
    }

    private void hibernate(Context context, AppPojo app) {
        String msg = context.getResources().getString(R.string.toast_hibernate_completed);
        if (!KissApplication.getApplication(context).getRootHandler().hibernateApp(pojo.packageName)) {
            msg = context.getResources().getString(R.string.toast_hibernate_error);
        } else {
            KissApplication.getApplication(context).getDataHandler().reloadApps();
        }

        Toast.makeText(context, String.format(msg, app.getName()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Open an activity to uninstall the current package
     */
    private void launchUninstall(Context context, AppPojo app) {
        Intent intent = new Intent(Intent.ACTION_DELETE,
                Uri.fromParts("package", app.packageName, null));
        intent.putExtra(Intent.EXTRA_USER, app.userHandle.getRealHandle());
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
                    icon = iconsHandler.getDrawableIconForPackage(className, this.pojo.userHandle);
                }
            }
        }
        DrawableUtils.setDisabled(icon, this.pojo.isDisabled());
        return icon;
    }

    @Override
    public boolean isDrawableDynamic() {
        // drawable may change because of async loading, so return true as long as icon is not cached
        // another dynamic icon is from Google Calendar
        return !isDrawableCached() || GoogleCalendarIcon.GOOGLE_CALENDAR.equals(pojo.packageName);
    }

    @Override
    public void doLaunch(Context context, View v) {
        try {
            LauncherApps launcher = ContextCompat.getSystemService(context, LauncherApps.class);
            assert launcher != null;
            Rect sourceBounds = null;
            Bundle opts = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // We're on a modern Android and can display activity animations
                // If AppResult, find the icon
                View potentialIcon = v.findViewById(R.id.item_app_icon);
                if (potentialIcon == null) {
                    // If favorite, find the icon
                    potentialIcon = v.findViewById(R.id.favorite);
                }

                if (potentialIcon != null) {
                    sourceBounds = getViewBounds(potentialIcon);

                    // If we got an icon, we create options to get a nice animation
                    opts = ActivityOptions.makeClipRevealAnimation(potentialIcon, 0, 0, potentialIcon.getMeasuredWidth(), potentialIcon.getMeasuredHeight()).toBundle();
                }
            }

            launcher.startMainActivity(className, pojo.userHandle.getRealHandle(), sourceBounds, opts);
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

    public void setCustomIcon(long dbId, Drawable drawable) {
        pojo.setCustomIconId(dbId);
        setDrawableCache(drawable);
    }

    public void clearCustomIcon() {
        pojo.setCustomIconId(0);
        setDrawableCache(null);
    }

    public String getComponentName() {
        return pojo.getComponentName();
    }
}
