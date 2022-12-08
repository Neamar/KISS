package fr.neamar.kiss.pojo;

import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.utils.ShortcutUtil;

public final class ShortcutPojo extends PojoWithTags {

    public static final String SCHEME = "shortcut://";
    public static final String OREO_PREFIX = "oreo-shortcut/";

    public final String packageName;
    public final String intentUri;// TODO: 15/10/18 Use boolean instead of prefix for Oreo shortcuts
    private final String componentName; // related component including user, for check of excluded apps
    private final boolean pinned; // pinned shortcut
    private final boolean dynamic; // dynamic shortcut

    public ShortcutPojo(ShortcutRecord shortcutRecord, String componentName, boolean pinned, boolean dynamic) {
        super(ShortcutUtil.generateShortcutId(shortcutRecord));
        this.packageName = shortcutRecord.packageName;
        this.intentUri = shortcutRecord.intentUri;
        this.componentName = componentName;
        this.pinned = pinned;
        this.dynamic = dynamic;
    }

    /**
     * Oreo shortcuts do not have a real intentUri, instead they have a shortcut id
     * and the Android system is responsible for safekeeping the Intent
     */
    public boolean isOreoShortcut() {
        return intentUri.contains(ShortcutPojo.OREO_PREFIX);
    }

    public String getOreoId() {
        // Oreo shortcuts encode their id in the unused intentUri field
        return intentUri.replace(ShortcutPojo.OREO_PREFIX, "");
    }

    public String getComponentName() {
        return componentName;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isDynamic() {
        return dynamic;
    }
}
