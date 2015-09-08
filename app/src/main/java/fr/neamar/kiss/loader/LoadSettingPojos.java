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
        settings.add(createPojo(context.getString(R.string.settings_airplane),
                android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS, R.drawable.setting_airplane));
        settings.add(createPojo(context.getString(R.string.settings_device_info),
                android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS, android.R.drawable.ic_menu_manage));
        settings.add(createPojo(context.getString(R.string.settings_applications),
                android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, android.R.drawable.sym_def_app_icon));
        settings.add(createPojo(context.getString(R.string.settings_connectivity),
                android.provider.Settings.ACTION_WIRELESS_SETTINGS, R.drawable.toggle_wifi));
        settings.add(createPojo(context.getString(R.string.settings_battery),
                Intent.ACTION_POWER_USAGE_SUMMARY, R.drawable.setting_battery));
        return settings;
    }

    private SettingPojo createPojo(String name, String settingName, int resId) {
        SettingPojo pojo = new SettingPojo();
        pojo.id = pojoScheme + settingName.toLowerCase(Locale.ENGLISH);
        pojo.name = name;
        pojo.nameNormalized = pojo.name.toLowerCase(Locale.ENGLISH);
        pojo.settingName = settingName;
        pojo.icon = resId;

        return pojo;
    }
}
