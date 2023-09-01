package fr.neamar.kiss;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.AlarmClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.db.DBHelper;

public class TagsHandler {
    private final Context context;
    // cached tags
    private final Map<String, String> tagsCache;

    TagsHandler(Context context) {
        this.context = context;
        tagsCache = DBHelper.loadTags(this.context);
        addDefaultAliases();
    }

    public void setTags(String id, String tags) {
        // sanitize tags
        tags = tags.trim().toLowerCase(Locale.getDefault());
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
        for (String value : tagsCache.values()) {
            tags.addAll(Arrays.asList(value.split("\\s+")));
        }
        tags.remove("");
        return tags;
    }

    public Map<String, String> getTags() {
        return tagsCache;
    }

    public void clearTags() {
        tagsCache.clear();
        DBHelper.deleteTags(this.context);
    }

    private void addDefaultAliases() {
        final PackageManager pm = context.getPackageManager();

        List<String> phoneApps = getApps(pm, Intent.ACTION_DIAL);
        String phoneAlias = context.getResources().getString(R.string.alias_phone);
        addDefaultAliases(phoneAlias, phoneApps);

        List<String> contactApps = getAppsByCategory(pm, Intent.CATEGORY_APP_CONTACTS);
        String contactAlias = context.getResources().getString(R.string.alias_contacts);
        addDefaultAliases(contactAlias, contactApps);

        List<String> browserApps = getAppsByCategory(pm, Intent.CATEGORY_APP_BROWSER);
        String webAlias = context.getResources().getString(R.string.alias_web);
        addDefaultAliases(webAlias, browserApps);

        List<String> mailApps = getAppsByCategory(pm, Intent.CATEGORY_APP_EMAIL);
        String mailAlias = context.getResources().getString(R.string.alias_mail);
        addDefaultAliases(mailAlias, mailApps);

        List<String> marketApps = getAppsByCategory(pm, Intent.CATEGORY_APP_MARKET);
        String marketAlias = context.getResources().getString(R.string.alias_market);
        addDefaultAliases(marketAlias, marketApps);

        List<String> messagingApps = getAppsByCategory(pm, Intent.CATEGORY_APP_MESSAGING);
        String messagingAlias = context.getResources().getString(R.string.alias_messaging);
        addDefaultAliases(messagingAlias, messagingApps);

        List<String> clockApps = getClockApps(pm);
        String clockAlias = context.getResources().getString(R.string.alias_clock);
        addDefaultAliases(clockAlias, clockApps);
    }

    private void addDefaultAliases(@NonNull String aliases, @NonNull List<String> apps) {
        for (String app : apps) {
            addDefaultAlias(aliases, app);
        }
    }

    private void addDefaultAlias(String aliases, String app) {
        // add aliases only if they haven't overridden by the user (not in db)
        if (!tagsCache.containsKey(app)) {
            tagsCache.put(app, aliases.replace(",", " ").trim().toLowerCase(Locale.getDefault()));
        }
    }

    @NonNull
    private List<String> getApps(PackageManager pm, String action) {
        Intent lookingFor = new Intent(action, null);
        return getApps(pm, lookingFor);
    }

    @NonNull
    private List<String> getAppsByCategory(PackageManager pm, String category) {
        Intent lookingFor = new Intent(Intent.ACTION_MAIN, null);
        lookingFor.addCategory(category);
        return getApps(pm, lookingFor);
    }

    @NonNull
    private List<String> getApps(PackageManager pm, Intent lookingFor) {
        List<ResolveInfo> list = pm.queryIntentActivities(lookingFor, 0);
        List<String> apps = new ArrayList<>(list.size());
        for (ResolveInfo resolveInfo : list) {
            String app = "app://" + resolveInfo.activityInfo.applicationInfo.packageName + "/" + resolveInfo.activityInfo.name;
            apps.add(app);
        }
        return apps;
    }

    @NonNull
    private List<String> getClockApps(PackageManager pm) {
        List<String> clockApps = new ArrayList<>();

        // check for clock by intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent alarmClockIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            List<String> appsByIntent = getApps(pm, alarmClockIntent);
            clockApps.addAll(appsByIntent);
        }

        // Known clock implementations
        // See http://stackoverflow.com/questions/3590955/intent-to-launch-the-clock-application-on-android
        String[][] clockImpls = {
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

                clockApps.add("app://" + packageName + "/" + className);
            } catch (PackageManager.NameNotFoundException e) {
                // Try next suggestion, this one does not exists on the phone.
            }
        }

        return clockApps;
    }
}
