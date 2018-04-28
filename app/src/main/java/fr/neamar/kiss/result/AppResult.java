package fr.neamar.kiss.result;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.FuzzyScore;
import fr.neamar.kiss.utils.SpaceTokenizer;

public class AppResult extends Result {
    private final AppPojo appPojo;
    private final ComponentName className;
    private Drawable icon = null;

    AppResult(AppPojo appPojo) {
        super(appPojo);
        this.appPojo = appPojo;

        className = new ComponentName(appPojo.packageName, appPojo.activityName);
    }

    @Override
    public View display(final Context context, int position, View convertView, FuzzyScore fuzzyScore) {
        View view = convertView;
        if (convertView == null) {
            view = inflateFromId(context, R.layout.item_app);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        TextView appName = view.findViewById(R.id.item_app_name);

        displayHighlighted(appPojo.normalizedName, appPojo.getName(), fuzzyScore, appName, context);

        TextView tagsView = view.findViewById(R.id.item_app_tag);
        // Hide tags view if tags are empty
        if (appPojo.getTags().isEmpty()) {
            tagsView.setVisibility(View.GONE);
        } else if (displayHighlighted(appPojo.normalizedTags, appPojo.getTags(),
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
        return view;
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        if ((!(context instanceof MainActivity)) || (((MainActivity) context).isViewingSearchResults())) {
            adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_exclude));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_tags_edit));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_app_details));

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

        //append root menu if available
        if (KissApplication.getApplication(context).getRootHandler().isRootActivated() && KissApplication.getApplication(context).getRootHandler().isRootAvailable()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_hibernate));
        }

        return inflatePopupMenu(adapter, context);
    }

    @Override
    protected Boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId) {
        switch (stringId) {
            case R.string.menu_app_details:
                launchAppDetails(context, appPojo);
                return true;
            case R.string.menu_app_uninstall:
                launchUninstall(context, appPojo);
                return true;
            case R.string.menu_app_hibernate:
                hibernate(context, appPojo);
                return true;
            case R.string.menu_exclude:
                // remove item since it will be hidden
                parent.removeResult(this);
                excludeFromAppList(context, appPojo);
                return true;
            case R.string.menu_tags_edit:
                launchEditTagsDialog(context, appPojo);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId);
    }

    private void excludeFromAppList(Context context, AppPojo appPojo) {
        KissApplication.getApplication(context).getDataHandler().addToExcluded(appPojo.packageName, appPojo.userHandle);
        //remove app pojo from appProvider results - no need to reset handler
        KissApplication.getApplication(context).getDataHandler().getAppProvider().removeApp(appPojo);
        KissApplication.getApplication(context).getDataHandler().removeFromFavorites((MainActivity) context, appPojo.id);
        Toast.makeText(context, R.string.excluded_app_list_added, Toast.LENGTH_LONG).show();

    }


    private void launchEditTagsDialog(final Context context, final AppPojo app) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.tags_add_title));

        // Create the tag dialog
        final View v = View.inflate(context, R.layout.tags_dialog, null);
        final MultiAutoCompleteTextView tagInput = v.findViewById(R.id.tag_input);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, KissApplication.getApplication(context).getDataHandler().getTagsHandler().getAllTagsAsArray());
        tagInput.setTokenizer(new SpaceTokenizer());
        tagInput.setText(appPojo.getTags());

        tagInput.setAdapter(adapter);
        builder.setView(v);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Refresh tags for given app
                app.setTags(tagInput.getText().toString());
                KissApplication.getApplication(context).getDataHandler().getTagsHandler().setTags(app.id, app.getTags());
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

        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
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

    private void hibernate(Context context, AppPojo app) {
        String msg = context.getResources().getString(R.string.toast_hibernate_completed);
        if (!KissApplication.getApplication(context).getRootHandler().hibernateApp(appPojo.packageName)) {
            msg = context.getResources().getString(R.string.toast_hibernate_error);
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
        synchronized (this) {
            if (icon == null) {
                icon = KissApplication.getApplication(context).getIconsHandler()
                        .getDrawableIconForPackage(className, this.appPojo.userHandle);
            }

            return icon;
        }
    }

    @Override
    public void doLaunch(Context context, View v) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcher != null;
                launcher.startMainActivity(className, appPojo.userHandle.getRealHandle(), v.getClipBounds(), null);
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(className);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    intent.setSourceBounds(v.getClipBounds());
                }

                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException | NullPointerException e) {
            // Application was just removed?
            // (null pointer exception can be thrown on Lollipop+ when app is missing)
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }
    }
}
