package fr.neamar.kiss.loader;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.utils.ShortcutUtil;

public class LoadShortcutsPojos extends LoadPojos<ShortcutPojo> {

    private final TagsHandler tagsHandler;

    public LoadShortcutsPojos(Context context) {
        super(context, ShortcutPojo.SCHEME);
        tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected ArrayList<ShortcutPojo> doInBackground(Void... arg0) {
        if(context.get() == null) {
            return new ArrayList<>();
        }

        List<ShortcutRecord> records = DBHelper.getShortcuts(context.get());
        ArrayList<ShortcutPojo> pojos = new ArrayList<>(records.size());

        for (ShortcutRecord shortcutRecord : records) {
            String id = ShortcutUtil.generateShortcutId(shortcutRecord.name);

            ShortcutPojo pojo = new ShortcutPojo(id, shortcutRecord.dbId, shortcutRecord.packageName, shortcutRecord.intentUri);

            pojo.setName(shortcutRecord.name);
            pojo.setTags(tagsHandler.getTags(pojo.id));

            pojos.add(pojo);
        }

        return pojos;
    }

}
