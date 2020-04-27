package fr.neamar.kiss.db;

public class AppRecord {
    public static final int FLAG_CUSTOM_NAME = 2;
    public static final int FLAG_CUSTOM_ICON = 4;

    public long dbId;

    public String name;

    public String componentName;

    public int flags;

    public boolean hasCustomName() {
        return (flags & FLAG_CUSTOM_NAME) == FLAG_CUSTOM_NAME;
    }

    public boolean hasCustomIcon() {
        return (flags & FLAG_CUSTOM_ICON) == FLAG_CUSTOM_ICON;
    }
}
