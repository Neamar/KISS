package fr.neamar.kiss.loader;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.BitmapFactory;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.ShortcutPojo;

public class LoadShortcutPojos extends LoadPojos<ShortcutPojo> {

    public LoadShortcutPojos(Context context) {
        super(context, ShortcutPojo.SCHEME);
    }

    @Override
    protected ArrayList<ShortcutPojo> doInBackground(Void... arg0) {
        ArrayList<ShortcutRecord> records = DBHelper.getShortcuts(context);
        ArrayList<ShortcutPojo> pojos = new ArrayList<>();
        for (ShortcutRecord shortcutRecord : records) {
            ShortcutPojo pojo = createPojo(shortcutRecord.name);
            pojo.packageName = shortcutRecord.packageName;
            pojo.resourceName = shortcutRecord.iconResource;
            pojo.intentUri = shortcutRecord.intentUri;
            if (shortcutRecord.icon_blob != null)
                pojo.icon = BitmapFactory.decodeByteArray(shortcutRecord.icon_blob, 0, shortcutRecord.icon_blob.length);
            

            pojos.add(pojo);
        }

        return pojos;
    }
    
    public ShortcutPojo createPojo(String name) {
        ShortcutPojo pojo = new ShortcutPojo();

        pojo.id = ShortcutPojo.SCHEME + name.toLowerCase();
        pojo.setName(name);

        return pojo;
    }

}
