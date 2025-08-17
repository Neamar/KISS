package fr.neamar.kiss.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherUserInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackageManagerUtils {

    private static final String TAG = PackageManagerUtils.class.getSimpleName();

    /**
     * Method to enable/disable a specific component
     */
    public static void enableComponent(Context ctx, Class<?> component, boolean enabled) {
        PackageManager pm = ctx.getPackageManager();
        ComponentName cn = new ComponentName(ctx, component);
        pm.setComponentEnabledSetting(cn,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Search best matching app for given intent.
     *
     * @param context context
     * @param intent  intent
     * @return ResolveInfo for best matching app by intent
     */
    public static ResolveInfo getBestResolve(Context context, Intent intent) {
        if (intent == null) {
            return null;
        }

        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> matches = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        final int size = matches.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return matches.get(0);
        }

        // Try finding preferred activity, otherwise detect disambig
        final ResolveInfo foundResolve = packageManager.resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        final boolean foundDisambig = (foundResolve.match &
                IntentFilter.MATCH_CATEGORY_MASK) == 0;

        if (!foundDisambig) {
            // Found concrete match, so return directly
            return foundResolve;
        }

        // Accept first system app
        ResolveInfo firstSystem = null;
        for (ResolveInfo info : matches) {
            final boolean isSystem = (info.activityInfo.applicationInfo.flags
                    & ApplicationInfo.FLAG_SYSTEM) != 0;

            if (isSystem && firstSystem == null) firstSystem = info;
        }

        // Return first system found, otherwise first from list
        return firstSystem != null ? firstSystem : matches.get(0);
    }

    /**
     * @param context context
     * @param intent  intent
     * @return component name of best matching app for given intent
     */
    public static ComponentName getComponentName(Context context, Intent intent) {
        ResolveInfo resolveInfo = getBestResolve(context, intent);
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        }
        return null;
    }

    /**
     * @param context context
     * @param intent  intent
     * @return label of best matching app for given intent
     */
    public static String getLabel(Context context, Intent intent) {
        ResolveInfo resolveInfo = PackageManagerUtils.getBestResolve(context, intent);
        if (resolveInfo != null) {
            return String.valueOf(resolveInfo.loadLabel(context.getPackageManager()));
        } else {
            Log.w(TAG, "Unable to get label from intent: " + intent);
            return null;
        }
    }

    /**
     * @param context
     * @param packageName
     * @param userHandle
     * @return label from package name
     */
    @Nullable
    public static String getLabel(@NonNull Context context, @NonNull String packageName, @NonNull UserHandle userHandle) {
        ApplicationInfo appInfo = getApplicationInfo(context, packageName, userHandle);
        if (appInfo != null) {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getApplicationLabel(appInfo).toString();
        } else {
            Log.w(TAG, "Unable to get label from package: " + packageName);
            return null;
        }
    }

    @Nullable
    public static String getLabel(@NonNull Context context, @NonNull ComponentName componentName, @NonNull UserHandle user) {
        ActivityInfo activityInfo = getActivityInfo(context, componentName, user);
        if (activityInfo != null) {
            PackageManager packageManager = context.getPackageManager();
            return activityInfo.loadLabel(packageManager).toString();
        } else {
            Log.w(TAG, "Unable to get label from component: " + componentName);
            return null;
        }
    }

    @Nullable
    public static ApplicationInfo getApplicationInfo(@NonNull Context context, @NonNull String packageName, @NonNull UserHandle user) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                return launcherApps.getApplicationInfo(packageName, 0, user.getRealHandle());
            } else {
                return context.getPackageManager().getApplicationInfo(packageName, 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Nullable
    private static ActivityInfo getActivityInfo(@NonNull Context context, @NonNull ComponentName componentName, @NonNull UserHandle user) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LauncherActivityInfo info = getLauncherActivityInfo(context, componentName, user);
            if (info != null) {
                return info.getActivityInfo();
            }
        }
        try {
            return context.getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Nullable
    private static LauncherActivityInfo getLauncherActivityInfo(@NonNull Context context, @NonNull ComponentName componentName, @NonNull UserHandle user) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            List<LauncherActivityInfo> activities = launcherApps.getActivityList(componentName.getPackageName(), user.getRealHandle());
            for (LauncherActivityInfo activity : activities) {
                if (activity.getComponentName().equals(componentName)) {
                    return activity;
                }
            }
        }
        return null;
    }


    /**
     * @param context       context
     * @param componentName componentName
     * @return launching component name for given component
     */
    public static ComponentName getLaunchingComponent(Context context, ComponentName componentName, UserHandle user) {
        if (componentName == null) {
            return null;
        }
        ComponentName launchingComponent = getLaunchingComponent(context, componentName.getPackageName(), user);
        if (launchingComponent != null) {
            return launchingComponent;
        }
        return componentName;
    }

    /**
     * @param context     context
     * @param packageName package name
     * @return launching component name for given package
     */
    public static ComponentName getLaunchingComponent(Context context, String packageName, UserHandle user) {
        if (packageName == null) {
            return null;
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !user.isCurrentUser()) {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            List<LauncherActivityInfo> activities = launcherApps.getActivityList(packageName, user.getRealHandle());
            if (!activities.isEmpty()) {
                List<ComponentName> componentNames = new ArrayList<>();
                for (LauncherActivityInfo activity : activities) {
                    componentNames.add(activity.getComponentName());
                }
                Collections.sort(componentNames);
                return componentNames.get(0);
            }
        } else {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                return launchIntent.getComponent();
            }
        }
        return null;
    }

    /**
     * Creates intent to start activity with given uri.
     * Uri must have some given criteria to work:
     * <ul>
     * <li>it must contain an explicit schema (absolute)</li>
     * <li>the schema specific part must be longer than 2 (//...) so is some result that can be handled</li>
     * </ul>
     *
     * @param uri
     * @return intent
     */
    public static @NonNull Intent createUriIntent(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Get activity icon by component name using launcher apps, use icon provided by package manager as fallback.
     *
     * @param ctx           context
     * @param componentName component name
     * @param userHandle    handle for current user
     * @return icon for given componentName
     */
    public static Drawable getActivityIcon(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                LauncherActivityInfo info = getLauncherActivityInfo(ctx, componentName, userHandle);
                if (info != null) {
                    Drawable drawable = info.getIcon(0);
                    if (drawable != null) {
                        return DrawableUtils.getThemedDrawable(ctx, drawable);
                    }
                }
            } catch (SecurityException e) {
                // https://github.com/Neamar/KISS/issues/1715
                // not sure how to avoid it so we catch and ignore
                Log.e(TAG, "Unable to find activity icon for component " + componentName.toShortString(), e);
            }
        }

        return getActivityIcon(ctx, componentName);
    }

    /**
     * Get activity icon by component name using package manager.
     *
     * @param ctx           context
     * @param componentName component name
     * @return icon for given componentName
     */
    private static Drawable getActivityIcon(@NonNull Context ctx, @NonNull ComponentName componentName) {
        try {
            Drawable drawable = ctx.getPackageManager().getActivityIcon(componentName);
            return DrawableUtils.getThemedDrawable(ctx, drawable);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find activity icon for component " + componentName.toShortString(), e);
        }

        // This should never happen, let's just return the application icon
        return getApplicationIcon(ctx, componentName.getPackageName());
    }

    /**
     * Get application icon by package name using package manager.
     *
     * @param ctx         context
     * @param packageName package name
     * @return icon for given packageName
     */
    public static Drawable getApplicationIcon(@NonNull Context ctx, @NonNull String packageName) {
        try {
            Drawable drawable = ctx.getPackageManager().getApplicationIcon(packageName);
            return DrawableUtils.getThemedDrawable(ctx, drawable);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find application icon for package " + packageName, e);
        }

        // This should never happen, let's just return the generic activity icon
        Drawable drawable = ctx.getPackageManager().getDefaultActivityIcon();
        return DrawableUtils.getThemedDrawable(ctx, drawable);
    }

    public static boolean isAppSuspended(ApplicationInfo appInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return (appInfo.flags & ApplicationInfo.FLAG_SUSPENDED) != 0;
        } else {
            return false;
        }
    }

    public static boolean isAppSuspended(@NonNull Context context, @NonNull String packageName, @NonNull UserHandle user) {
        final ApplicationInfo appInfo = getApplicationInfo(context, packageName, user);
        return appInfo != null && isAppSuspended(appInfo);
    }

    public static boolean isPrivateProfile(@NonNull LauncherApps launcherApps, @NonNull android.os.UserHandle userHandle) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            LauncherUserInfo info = launcherApps.getLauncherUserInfo(userHandle);
            return info != null && UserManager.USER_TYPE_PROFILE_PRIVATE.equalsIgnoreCase(info.getUserType());
        }
        return false;
    }

}
