package fr.neamar.kiss.dataprovider;

import android.content.Context;
import android.webkit.URLUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.R;
import fr.neamar.kiss.loader.LoadSearchPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchProvider extends Provider<SearchPojo> {
    public static final String URL_REGEX = "^(?:[a-z]+://)?(?:[a-z0-9-]|[^\\x00-\\x7F])+(?:[.](?:[a-z0-9-]|[^\\x00-\\x7F])+)+.*$";

    public static final Pattern urlPattern = Pattern.compile(URL_REGEX);

    @Override
    public void reload() {
        this.initialize(new LoadSearchPojos(this));
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> records = new ArrayList<>();

        for (SearchPojo pojo : pojos) {
            // Set the id, otherwise the result will be boosted since KISS will assume "we've selected this search provider multiple times before"
            pojo.id = "search://" + query;
            pojo.query = query;
            records.add(pojo);
        }

        // Open URLs directly
        Matcher m = urlPattern.matcher(query);
        if (m.find()) {
            String guessedUrl = URLUtil.guessUrl(query);
            if (URLUtil.isValidUrl(guessedUrl)) {
                SearchPojo pojo = new SearchPojo();
                pojo.query = "";
                pojo.relevance = 50;
                pojo.name = guessedUrl;
                pojo.url = guessedUrl;
                pojo.direct = true;
                records.add(pojo);
            }
        }
        return records;
    }

    public static Set<String> getSearchProviders(Context context) {
        String[] defaultSearchProviders = context.getResources().getStringArray(R.array.defaultSearchProviders);
        return new HashSet<>(Arrays.asList(defaultSearchProviders));
    }
}
