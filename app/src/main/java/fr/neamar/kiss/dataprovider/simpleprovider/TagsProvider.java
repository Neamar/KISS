package fr.neamar.kiss.dataprovider.simpleprovider;

import java.util.Locale;

import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.searcher.Searcher;

public class TagsProvider extends SimpleProvider {
    public static final String SCHEME = "kisstag://";

    public static String generateUniqueId(String tag) {
        return SCHEME + tag.toLowerCase(Locale.ROOT);
    }

    @Override
    public void requestResults(String s, Searcher searcher) {

    }

    @Override
    public boolean mayFindById(String id) {
        return id.startsWith(SCHEME);
    }

    @Override
    public Pojo findById(String id) {
        return new TagDummyPojo(id);
    }
}
