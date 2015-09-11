package fr.neamar.kiss.loader;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AliasPojo;

public class LoadAliasPojos extends LoadPojos<AliasPojo> {

    public LoadAliasPojos(Context context) {
        super(context, "none://");
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    protected ArrayList<AliasPojo> doInBackground(Void... params) {
        final PackageManager pm = context.getPackageManager();
        ArrayList<AliasPojo> alias = new ArrayList<>();

        String phoneApp = getApp(pm, Intent.ACTION_DIAL);
        if (phoneApp != null) {
            String phoneAlias = context.getResources().getString(R.string.alias_phone);
            addAliasesPojo(alias, phoneAlias.split(","), phoneApp);
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            String contactApp = getAppByCategory(pm, Intent.CATEGORY_APP_CONTACTS);
            if (contactApp != null) {
                String contactAlias = context.getResources().getString(R.string.alias_contacts);
                addAliasesPojo(alias, contactAlias.split(","), contactApp);
            }

            String browserApp = getAppByCategory(pm, Intent.CATEGORY_APP_BROWSER);
            if (browserApp != null) {
                String webAlias = context.getResources().getString(R.string.alias_web);
                addAliasesPojo(alias, webAlias.split(","), browserApp);
            }

            String mailApp = getAppByCategory(pm, Intent.CATEGORY_APP_EMAIL);
            if (mailApp != null) {
                String mailAlias = context.getResources().getString(R.string.alias_mail);
                addAliasesPojo(alias, mailAlias.split(","), mailApp);
            }

            String marketApp = getAppByCategory(pm, Intent.CATEGORY_APP_MARKET);
            if (marketApp != null) {
                String marketAlias = context.getResources().getString(R.string.alias_market);
                addAliasesPojo(alias, marketAlias.split(","), marketApp);
            }

            String messagingApp = getAppByCategory(pm, Intent.CATEGORY_APP_MESSAGING);
            if (messagingApp != null) {
                String messagingAlias = context.getResources().getString(R.string.alias_messaging);
                addAliasesPojo(alias, messagingAlias.split(","), messagingApp);
            }

            String clockApp = getClockApp(pm);
            if (clockApp != null) {
                String clockAlias = context.getResources().getString(R.string.alias_clock);
                addAliasesPojo(alias, clockAlias.split(","), clockApp);
            }
        }

        return alias;

    }

    private void addAliasesPojo(ArrayList<AliasPojo> alias, String[] aliases, String appInfo) {
        for (String a : aliases) {
            alias.add(makeAliasPojo(a, appInfo));
        }
    }

    private AliasPojo makeAliasPojo(String alias, String appInfo) {
        AliasPojo aliasPojo = new AliasPojo();
        aliasPojo.alias = alias;
        aliasPojo.app = appInfo;

        return aliasPojo;
    }

    private String getApp(PackageManager pm, String action) {
        Intent lookingFor = new Intent(action, null);
        return getApp(pm, lookingFor);
    }

    private String getAppByCategory(PackageManager pm, String category) {
        Intent lookingFor = new Intent(Intent.ACTION_MAIN, null);
        lookingFor.addCategory(category);
        return getApp(pm, lookingFor);
    }

    private String getApp(PackageManager pm, Intent lookingFor) {
        List<ResolveInfo> list = pm.queryIntentActivities(lookingFor, 0);
        if (list.size() == 0) {
            return null;
        } else {
            return "app://" + list.get(0).activityInfo.applicationInfo.packageName + "/"
                    + list.get(0).activityInfo.name;
        }
    }

    private String getClockApp(PackageManager pm) {
        Intent alarmClockIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        // Known clock implementations
        // See http://stackoverflow.com/questions/3590955/intent-to-launch-the-clock-application-on-android
        String clockImpls[][] = {
                // Nexus
                {"com.android.deskclock", "com.android.deskclock.DeskClock"},
                // Samsung
                {"com.sec.android.app.clockpackage", "com.sec.android.app.clockpackage.ClockPackage"},
                // HTC
                {"com.htc.android.worldclock", "com.htc.android.worldclock.WorldClockTabControl"},
                // Standard Android
                {"com.android.deskclock", "com.android.deskclock.AlarmClock"},
                // New Android versions
                {"com.google.android.deskclock", "com.android.deskclock.AlarmClock"},
                // Froyo
                {"com.google.android.deskclock", "com.android.deskclock.DeskClock"},
                // Motorola
                {"com.motorola.blur.alarmclock", "com.motorola.blur.alarmclock.AlarmClock"},
                // Sony
                {"com.sonyericsson.organizer", "com.sonyericsson.organizer.Organizer_WorldClock"},
                // ASUS Tablets
                {"com.asus.deskclock", "com.asus.deskclock.DeskClock"}
        };

        for (String[] clockImpl : clockImpls) {
            String packageName = clockImpl[0];
            String className = clockImpl[1];
            try {
                ComponentName cn = new ComponentName(packageName, className);

                pm.getActivityInfo(cn, PackageManager.GET_META_DATA);
                alarmClockIntent.setComponent(cn);

                return "app://" + packageName + "/" + className;
            } catch (PackageManager.NameNotFoundException e) {
                // Try next suggestion, this one does not exists on the phone.
            }
        }

        return null;
    }
}
