package fr.neamar.kiss.pojo;

import android.graphics.drawable.Drawable;
import android.util.Pair;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import fr.neamar.kiss.normalizer.StringNormalizer;

public class PojoWithTags extends Pojo {
    // tags normalized, for faster search
    private StringNormalizer.Result normalizedTags = null;
    // Tags assigned to this pojo
    private String tags = "";

    public PojoWithTags(String id, Future<Drawable> icon) {
        super(id, icon);
    }

    public StringNormalizer.Result getNormalizedTags() {
        return normalizedTags;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        if (tags != null) {
            // Set the actual user-friendly name
            this.tags = tags;
            this.normalizedTags = StringNormalizer.normalizeWithResult(this.tags, false);
        } else {
            this.tags = null;
            this.normalizedTags = null;
        }
    }
}
