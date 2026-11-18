package fr.neamar.kiss.dataprovider;

public enum ProviderName {
    APPS(true, "app"),
    SHORTCUTS(true, "shortcuts"),
    CALCULATOR(false, "calculator"),
    SETTINGS(false, "settings");

    final boolean isService;
    final String settingName;

    ProviderName(boolean isService, String settingName) {
        this.isService = isService;
        this.settingName = settingName;
    }

    public boolean isService() {
        return isService;
    }

    public String getSettingName() {
        return settingName;
    }

}
