package fr.neamar.kiss.loader;

import android.content.Context;
import android.graphics.BitmapFactory;

import java.util.ArrayList;

import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.ShortcutsPojo;

public class LoadShortcutsPojos extends LoadPojos<ShortcutsPojo> {

    public LoadShortcutsPojos(Context context) {
        super(context, ShortcutsPojo.SCHEME);
    }

    @Override
    protected ArrayList<ShortcutsPojo> doInBackground(Void... arg0) {
        ArrayList<ShortcutRecord> records = DBHelper.getShortcuts(context);
        ArrayList<ShortcutsPojo> pojos = new ArrayList<>();
        for (ShortcutRecord shortcutRecord : records) {
            ShortcutsPojo pojo = createPojo(shortcutRecord.name);

            pojo.packageName = shortcutRecord.packageName;
            pojo.resourceName = shortcutRecord.iconResource;
            pojo.intentUri = shortcutRecord.intentUri;
            if (shortcutRecord.icon_blob != null) {
                pojo.icon = BitmapFactory.decodeByteArray(shortcutRecord.icon_blob, 0, shortcutRecord.icon_blob.length);
            }

            pojos.add(pojo);
        }

        return pojos;
    }

    public ShortcutsPojo createPojo(String name) {
        ShortcutsPojo pojo = new ShortcutsPojo();

        pojo.id = ShortcutsPojo.SCHEME + name.toLowerCase();
        pojo.setName(name);

        return pojo;
    }

}
