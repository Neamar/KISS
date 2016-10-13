package fr.neamar.kiss;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.neamar.kiss.db.DBHelper;

/**
 * Created by nmitsou on 13.10.16.
 */

public class TagsHandler {
    Context context;
    //cached tags
    private Map<String, String> tagsCache;

    public TagsHandler(Context context) {
        this.context = context;
        tagsCache = DBHelper.loadTags(this.context);
    }

    public boolean setTags(String id, String tags) {
        //remove existing tags for id
        DBHelper.deleteTagsForId(this.context, id);
        if (!tags.isEmpty()) {
            //add to db
            DBHelper.insertTagsToId(this.context, tags, id);
            //add to cache
            tagsCache.put(id, tags);
            return true;
        }
        else {
            //remove from cache if empty tag
            tagsCache.remove(id);
            return false;
        }
    }

    public String getTags(String id) {
        String tag = tagsCache.get(id);
        if (tag == null) {
            return "";
        }
        return tag;
    }
}
