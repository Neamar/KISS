package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadSearchPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchProvider extends Provider<SearchPojo> {
    public static final String SEARCH_SCHEME = "search://";

    public SearchProvider(Context context) {
        super(new LoadSearchPojos(context));
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> pojos = new ArrayList<>();
        pojos.add(getResult(query));

        return pojos;
    }

    public Pojo findById(String id) {
        return getResult(id.replaceFirst(Pattern.quote(SEARCH_SCHEME), ""));
    }

    public Pojo getResult(String query) {
        SearchPojo pojo = new SearchPojo();
        pojo.id = "search://" + query;
        pojo.query = query;
        pojo.relevance = 10;
        return pojo;
    }
}
