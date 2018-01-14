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
import android.os.AsyncTask;
import android.os.Build;
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

    class AsyncSetImage extends AsyncTask<Void, Void, Drawable>
	{
		final private View view;
		final private ImageView image;
		AsyncSetImage( View view, ImageView image )
		{
			super();
			this.view = view;
			this.image = image;
		}

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			image.setImageResource(android.R.color.transparent);
		}

		@Override
		protected Drawable doInBackground( Void... voids )
		{
			if ( isCancelled() || view.getTag() != this )
				return null;
			return getDrawable( view.getContext() );
		}

		@Override
		protected void onPostExecute( Drawable drawable )
		{
			if ( isCancelled() || drawable == null )
				return;
			image.setImageDrawable( drawable );
			view.setTag( null );
		}
	}

    public AppResult(AppPojo appPojo) {
        super();
        this.pojo = this.appPojo = appPojo;

        className = new ComponentName(appPojo.packageName, appPojo.activityName);
    }

    @Override
    public View display(final Context context, int position, View convertView) {
    	View view = convertView;
        if (convertView == null) {
            view = inflateFromId(context, R.layout.item_app);
        }

        TextView appName = (TextView) view.findViewById(R.id.item_app_name);
        appName.setText(enrichText(appPojo.displayName, context));

        displayTags(context, view, appPojo);

        final ImageView appIcon = (ImageView) view.findViewById(R.id.item_app_icon);
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("icons-hide", false)) {
			if ( view.getTag() instanceof AsyncSetImage )
			{
				((AsyncSetImage)view.getTag()).cancel( true );
				view.setTag( null );
			}
			if( isDrawableCached() )
			{
				appIcon.setImageDrawable(getDrawable(appIcon.getContext()));
			}
			else
			{
				view.setTag( new AsyncSetImage( view, appIcon ).execute() );
			}
		}
        else {
            appIcon.setVisibility(View.INVISIBLE);
        }
        return view;
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
                // remove item since it will be hidden
                parent.removeResult(this);
                excludeFromAppList(context, appPojo);
                return true;
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

    boolean isDrawableCached()
	{
		return icon != null;
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
