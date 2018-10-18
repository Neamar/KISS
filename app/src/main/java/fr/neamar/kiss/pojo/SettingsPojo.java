package fr.neamar.kiss.pojo;

public class SettingsPojo extends Pojo {
    public String settingName;
    public String packageName = "";
    public int icon = -1;

    public SettingsPojo(String settingName, int icon) {
        this.settingName = settingName;
        this.packageName = "";
        this.icon = icon;
    }

    public SettingsPojo(String settingName, String packageName, int icon) {
        this.settingName = settingName;
        this.packageName = packageName;
        this.icon = icon;
    }
}
