package fr.neamar.kiss.pojo;

import fr.neamar.kiss.dataprovider.simpleprovider.TagsProvider;

public class TagDummyPojo extends Pojo {

    public TagDummyPojo(String id) {
        super(id);
        setName(id.substring(TagsProvider.SCHEME.length()), false);
    }
}
