package fr.neamar.kiss.pojo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class ShortcutsPojo extends PojoWithTags {

    public static final String SCHEME = "shortcut://";
    public static final String OREO_PREFIX = "oreo-shortcut/";

    public final String packageName;
    public final String resourceName;
    public final String intentUri;// TODO: 15/10/18 Use boolean instead of prefix for Oreo shortcuts
    public final Bitmap bitmapIcon;
    public final Drawable oreoIcon;

    public ShortcutsPojo(String id, String packageName, String resourceName, String intentUri,
                         Bitmap icon) {
        super(id);

        this.packageName = packageName;
        this.resourceName = resourceName;
        this.intentUri = intentUri;
        this.bitmapIcon = icon;
        this.oreoIcon = null;
    }

    /**
     * Oreo shortcuts do not have a real intentUri, instead they have a shortcut id
     * and the Android system is responsible for safekeeping the Intent
     */
    public ShortcutsPojo(String id, String packageName, String oreoId, Drawable icon) {
        super(id);

        this.packageName = packageName;
        this.resourceName = null;
        this.intentUri = ShortcutsPojo.OREO_PREFIX + oreoId;
        this.oreoIcon = icon;
        this.bitmapIcon = null;
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
