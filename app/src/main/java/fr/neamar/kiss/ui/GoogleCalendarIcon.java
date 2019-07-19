package fr.neamar.kiss.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

import java.util.Calendar;

/**
 * This class is used to display a custom icon for Google Calendar
 * Every day, the icon is different and display the current day
 * Credits: https://github.com/LawnchairLauncher/Lawnchair/blob/d12b30d5333c03969ad340eb9b4e1846c12a6a73/src/com/google/android/apps/nexuslauncher/DynamicIconProvider.java
 */
public class GoogleCalendarIcon {
    public static final String GOOGLE_CALENDAR = "com.google.android.calendar";

    @Nullable
    public static Drawable getDrawable(Context context, String activityName) {
        // Do nothing if using a custom icon pack
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentIconPack = prefs.getString("icons-pack", "default");
        if (!"default".equals(currentIconPack)) {
            return null;
        }

        // Otherwise, retrieve today's icon
        PackageManager pm = context.getPackageManager();
        try {
            ComponentName cn = new ComponentName(GOOGLE_CALENDAR, activityName);
            Bundle metaData = pm.getActivityInfo(cn, PackageManager.GET_META_DATA | PackageManager.GET_UNINSTALLED_PACKAGES).metaData;
            Resources resourcesForApplication = pm.getResourcesForApplication(GOOGLE_CALENDAR);
            int dayResId = getDayResId(metaData, resourcesForApplication);
            if (dayResId != 0) {
                return resourcesForApplication.getDrawable(dayResId);
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return null;
    }

    private static int getDayResId(Bundle bundle, Resources resources) {
        if (bundle != null) {
            int dateArrayId = bundle.getInt(GOOGLE_CALENDAR + ".dynamic_icons_nexus_round", 0);
            if (dateArrayId != 0) {
                try {
                    TypedArray dateIds = resources.obtainTypedArray(dateArrayId);
                    int dateId = dateIds.getResourceId(getDayOfMonth(), 0);
                    dateIds.recycle();
                    return dateId;
                } catch (Resources.NotFoundException ex) {
                }
            }
        }
        return 0;
    }

    private static int getDayOfMonth() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1;
    }
}
