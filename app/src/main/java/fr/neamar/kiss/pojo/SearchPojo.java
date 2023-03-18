package fr.neamar.kiss.pojo;

public final class SearchPojo extends Pojo {
    public String query;
    public final String url;
    public final SearchPojoType type;

    public SearchPojo(String query, String url, SearchPojoType type) {
        this(url, query, url, type);
    }

    public SearchPojo(String id, String query, String url, SearchPojoType type) {
        super(id);
        this.query = query;
        this.url = url;
        this.type = type;
    }

    @Override
    public String getHistoryId() {
        // Search POJO should not appear in history
        return "";
    }
}
