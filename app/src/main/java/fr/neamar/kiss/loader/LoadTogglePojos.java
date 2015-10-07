package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.TogglePojo;

public class LoadTogglePojos extends LoadPojos<TogglePojo> {

    public LoadTogglePojos(Context context) {
        super(context, "toggle://");
    }

    @Override
    protected ArrayList<TogglePojo> doInBackground(Void... params) {
        ArrayList<TogglePojo> toggles = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            toggles.add(createPojo(context.getString(R.string.toggle_wifi), "wifi", R.drawable.toggle_wifi));
        }
        if (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            toggles.add(createPojo(context.getString(R.string.toggle_bluetooth), "bluetooth", R.drawable.toggle_bluetooth));
        }
        toggles.add(createPojo(context.getString(R.string.toggle_silent), "silent", R.drawable.toggle_silent));
        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            // Not working after 4.4
            // See http://stackoverflow.com/questions/26539445/the-setmobiledataenabled-method-is-no-longer-callable-as-of-android-l-and-later
            toggles.add(createPojo(context.getString(R.string.toggle_data), "data", R.drawable.toggle_data));
        }
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && KissApplication.getCameraHandler().isTorchAvailable()) {
            toggles.add(createPojo(context.getString(R.string.toggle_torch), "torch", R.drawable.toggle_torch));
        }

        //toggle for synchronization
        toggles.add(createPojo(context.getString(R.string.toggle_sync), "sync", android.R.drawable.ic_popup_sync));

        return toggles;
    }

    private TogglePojo createPojo(String name, String settingName, int resId) {
        TogglePojo pojo = new TogglePojo();
        pojo.id = pojoScheme + name.toLowerCase();
        pojo.name = name;
        pojo.nameNormalized = pojo.name.toLowerCase();
        pojo.settingName = settingName;
        pojo.icon = resId;

        return pojo;
    }
}
