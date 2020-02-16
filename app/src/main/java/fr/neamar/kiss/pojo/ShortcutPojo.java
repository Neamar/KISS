package fr.neamar.kiss.pojo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import fr.neamar.kiss.db.DBHelper;

public final class ShortcutPojo extends PojoWithTags {

    public static final String SCHEME = "shortcut://";
    public static final String OREO_PREFIX = "oreo-shortcut/";

    private final int dbId;
    public final String packageName;
    public final String intentUri;// TODO: 15/10/18 Use boolean instead of prefix for Oreo shortcuts

    public ShortcutPojo(String id, int dbId, String packageName, String intentUri) {
        super(id);

        this.dbId = dbId;
        this.packageName = packageName;
        this.intentUri = intentUri;
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

    public Bitmap getIcon(Context context) {
        byte[] iconBlob = DBHelper.getShortcutIcon(context, this.dbId);

        if(iconBlob == null) {
            return null;
        }

        return BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length);
    }
}
