package fr.neamar.kiss.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.utils.ShortcutUtil;

public class LoadShortcutsPojos extends LoadPojos<ShortcutsPojo> {

    private final TagsHandler tagsHandler;

    public LoadShortcutsPojos(Context context) {
        super(context, ShortcutsPojo.SCHEME);
        tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected ArrayList<ShortcutsPojo> doInBackground(Void... arg0) {
        ArrayList<ShortcutsPojo> pojos = new ArrayList<>();

        if(context.get() == null) {
            return pojos;
        }
        List<ShortcutRecord> records = DBHelper.getShortcuts(context.get());
        for (ShortcutRecord shortcutRecord : records) {
            FutureTask<Drawable> futureIcon = new FutureTask<>(new IconCallable(context, shortcutRecord.icon_blob));
            String id = ShortcutUtil.generateShortcutId(shortcutRecord.name);

            IMAGE_EXCECUTOR.execute(futureIcon);

            ShortcutsPojo pojo = new ShortcutsPojo(id, shortcutRecord.packageName,
                    shortcutRecord.iconResource, shortcutRecord.intentUri,
                    futureIcon);

            pojo.setName(shortcutRecord.name);
            pojo.setTags(tagsHandler.getTags(pojo.id));

            pojos.add(pojo);
        }

        return pojos;
    }

    private static final class IconCallable implements Callable<Drawable> {
        private final WeakReference<Context> context;
        private final byte[] icon_blob;

        private IconCallable(WeakReference<Context> context, byte[] icon_blob) {
            this.context = context;
            this.icon_blob = icon_blob;
        }


        @Override
        public Drawable call() {
            final Context context = this.context.get();
            if (context == null) {
                return null;
            }

            Bitmap icon = BitmapFactory.decodeByteArray(icon_blob, 0, icon_blob.length);

            return new BitmapDrawable(context.getResources(), icon);
        }
    }

}
