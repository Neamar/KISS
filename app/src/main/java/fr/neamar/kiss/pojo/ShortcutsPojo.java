package fr.neamar.kiss.pojo;

import android.graphics.drawable.Drawable;

import java.util.concurrent.Future;

public class ShortcutsPojo extends PojoWithTags {

    public static final String SCHEME = "shortcut://";
    public static final String OREO_PREFIX = "oreo-shortcut/";

    public final String packageName;
    public final String resourceName;
    public final String intentUri;// TODO: 15/10/18 Use boolean instead of prefix for Oreo shortcuts

    public ShortcutsPojo(String id, String packageName, String resourceName, String intentUri,
                         Future<Drawable> icon) {
        super(id, icon);

        this.packageName = packageName;
        this.resourceName = resourceName;
        this.intentUri = intentUri;
    }

    /**
     * Oreo shortcuts do not have a real intentUri, instead they have a shortcut id
     * and the Android system is responsible for safekeeping the Intent
     */
    public ShortcutsPojo(String id, String packageName, String oreoId, Future<Drawable> icon) {
        super(id, icon);

        this.packageName = packageName;
        this.resourceName = null;
        this.intentUri = ShortcutsPojo.OREO_PREFIX + oreoId;
    }

    /**
     * Oreo shortcuts do not have a real intentUri, instead they have a shortcut id
     * and the Android system is responsible for safekeeping the Intent
     */
    public boolean isOreoShortcut() {
        return intentUri.contains(ShortcutsPojo.OREO_PREFIX);
    }

    public String getOreoId() {
        // Oreo shortcuts encode their id in the unused intentUri field
        return intentUri.replace(ShortcutsPojo.OREO_PREFIX, "");
    }
}
