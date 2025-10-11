package fr.neamar.kiss.searcher;

import android.text.TextUtils;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.PojoWithTags;

/**
 * Returns a list of all results that match the specified tag
 */
public class TagsSearcher extends PojoWithTagSearcher {
    public TagsSearcher(MainActivity activity, String query) {
        super(activity, query == null ? "<tags>" : query);
    }

    @Override
    protected boolean acceptPojo(PojoWithTags pojoWithTags) {
        return !TextUtils.isEmpty(pojoWithTags.getTags()) && pojoWithTags.getTags().contains(query);
    }

}
