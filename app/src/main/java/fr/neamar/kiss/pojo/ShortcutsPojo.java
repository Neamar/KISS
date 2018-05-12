package fr.neamar.kiss.pojo;

import android.graphics.Bitmap;

public class ShortcutsPojo extends PojoWithTags {

    public static final String SCHEME = "shortcut://";
    public static final String OREO_PREFIX = "oreo-shortcut/";

    public String packageName;
    public String resourceName;
    public String intentUri;
    public Bitmap icon;

    public boolean isOreoShortcut() {
        // Oreo shortcuts do not have a real intentUri, instead they have a shortcut id
        // and the Android system is responsible for safekeeping the Intent
        return intentUri.contains(ShortcutsPojo.OREO_PREFIX);
    }

    public void setOreoId(String id) {
        intentUri = ShortcutsPojo.OREO_PREFIX + id;
    }

    public String getOreoId() {
        // Oreo shortcuts encode their id in the unused intentUri field
        return intentUri.replace(ShortcutsPojo.OREO_PREFIX, "");
    }
}
