package fr.neamar.kiss.pojo;

import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.UserHandle;

public final class ShortcutPojo extends PojoWithTags {

    public static final String SCHEME = "shortcut://";
    public static final String OREO_PREFIX = "oreo-shortcut/";

    public final String packageName;
    public final String intentUri;// TODO: 15/10/18 Use boolean instead of prefix for Oreo shortcuts
    private final String appId; // related app, for check of excluded apps
    private final boolean pinned; // pinned shortcut
    private final boolean dynamic; // dynamic shortcut
    private final boolean disabled;
    private final UserHandle userHandle;
    public final long lastChanged; // last change of oreo shortcut, 0 otherwise

    public ShortcutPojo(UserHandle userHandle, ShortcutRecord shortcutRecord, String appId, boolean pinned, boolean dynamic, boolean disabled, long lastChanged) {
        super(ShortcutUtil.generateShortcutId(userHandle, shortcutRecord));
        this.packageName = shortcutRecord.packageName;
        this.intentUri = shortcutRecord.intentUri;
        this.appId = appId;
        this.pinned = pinned;
        this.dynamic = dynamic;
        this.disabled = disabled;
        this.userHandle = userHandle;
        this.lastChanged = lastChanged;
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

    public String getAppId() {
        return appId;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public UserHandle getUserHandle() {
        return userHandle;
    }
}
