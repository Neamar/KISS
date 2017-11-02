package fr.neamar.kiss.result;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.utils.SpaceTokenizer;

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
        if (v == null) {
            v = inflateFromId(context, R.layout.item_app);
        }

        TextView appName = (TextView) v.findViewById(R.id.item_app_name);
        appName.setText(enrichText(appPojo.displayName, context));

        TextView tagsView = (TextView) v.findViewById(R.id.item_app_tag);
        //Hide tags view if tags are empty or if user has selected to hide them and the query doesnt match tags
        if (appPojo.displayTags.isEmpty() ||
                ((!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("tags-visible", true)) && (appPojo.displayTags.equals(appPojo.tags)))) {
            tagsView.setVisibility(View.GONE);
        }
        else {
            tagsView.setVisibility(View.VISIBLE);
            tagsView.setText(enrichText(appPojo.displayTags, context));
        }

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

    @Override
    protected PopupMenu buildPopupMenu(Context context, final RecordAdapter parent, View parentView) {
        PopupMenu menu = inflatePopupMenu(R.menu.menu_item_app, context, parentView);

        if ((context instanceof MainActivity) && (!((MainActivity)context).isOnSearchView())) {
            menu.getMenu().removeItem(R.id.item_remove);
        }
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
                menu.getMenuInflater().inflate(R.menu.menu_item_app_uninstall, menu.getMenu());
            }
        } catch (NameNotFoundException | IndexOutOfBoundsException e) {
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
                return true;
            case R.id.item_app_hibernate:
                hibernate(context, appPojo);
                return true;
            case R.id.item_exclude:
                // remove item since it will be hiddden
                parent.removeResult(this);
                excludeFromAppList(context, appPojo);
                return true;
            case R.id.item_tags_edit:
                launchEditTagsDialog(context, appPojo);
                break;
        }

        return super.popupMenuClickHandler(context, parent, item);
    }

    private void excludeFromAppList(Context context, AppPojo appPojo) {
        KissApplication.getDataHandler(context).addToExcluded(appPojo.packageName, appPojo.userHandle);
        //remove app pojo from appProvider results - no need to reset handler
        KissApplication.getDataHandler(context).getAppProvider().removeApp(appPojo);
        KissApplication.getDataHandler(context).removeFromFavorites((MainActivity) context, appPojo.id);
        Toast.makeText(context, R.string.excluded_app_list_added, Toast.LENGTH_LONG).show();

    }


    private void launchEditTagsDialog(final Context context, final AppPojo app) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.tags_add_title));

        // Create the tag dialog

        final View v = LayoutInflater.from(context).inflate(R.layout.tags_dialog, null);
        final MultiAutoCompleteTextView tagInput = (MultiAutoCompleteTextView) v.findViewById(R.id.tag_input);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_dropdown_item_1line, KissApplication.getDataHandler(context).getTagsHandler().getAllTagsAsArray());
        tagInput.setTokenizer(new SpaceTokenizer());
        tagInput.setText(appPojo.tags);

        tagInput.setAdapter(adapter);
        builder.setView(v);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                KissApplication.getDataHandler(context).getTagsHandler().setTags(app.id, tagInput.getText().toString());
                // Refresh tags for given app
                app.setTags(tagInput.getText().toString());
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
		dialog.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE );

		dialog.show();
    }

    /**
     * Open an activity displaying details regarding the current package
     */
    private void launchAppDetails(Context context, AppPojo app) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
			launcher.startAppDetailsActivity(className, appPojo.userHandle.getRealHandle(), null, null);
		} else {
			Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
					Uri.fromParts("package", app.packageName, null));
			context.startActivity(intent);
			}
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
        
        if (icon == null) {
             icon = KissApplication.getIconsHandler(context).getDrawableIconForPackage(className, this.appPojo.userHandle);
        }
                
        return icon;
        
    }

	@Override
	public void doLaunch(Context context, View v) {
		try {
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
				launcher.startMainActivity(className, appPojo.userHandle.getRealHandle(), v.getClipBounds(), null);
			} else {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setComponent(className);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				
				if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
					intent.setSourceBounds(v.getClipBounds());
				}
				
				context.startActivity(intent);
			}
		 } catch (ActivityNotFoundException e) {
			// Application was just removed?
			Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
		}
	}
}
