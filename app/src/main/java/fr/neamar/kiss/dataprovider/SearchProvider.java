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
import fr.neamar.kiss.searcher.Searcher;

public class SearchProvider extends Provider<SearchPojo> {
    private static final String URL_REGEX = "^(?:[a-z]+://)?(?:[a-z0-9-]|[^\\x00-\\x7F])+(?:[.](?:[a-z0-9-]|[^\\x00-\\x7F])+)+.*$";

    public static final Pattern urlPattern = Pattern.compile(URL_REGEX);

    public static Set<String> getSearchProviders(Context context) {
        String[] defaultSearchProviders = context.getResources().getStringArray(R.array.defaultSearchProviders);
        return new HashSet<>(Arrays.asList(defaultSearchProviders));
    }

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadSearchPojos(this));
    }

    @Override
    public void requestResults(String s, Searcher searcher) {
        searcher.addResult(getResults(s).toArray(new Pojo[0]));
    }

    private ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> records = new ArrayList<>();
        for (SearchPojo pojo : pojos) {
            // Set the id, otherwise the result will be boosted since KISS will assume "we've selected this search provider multiple times before"
            pojo.id = "search://" + query;
            pojo.query = query;
            records.add(pojo);
        }

        // Open URLs directly (if I type http://something.com for instance)
        Matcher m = urlPattern.matcher(query);
        if (m.find()) {
            String guessedUrl = URLUtil.guessUrl(query);
            // URLUtil returns an http URL... we'll upgrade it to HTTPS
            // to avoid security issues on open networks,
            // technological problems when using HSTS
            // and do one less redirection to https
            // (tradeoff: non https URL will break, but they shouldn't exist anymore)
            guessedUrl = guessedUrl.replace("http://", "https://");
            if (URLUtil.isValidUrl(guessedUrl)) {
                SearchPojo pojo = new SearchPojo("", guessedUrl, SearchPojo.URL_QUERY);
                pojo.relevance = 50;
                pojo.setName(guessedUrl, false);
                records.add(pojo);
            }
        }
        return records;
    }
}
