package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Locale;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.SettingsPojo;

public class LoadSettingsPojos extends LoadPojos<SettingsPojo> {

    public LoadSettingsPojos(Context context) {
        super(context, "setting://");
    }

    @Override
    protected ArrayList<SettingsPojo> doInBackground(Void... params) {
        PackageManager pm = context.getPackageManager();
        ArrayList<SettingsPojo> settings = new ArrayList<>();
        settings.add(createPojo(context.getString(R.string.settings_airplane),
                Settings.ACTION_AIRPLANE_MODE_SETTINGS, R.drawable.setting_airplane));
        settings.add(createPojo(context.getString(R.string.settings_device_info),
                Settings.ACTION_DEVICE_INFO_SETTINGS, R.drawable.setting_info));
        settings.add(createPojo(context.getString(R.string.settings_applications),
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, R.drawable.setting_apps));
        settings.add(createPojo(context.getString(R.string.settings_connectivity),
                Settings.ACTION_WIRELESS_SETTINGS, R.drawable.toggle_wifi));
        settings.add(createPojo(context.getString(R.string.settings_storage),
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS, R.drawable.setting_storage));
        settings.add(createPojo(context.getString(R.string.settings_accessibility),
                Settings.ACTION_ACCESSIBILITY_SETTINGS, R.drawable.setting_accessibility));
        settings.add(createPojo(context.getString(R.string.settings_battery),
                Intent.ACTION_POWER_USAGE_SUMMARY, R.drawable.setting_battery));
        settings.add(createPojo(context.getString(R.string.settings_tethering), "com.android.settings",
                "com.android.settings.TetherSettings", R.drawable.setting_tethering));
        settings.add(createPojo(context.getString(R.string.settings_sound),
                Settings.ACTION_SOUND_SETTINGS, R.drawable.setting_dev));
        settings.add(createPojo(context.getString(R.string.settings_display),
                Settings.ACTION_DISPLAY_SETTINGS, R.drawable.setting_dev));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                settings.add(createPojo(context.getString(R.string.settings_nfc),
                        Settings.ACTION_NFC_SETTINGS, R.drawable.setting_nfc));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.add(createPojo(context.getString(R.string.settings_dev),
                    Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, R.drawable.setting_dev));
        }
        return settings;
    }

    private SettingsPojo createPojo(String name, String packageName, String settingName, int resId) {
        SettingsPojo pojo = this.createPojo(name, settingName, resId);
        pojo.packageName = packageName;
        return pojo;
    }

    private SettingsPojo createPojo(String name, String settingName, int resId) {
        SettingsPojo pojo = new SettingsPojo();
        pojo.id = pojoScheme + settingName.toLowerCase(Locale.ENGLISH);
        pojo.name = name;
        pojo.nameNormalized = pojo.name.toLowerCase(Locale.ENGLISH);
        pojo.settingName = settingName;
        pojo.icon = resId;

        return pojo;
    }
}
