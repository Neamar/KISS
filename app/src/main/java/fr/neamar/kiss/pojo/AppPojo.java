package fr.neamar.kiss.pojo;
import fr.neamar.kiss.utils.UserHandle;

import android.util.Pair;

import fr.neamar.kiss.normalizer.StringNormalizer;

public class AppPojo extends Pojo {
    public String packageName;
    public String activityName;
    public UserHandle userHandle;

    // Tags normalized
    public String tagsNormalized;
    // Array that contains the non-normalized positions for every normalized
    // character entry
    private int[] tagsPositionMap = null;

    public void setTags(String tags) {
    // Set the actual user-friendly name
        this.tags = tags;

        if (this.tags != null) {
            this.tags = this.tags.replaceAll("<", "&lt;");
            // Normalize name for faster searching
            Pair<String, int[]> normalized = StringNormalizer.normalizeWithMap(this.tags);
            this.tagsNormalized = normalized.first;
            this.tagsPositionMap = normalized.second;
        }
    }

    public void setTagHighlight(int positionStart, int positionEnd) {
        int posStart = this.mapTagsPosition(positionStart);
        int posEnd = this.mapTagsPosition(positionEnd);

        this.displayTags = this.tags.substring(0, posStart)
                + '{' + this.tags.substring(posStart, posEnd) + '}' + this.tags.substring(posEnd, this.tags.length());
    }

    /**
     * Map a position in the normalized name to a position in the standard name string
     *
     * @param position Position in normalized name
     * @return Position in non-normalized string
     */
    public int mapTagsPosition(int position) {
        if (this.tagsPositionMap != null) {
            if (position < this.tagsPositionMap.length) {
                return this.tagsPositionMap[position];
            }
            return this.tags.length();
        } else {
            // No mapping defined
            if (position < this.tags.length()) {
                return position;
            }
            return this.tags.length();
        }
    }

}
