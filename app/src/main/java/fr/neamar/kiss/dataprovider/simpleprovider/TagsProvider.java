package fr.neamar.kiss.dataprovider.simpleprovider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.searcher.Searcher;

public class TagsProvider extends SimpleProvider<TagDummyPojo> {
    public static final String SCHEME = "kisstag://";

    private final Map<String, TagDummyPojo> pojos = new HashMap<>();

    public static String generateUniqueId(String tag) {
        return SCHEME + tag.toLowerCase(Locale.ROOT);
    }

    @Override
    public void requestResults(String s, Searcher searcher) {

    }

    @Override
    public void reload() {
        super.reload();
        pojos.clear();
    }

    @Override
    public boolean mayFindById(String id) {
        return id.startsWith(SCHEME);
    }

    @Override
    public TagDummyPojo findById(String id) {
        // keep instances of TagDummyPojo to improve behavior of tags in favorites
        return pojos.computeIfAbsent(id, TagDummyPojo::new);
    }
}
