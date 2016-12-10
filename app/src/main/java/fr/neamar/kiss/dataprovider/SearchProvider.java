package fr.neamar.kiss.dataprovider;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.URLUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadSearchPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchProvider extends Provider<SearchPojo> {
    private SharedPreferences prefs;
    public static final String URL_REGEX = "^(?:[a-z]+://)?(?:[a-z0-9-]|[^\\x00-\\x7F])+(?:[.](?:[a-z0-9-]|[^\\x00-\\x7F])+)+.*$";

    private static final Map<String, String> searchProviderUrls = new LinkedHashMap<>();
    private static final Pattern p = Pattern.compile(URL_REGEX);

    static {
        searchProviderUrls.put("Bing", "https://www.bing.com/search?q=");
        searchProviderUrls.put("DuckDuckGo", "https://duckduckgo.com/?q=");
        searchProviderUrls.put("Google", "https://encrypted.google.com/search?q=");
        searchProviderUrls.put("Qwant", "https://www.qwant.com/?q=");
        searchProviderUrls.put("StartPage", "https://startpage.com/do/search?language=cat=web&query=");
        searchProviderUrls.put("Wikipedia", "https://en.wikipedia.org/wiki/");
        searchProviderUrls.put("Yahoo", "https://search.yahoo.com/search?p=");
    }

    @Override
    public void reload() {
        this.initialize(new LoadSearchPojos(this));
    }

    public ArrayList<Pojo> getResults(String query) {

        ArrayList<Pojo> pojos = new ArrayList<>();
        Set<String> selectedProviders = new TreeSet<>();
        selectedProviders.addAll(PreferenceManager.getDefaultSharedPreferences(this).getStringSet("search-providers", new HashSet<>(Arrays.asList("Google"))));
        for (String searchProvider : selectedProviders) {
            SearchPojo pojo = new SearchPojo();
            pojo.query = query;
            pojo.relevance = 10;
            pojo.url = searchProviderUrls.get(searchProvider);
            pojo.name = searchProvider;
            pojos.add(pojo);
        }

        Matcher m = p.matcher(query);
        if (m.find()) {
            String guessedUrl = URLUtil.guessUrl(query);
            if (URLUtil.isValidUrl(guessedUrl)) {
                SearchPojo pojo = new SearchPojo();
                pojo.query = "";
                pojo.relevance = 50;
                pojo.name = guessedUrl;
                pojo.url = guessedUrl;
                pojo.direct = true;
                pojos.add(pojo);
            }
        }
        return pojos;
    }

    public static String[] getSearchProviders() {
        return searchProviderUrls.keySet().toArray(new String[searchProviderUrls.size()]);
    }
}
