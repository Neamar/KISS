package fr.neamar.kiss.result;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Locale;

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
import fr.neamar.kiss.utils.FuzzyScore;
import fr.neamar.kiss.utils.SpaceTokenizer;

public class AppResult extends Result {
    private static final String TAG = AppResult.class.getSimpleName();
    private final AppPojo appPojo;
    private final ComponentName className;
    private volatile Drawable icon = null;

    AppResult(AppPojo appPojo) {
        super(appPojo);
        this.appPojo = appPojo;

        className = new ComponentName(appPojo.packageName, appPojo.activityName);
    }

    @NonNull
    @Override
    public View display(final Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null) {
            view = inflateFromId(context, R.layout.item_app, parent);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        TextView appName = view.findViewById(R.id.item_app_name);

        displayHighlighted(appPojo.normalizedName, appPojo.getName(), fuzzyScore, appName, context);

        TextView tagsView = view.findViewById(R.id.item_app_tag);
        // Hide tags view if tags are empty
        if (appPojo.getTags().isEmpty()) {
            tagsView.setVisibility(View.GONE);
        } else if (displayHighlighted(appPojo.getNormalizedTags(), appPojo.getTags(),
                fuzzyScore, tagsView, context) || prefs.getBoolean("tags-visible", true)) {
            tagsView.setVisibility(View.VISIBLE);
        } else {
            tagsView.setVisibility(View.GONE);
        }

        final ImageView appIcon = view.findViewById(R.id.item_app_icon);
        if (!prefs.getBoolean("icons-hide", false)) {
            if (appIcon.getTag() instanceof ComponentName && className.equals(appIcon.getTag())) {
                icon = appIcon.getDrawable();
            }
            this.setAsyncDrawable(appIcon);
        } else {
            appIcon.setImageDrawable(null);
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String packageKey = getPackageKey();

            SharedPreferences notificationPrefs = context.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
            ImageView notificationView = view.findViewById(R.id.item_notification_dot);
            notificationView.setVisibility(notificationPrefs.contains(packageKey) ? View.VISIBLE : View.GONE);
            notificationView.setTag(packageKey);

            int dotColor = UIColors.getNotificationDotColor(context);
            notificationView.setColorFilter(dotColor);
        }

        return view;
    }

    private String getPackageKey() {
        return appPojo.getPackageKey();
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        if (!(context instanceof MainActivity) || ((MainActivity) context).isViewingSearchResults()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_exclude));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_app_rename));
        // only display this option if we're using a custom icon pack, as it is not useful otherwise
        if (KissApplication.getApplication(context).getIconsHandler().getCustomIconPack() != null)
            adapter.add(new ListPopup.Item(context, R.string.menu_custom_icon));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_tags_edit));
        adapter.add(new ListPopup.Item(context, R.string.menu_app_details));
        adapter.add(new ListPopup.Item(context, R.string.menu_app_store));

        try {
            // app installed under /system can't be uninstalled
            boolean isSameProfile = true;
            ApplicationInfo ai;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                LauncherActivityInfo info = launcher.getActivityList(this.appPojo.packageName, this.appPojo.userHandle.getRealHandle()).get(0);
                ai = info.getApplicationInfo();

                isSameProfile = this.appPojo.userHandle.isCurrentUser();
            } else {
                ai = context.getPackageManager().getApplicationInfo(this.appPojo.packageName, 0);
            }

            // Need to AND the flags with SYSTEM:
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && isSameProfile) {
                adapter.add(new ListPopup.Item(context, R.string.menu_app_uninstall));
            }
        } catch (NameNotFoundException | IndexOutOfBoundsException e) {
            // should not happen
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
            launchAppDetails(context, appPojo);
            return true;
        } else if (stringId == R.string.menu_app_store) {
            launchAppStore(context, appPojo);
            return true;
        } else if (stringId == R.string.menu_app_uninstall) {
            launchUninstall(context, appPojo);
            return true;
        } else if (stringId == R.string.menu_app_hibernate) {
            hibernate(context, appPojo);
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
                        excludeFromHistory(context, appPojo);
                        return true;
                    case EXCLUDE_KISS_ID:
                        excludeFromKiss(context, appPojo, parent);
                        return true;
                }

                return true;
            });

            popupExcludeMenu.show();
            return true;
        } else if (stringId == R.string.menu_tags_edit) {
            launchEditTagsDialog(context, parent, appPojo);
            return true;
        } else if (stringId == R.string.menu_app_rename) {
            launchRenameDialog(context, parent, appPojo);
            return true;
        } else if (stringId == R.string.menu_custom_icon) {
            launchCustomIcon(context, parent);
            return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private void excludeFromHistory(Context context, AppPojo appPojo) {
        // add to excluded from history app list
        KissApplication.getApplication(context).getDataHandler().addToExcludedFromHistory(appPojo);
        // remove from history
        removeFromHistory(context);
        // inform user
        Toast.makeText(context, R.string.excluded_app_history_added, Toast.LENGTH_LONG).show();
    }

    private void excludeFromKiss(Context context, AppPojo appPojo, final RecordAdapter parent) {
        // remove item since it will be hidden
        parent.removeResult(context, AppResult.this);

        KissApplication.getApplication(context).getDataHandler().addToExcluded(appPojo);
        // In case the newly excluded app was in a favorite, refresh them
        ((MainActivity) context).onFavoriteChange();
        Toast.makeText(context, R.string.excluded_app_list_added, Toast.LENGTH_LONG).show();
    }

    private void launchEditTagsDialog(final Context context, RecordAdapter parent, final AppPojo app) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.tags_add_title));

        // Create the tag dialog
        final View v = View.inflate(context, R.layout.tags_dialog, null);
        final MultiAutoCompleteTextView tagInput = v.findViewById(R.id.tag_input);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, KissApplication.getApplication(context).getDataHandler().getTagsHandler().getAllTagsAsArray());
        tagInput.setTokenizer(new SpaceTokenizer());
        tagInput.setText(app.getTags());
        tagInput.setAdapter(adapter);
        builder.setView(v);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            dialog.dismiss();
            // Refresh tags for given app
            app.setTags(tagInput.getText().toString().trim().toLowerCase(Locale.ROOT));
            KissApplication.getApplication(context).getDataHandler().getTagsHandler().setTags(app.id, app.getTags());
            // Show toast message
            String msg = context.getResources().getString(R.string.tags_confirmation_added);
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            // We'll need to reset the list view to its previous transcript mode,
            // but it has to happen *after* the keyboard is hidden, otherwise scroll will be reset
            // Let's wait for half a second, that's ugly but we don't have any other option :(
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                }
            }, 500);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // See comment above
                    parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                }
            }, 500);

        });

        parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void launchRenameDialog(final Context context, RecordAdapter parent, final AppPojo app) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.app_rename_title));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.rename_dialog);
        } else {
            builder.setView(View.inflate(context, R.layout.rename_dialog, null));
        }

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

            // We'll need to reset the list view to its previous transcript mode,
            // but it has to happen *after* the keyboard is hidden, otherwise scroll will be reset
            // Let's wait for half a second, that's ugly but we don't have any other option :(
            final Handler handler = new Handler();
            handler.postDelayed(() -> parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL), 500);
        });
        builder.setNegativeButton(R.string.custom_name_set_default, (dialog, which) -> {
            dialog.dismiss();

            // Get initial name
            String name = null;
            PackageManager pm = context.getPackageManager();
            try {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(app.packageName, 0);
                name = applicationInfo.loadLabel(pm).toString();
            } catch (NameNotFoundException ignored) {
            }

            // Set name
            if (name != null) {
                app.setName(name);
                KissApplication.getApplication(context).getDataHandler().removeRenameApp(getComponentName(), name);

                // Show toast message
                String msg = context.getResources().getString(R.string.app_rename_confirmation, appPojo.getName());
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }

            final Handler handler = new Handler();
            handler.postDelayed(() -> parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL), 500);
        });
        builder.setNeutralButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();

            final Handler handler = new Handler();
            handler.postDelayed(() -> parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL), 500);
        });

        parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
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
            args.putParcelable("userHandle", appPojo.userHandle);
            args.putString("componentName", appPojo.getComponentName());
            args.putLong("customIcon", appPojo.getCustomIconId());
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
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;
            launcher.startAppDetailsActivity(className, appPojo.userHandle.getRealHandle(), null, null);
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", app.packageName, null));
            context.startActivity(intent);
        }
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
        if (!KissApplication.getApplication(context).getRootHandler().hibernateApp(appPojo.packageName)) {
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
        if (!isDrawableCached()) {
            synchronized (this) {
                if (!isDrawableCached()) {
                    IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
                    icon = iconsHandler.getDrawableIconForPackage(className, this.appPojo.userHandle);
                }
            }
        }
        return icon;
    }

    @Override
    public boolean isDrawableDynamic() {
        // drawable may change because of async loading, so return true as long as icon is not cached
        // another dynamic icon is from Google Calendar
        return !isDrawableCached() || GoogleCalendarIcon.GOOGLE_CALENDAR.equals(appPojo.packageName);
    }

    @Override
    public void doLaunch(Context context, View v) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcher != null;
                Rect sourceBounds = null;
                Bundle opts = null;

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

                launcher.startMainActivity(className, appPojo.userHandle.getRealHandle(), sourceBounds, opts);
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(className);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    intent.setSourceBounds(getViewBounds(v));
                }

                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException | NullPointerException | SecurityException e) {
            Log.w(TAG, "Unable to launch activity", e);
            // Application was just removed?
            // (null pointer exception can be thrown on Lollipop+ when app is missing)
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }
    }

    private Rect getViewBounds(View v) {
        if (v == null) {
            return null;
        }

        int[] l = new int[2];
        v.getLocationOnScreen(l);
        return new Rect(l[0], l[1], l[0] + v.getWidth(), l[1] + v.getHeight());
    }

    public void setCustomIcon(long dbId, Drawable drawable) {
        appPojo.setCustomIconId(dbId);
        setDrawableCache(drawable);
    }

    public void clearCustomIcon() {
        appPojo.setCustomIconId(0);
        setDrawableCache(null);
    }

    public long getCustomIcon() {
        return appPojo.getCustomIconId();
    }

    public String getComponentName() {
        return appPojo.getComponentName();
    }
}
