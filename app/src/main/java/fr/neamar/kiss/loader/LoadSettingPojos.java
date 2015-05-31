package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Locale;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.SettingPojo;

public class LoadSettingPojos extends LoadPojos<SettingPojo> {

    public LoadSettingPojos(Context context) {
        super(context, "setting://");
    }

    @Override
    protected ArrayList<SettingPojo> doInBackground(Void... params) {
        ArrayList<SettingPojo> settings = new ArrayList<>();
        settings.add(createPojo("Airplane mode",
                android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS, R.attr.airplane));
        settings.add(createPojo("Device info",
                android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS));
        settings.add(createPojo("Applications",
                android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, R.attr.application));
        settings.add(createPojo("Connectivity",
                android.provider.Settings.ACTION_WIRELESS_SETTINGS, R.attr.wifi));
        settings.add(createPojo("Battery", Intent.ACTION_POWER_USAGE_SUMMARY, R.attr.battery));
        return settings;
    }

    private SettingPojo createPojo(String name, String settingName) {
        return createPojo(name, settingName, android.R.drawable.ic_menu_manage);
    }

    private SettingPojo createPojo(String name, String settingName, int resId) {
        SettingPojo pojo = new SettingPojo();
        pojo.id = pojoScheme + settingName.toLowerCase(Locale.ENGLISH);
        pojo.name = "Setting: " + name;
        pojo.nameLowerCased = pojo.name.toLowerCase(Locale.ENGLISH);
        pojo.settingName = settingName;
        pojo.icon = resId;

        return pojo;
    }
}
