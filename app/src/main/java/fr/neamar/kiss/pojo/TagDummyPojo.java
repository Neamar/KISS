package fr.neamar.kiss.pojo;

import fr.neamar.kiss.dataprovider.simpleprovider.TagsProvider;

public final class TagDummyPojo extends Pojo {
    public TagDummyPojo(String id) {
        super(id);
        setName(id.substring(TagsProvider.SCHEME.length()), false);
    }

    @Override
    public String getHistoryId() {
        // Should not appear in history
        return "";
    }

    @Override
    public String getFavoriteId() {
        // TagDummy are special, as they should appear in favorites but not in history
        return this.id;
    }
}
