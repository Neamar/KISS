package fr.neamar.kiss.pojo;

import android.support.annotation.DrawableRes;

public class SettingsPojo extends Pojo {
    public final String settingName;
    public final String packageName;
    public final @DrawableRes int icon;

    public SettingsPojo(String settingName, @DrawableRes int icon) {
        this.settingName = settingName;
        this.packageName = "";
        this.icon = icon;
    }

    public SettingsPojo(String settingName, String packageName, @DrawableRes int icon) {
        this.settingName = settingName;
        this.packageName = packageName;
        this.icon = icon;
    }
}
