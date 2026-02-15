package fr.neamar.kiss.preference;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import fr.neamar.kiss.DummyActivity;

public class DefaultLauncherPreference {

    public void onDialogClosed(Context context, boolean positiveResult) {
        if (positiveResult) {

            // get packet manager
            PackageManager packageManager = context.getPackageManager();
            // get dummyActivity
            ComponentName componentName = new ComponentName(context, DummyActivity.class);
            // enable dummyActivity (it starts disabled in the manifest.xml)
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            // create a new (implicit) intent with MAIN action
            Intent intent = new Intent(Intent.ACTION_MAIN);
            // add HOME category to it
            intent.addCategory(Intent.CATEGORY_HOME);
            // launch intent
            context.startActivity(intent);

            // disable dummyActivity once again
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }
}
