package fr.neamar.kiss.preference;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import fr.neamar.kiss.DummyActivity;

/**
 * A Dialog Preference that allows the User to change the default launcher
 */
public class DefaultLauncherPreference extends DialogPreference {

    public DefaultLauncherPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_POSITIVE) {

            // get context (in order to avoid multiple get() calls)
            Context context = getContext();

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
