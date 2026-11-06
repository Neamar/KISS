package fr.neamar.kiss.dataprovider;

public enum ProviderName {
    APPS(true, "app"),
    CONTACTS(true, "contacts"),
    SHORTCUTS(true, "shortcuts"),
    CALCULATOR(false, "calculator"),
    TIMER(false, "timer"),
    PHONE(false, "phone"),
    SEARCH(false, "search"),
    SETTINGS(false, "settings"),
    TAGS(false, "tags");

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
