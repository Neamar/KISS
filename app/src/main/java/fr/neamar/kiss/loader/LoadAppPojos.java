package fr.neamar.kiss.loader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import androidx.annotation.WorkerThread;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.ui.GoogleCalendarIcon;
import fr.neamar.kiss.utils.UserHandle;

public class LoadAppPojos extends LoadPojos<AppPojo> {

    private final TagsHandler tagsHandler;

    public LoadAppPojos(Context context) {
        super(context, "app://");
        tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected ArrayList<AppPojo> doInBackground(Void... params) {
        long start = System.nanoTime();

        ArrayList<AppPojo> apps = new ArrayList<>();

        Context ctx = context.get();
        if (ctx == null) {
            return apps;
        }

        Set<String> excludedAppList = KissApplication.getApplication(ctx).getDataHandler().getExcluded();
        Set<String> excludedFromHistoryAppList = KissApplication.getApplication(ctx).getDataHandler().getExcludedFromHistory();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserManager manager = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
            LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            // Handle multi-profile support introduced in Android 5 (#542)
            for (android.os.UserHandle profile : manager.getUserProfiles()) {
                UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);
                for (LauncherActivityInfo activityInfo : launcher.getActivityList(null, profile)) {
                    String activityName = activityInfo.getName();

                    ApplicationInfo appInfo = activityInfo.getApplicationInfo();
                    String packageName = appInfo.packageName;

                    String id = user.addUserSuffixToString(pojoScheme + appInfo.packageName + "/" + activityInfo.getName(), '/');

                    boolean isExcluded = excludedAppList.contains(AppPojo.getComponentName(appInfo.packageName, activityInfo.getName(), user));
                    boolean isExcludedFromHistory = excludedFromHistoryAppList.contains(id);

                    ComponentName className = new ComponentName(packageName, activityName);

                    AppPojo app = new AppPojo(id, packageName, activityName, user,
                            getIcon(packageName, activityName, className, user),
                            isExcluded, isExcludedFromHistory);

                    app.setName(activityInfo.getLabel().toString());

                    app.setTags(tagsHandler.getTags(app.id));

                    apps.add(app);
                }
            }
        } else {
            PackageManager manager = ctx.getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            for (ResolveInfo info : manager.queryIntentActivities(mainIntent, 0)) {
                String activityName = info.activityInfo.name;

                ApplicationInfo appInfo = info.activityInfo.applicationInfo;
                String packageName = appInfo.packageName;

                String id = pojoScheme + appInfo.packageName + "/" + info.activityInfo.name;
                boolean isExcluded = excludedAppList.contains(
                        AppPojo.getComponentName(appInfo.packageName, info.activityInfo.name, new UserHandle())
                );
                boolean isExcludedFromHistory = excludedFromHistoryAppList.contains(id);

                ComponentName className = new ComponentName(packageName, activityName);
                UserHandle userHandle = new UserHandle();

                AppPojo app = new AppPojo(id, packageName, activityName, userHandle,
                        getIcon(packageName, activityName, className, userHandle),
                        isExcluded, isExcludedFromHistory);

                app.setName(info.loadLabel(manager).toString());

                app.setTags(tagsHandler.getTags(app.id));

                apps.add(app);
            }
        }

        long end = System.nanoTime();
        Log.i("time", (end - start) / 1000000 + " milliseconds to list apps");

        return apps;
    }

    @WorkerThread
    private Future<Drawable> getIcon(String packageName, String activityName, ComponentName className,
                                     UserHandle userHandle) {
        FutureTask<Drawable> task = new FutureTask<>(new IconCallable(context, packageName,
                activityName, className, userHandle));
        IMAGE_EXCECUTOR.execute(task);
        return task;
    }

    private static final class IconCallable implements Callable<Drawable> {

        private final WeakReference<Context> weakContext;
        private final String packageName;
        private final String activityName;
        private final ComponentName className;
        private final UserHandle userHandle;

        public IconCallable(WeakReference<Context> weakContext, String packageName,
                            String activityName, ComponentName className, UserHandle userHandle) {

            this.weakContext = weakContext;
            this.packageName = packageName;
            this.activityName = activityName;
            this.className = className;
            this.userHandle = userHandle;
        }

        @Override
        public Drawable call() {
            final Context context = weakContext.get();
            if (context == null) {
                return null;
            }

            Drawable icon = null;
            if (GoogleCalendarIcon.GOOGLE_CALENDAR.equals(packageName)) {
                // Google Calendar has a special treatment and displays a custom icon every day
                icon = GoogleCalendarIcon.getDrawable(context, activityName);
            }

            if (icon == null) {
                icon = KissApplication.getApplication(context).getIconsHandler()
                        .getDrawableIconForPackage(className, userHandle);
            }

            return icon;
        }
    }

}
