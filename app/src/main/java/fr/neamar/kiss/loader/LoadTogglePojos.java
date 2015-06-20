package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.ArrayList;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.TogglePojo;

public class LoadTogglePojos extends LoadPojos<TogglePojo> {

    public LoadTogglePojos(Context context) {
        super(context, "app://");
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
        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            toggles.add(createPojo(context.getString(R.string.toggle_data), "data", R.drawable.toggle_data));
        }
        return toggles;
    }

    private TogglePojo createPojo(String name, String settingName, int resId) {
        TogglePojo pojo = new TogglePojo();
        pojo.id = pojoScheme + name.toLowerCase();
        pojo.name = context.getString(R.string.toggles_prefix) + name;
        pojo.nameLowerCased = pojo.name.toLowerCase();
        pojo.settingName = settingName;
        pojo.icon = resId;

        return pojo;
    }
}
