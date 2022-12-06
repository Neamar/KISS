package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

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
    protected ArrayList<ShortcutPojo> doInBackground(Void... arg0) {
        Context context = this.context.get();
        if (context == null) {
            return new ArrayList<>();
        }

        TagsHandler tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
        List<ShortcutRecord> records = DBHelper.getShortcuts(context);
        ArrayList<ShortcutPojo> pojos = new ArrayList<>(records.size());

        for (ShortcutRecord shortcutRecord : records) {
            String id = ShortcutUtil.generateShortcutId(shortcutRecord.name);

            ShortcutPojo pojo = new ShortcutPojo(id, shortcutRecord.packageName, shortcutRecord.intentUri);

            pojo.setName(shortcutRecord.name);
            pojo.setTags(tagsHandler.getTags(pojo.id));

            if (isExistingShortcut(pojo)) {
                pojos.add(pojo);
            }
        }

        List<ShortcutPojo> filteredPojos = filterOnEnabledApps(pojos, context)
        return filteredPojos;
    }

    private boolean isExistingShortcut(ShortcutPojo pojo) {
        if (pojo.isOreoShortcut()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ShortcutInfo shortcutInfo = ShortcutUtil.getShortCut(context.get(), pojo.packageName, pojo.getOreoId());
                return shortcutInfo != null;
            }
        }
        return true;
    }

    /**
     * @return a new list which only contains shortcuts which come from apps which the
     * user has enabled shortcuts for.
     * Does not modify the input list.
     */
    private List<ShortcutPojo> filterOnEnabledApps(List<ShortcutPojo> allPojos, Context context) {
        List<ShortcutPojo> result = new ArrayList<>();

        Set<String> enabledApps = KissApplication.getApplication(ctx).getDataHandler().getEnabledShortcutApps();

        if (enabledApps.size == 0) {
            return result;
        }

        for (ShortcutPojo pojo : allPojos) {
            if (enabledApps.contains(pojo.packageName)) {
                result.add(pojo)
            }
        }
        return result
    }
}
