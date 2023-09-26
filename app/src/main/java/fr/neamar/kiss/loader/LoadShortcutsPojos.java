
package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.utils.ShortcutUtil;

public class LoadShortcutsPojos extends LoadPojos<ShortcutPojo> {

    public LoadShortcutsPojos(Context context) {
        super(context, ShortcutPojo.SCHEME);
    }

    @Override
    protected List<ShortcutPojo> doInBackground(Void... params) {
        Context context = this.context.get();
        if (context == null) {
            return new ArrayList<>();
        }

        List<ShortcutRecord> records = DBHelper.getShortcuts(context);
        DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
        TagsHandler tagsHandler = dataHandler.getTagsHandler();
        Set<String> excludedApps = dataHandler.getExcluded();
        Set<String> excludedShortcutApps = dataHandler.getExcludedShortcutApps();

        List<ShortcutPojo> pojos = new ArrayList<>();

        for (ShortcutRecord shortcutRecord : records) {
            ShortcutPojo pojo = createPojo(shortcutRecord, tagsHandler, null, true, false);
            if (!pojo.isOreoShortcut()) {
                // add older shortcuts from DB
                pojos.add(pojo);
            }
        }

        // get all oreo shortcuts from system directly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<ShortcutInfo> shortcutInfos = ShortcutUtil.getAllShortcuts(context);
            for (ShortcutInfo shortcutInfo : shortcutInfos) {
                if (isCancelled()) {
                    break;
                }
                if (ShortcutUtil.isShortcutVisible(context, shortcutInfo, excludedApps, excludedShortcutApps)) {
                    ShortcutRecord shortcutRecord = ShortcutUtil.createShortcutRecord(context, shortcutInfo, !shortcutInfo.isPinned());
                    if (shortcutRecord != null) {
                        ShortcutPojo pojo = createPojo(shortcutRecord, tagsHandler, ShortcutUtil.getComponentName(context, shortcutInfo), shortcutInfo.isPinned(), shortcutInfo.isDynamic());
                        pojos.add(pojo);
                    }
                }
            }
        }

        return pojos;
    }

    private ShortcutPojo createPojo(ShortcutRecord shortcutRecord, TagsHandler tagsHandler, String componentName, boolean pinned, boolean dynamic) {
        ShortcutPojo pojo = new ShortcutPojo(shortcutRecord, componentName, pinned, dynamic);
        pojo.setName(shortcutRecord.name);
        pojo.setTags(tagsHandler.getTags(pojo.id));
        return pojo;
    }
}
