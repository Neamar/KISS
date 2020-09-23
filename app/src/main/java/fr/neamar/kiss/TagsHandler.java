package fr.neamar.kiss;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.db.DBHelper;

public class TagsHandler {
    private final Context context;
    //cached tags
    private final Map<String, String> tagsCache;

    TagsHandler(Context context) {
        this.context = context;
        tagsCache = DBHelper.loadTags(this.context);
        addDefaultAliases();
    }

    public void setTags(String id, String tags) {
        // remove existing tags for id
        DBHelper.deleteTagsForId(this.context, id);
        // add to db
        DBHelper.insertTagsForId(this.context, tags, id);
        // add to cache
        tagsCache.put(id, tags);
    }

    public String getTags(String id) {
        String tag = tagsCache.get(id);
        if (tag == null) {
            return "";
        }
        return tag;
    }

    public String[] getAllTagsAsArray() {
        Set<String> tags = getAllTagsAsSet();
        return tags.toArray(new String[0]);
    }

    public Set<String> getAllTagsAsSet() {
        Set<String> tags = new HashSet<>();
        for (Map.Entry<String, String> entry : tagsCache.entrySet()) {
            tags.addAll(Arrays.asList(entry.getValue().split("\\s+")));
        }
        tags.remove("");
        return tags;
    }

    private void addDefaultAliases() {
        final PackageManager pm = context.getPackageManager();

        String phoneApp = getApp(pm, Intent.ACTION_DIAL);
        if (phoneApp != null) {
            String phoneAlias = context.getResources().getString(R.string.alias_phone);
            addAliasesPojo(phoneAlias, phoneApp);
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            String contactApp = getAppByCategory(pm, Intent.CATEGORY_APP_CONTACTS);
            if (contactApp != null) {
                String contactAlias = context.getResources().getString(R.string.alias_contacts);
                addAliasesPojo(contactAlias, contactApp);
            }

            String browserApp = getAppByCategory(pm, Intent.CATEGORY_APP_BROWSER);
            if (browserApp != null) {
                String webAlias = context.getResources().getString(R.string.alias_web);
                addAliasesPojo(webAlias, browserApp);
            }

            String mailApp = getAppByCategory(pm, Intent.CATEGORY_APP_EMAIL);
            if (mailApp != null) {
                String mailAlias = context.getResources().getString(R.string.alias_mail);
                addAliasesPojo(mailAlias, mailApp);
            }

            String marketApp = getAppByCategory(pm, Intent.CATEGORY_APP_MARKET);
            if (marketApp != null) {
                String marketAlias = context.getResources().getString(R.string.alias_market);
                addAliasesPojo(marketAlias, marketApp);
            }

            String messagingApp = getAppByCategory(pm, Intent.CATEGORY_APP_MESSAGING);
            if (messagingApp != null) {
                String messagingAlias = context.getResources().getString(R.string.alias_messaging);
                addAliasesPojo(messagingAlias, messagingApp);
            }

            String clockApp = getClockApp(pm);
            if (clockApp != null) {
                String clockAlias = context.getResources().getString(R.string.alias_clock);
                addAliasesPojo(clockAlias, clockApp);
            }
        }

    }

    private void addAliasesPojo(String aliases, String app) {
        //add aliases only if they haven't overridden by the user (not in db)
        if (!tagsCache.containsKey(app)) {
            tagsCache.put(app, aliases.replace(",", " "));
        }
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
