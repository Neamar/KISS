package fr.neamar.kiss.searcher;

import android.text.TextUtils;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.PojoWithTags;

/**
 * Returns a list of all results that has no tag
 */
public class UntaggedSearcher extends PojoWithTagSearcher {

    public UntaggedSearcher(MainActivity activity) {
        super(activity, "<untagged>");
    }

    @Override
    protected boolean acceptPojo(PojoWithTags pojoWithTags) {
        return TextUtils.isEmpty(pojoWithTags.getTags());
    }

}
