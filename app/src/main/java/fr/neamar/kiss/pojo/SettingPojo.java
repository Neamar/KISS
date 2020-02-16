package fr.neamar.kiss.pojo;

import androidx.annotation.DrawableRes;

public final class SettingPojo extends Pojo {
    public final String settingName;
    public final String packageName;
    public final @DrawableRes int icon;

    public SettingPojo(String id, String settingName, @DrawableRes int icon) {
    	super(id);

        this.settingName = settingName;
        this.packageName = "";
        this.icon = icon;
    }

    public SettingPojo(String id, String settingName, String packageName, @DrawableRes int icon) {
	    super(id);

        this.settingName = settingName;
        this.packageName = packageName;
        this.icon = icon;
    }
}
