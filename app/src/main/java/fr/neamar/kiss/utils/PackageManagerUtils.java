package fr.neamar.kiss.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public class PackageManagerUtils {

    /**
     * Method to enable/disable a specific component
     */
    public static void enableComponent(Context ctx, Class component, boolean enabled) {
        PackageManager pm = ctx.getPackageManager();
        ComponentName cn = new ComponentName(ctx, component);
        pm.setComponentEnabledSetting(cn,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

}
