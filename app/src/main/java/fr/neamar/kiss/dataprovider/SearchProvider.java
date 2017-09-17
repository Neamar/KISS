package fr.neamar.kiss.dataprovider;

import android.preference.PreferenceManager;
import android.webkit.URLUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.loader.LoadSearchPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchProvider extends Provider<SearchPojo> {
    public static final String URL_REGEX = "^(?:[a-z]+://)?(?:[a-z0-9-]|[^\\x00-\\x7F])+(?:[.](?:[a-z0-9-]|[^\\x00-\\x7F])+)+.*$";

    private static final Set<String> defaultSearchProviders = new HashSet<>();
    public static final Pattern urlPattern = Pattern.compile(URL_REGEX);

    static {
        defaultSearchProviders.add("Bing|https://www.bing.com/search?q={q}");
        defaultSearchProviders.add("DuckDuckGo|https://duckduckgo.com/?q={q}");
        defaultSearchProviders.add("Google|https://encrypted.google.com/search?q={q}");
        defaultSearchProviders.add("Yahoo|https://search.yahoo.com/search?urlPattern={q}");
    }

    @Override
    public void reload() {
        this.initialize(new LoadSearchPojos(this));
    }

    private String getProviderUrl(Set<String> searchProviders, String searchProviderName) {
        for (String nameAndUrl : searchProviders) {
            if (nameAndUrl.contains(searchProviderName+"|")) {
                return nameAndUrl.split("\\|")[1];
            }
        }
        return null;
    }

    public ArrayList<Pojo> getResults(String query) {

        ArrayList<Pojo> pojos = new ArrayList<>();
        Set<String> selectedProviders = PreferenceManager.getDefaultSharedPreferences(this).getStringSet("selected-search-provider-names", new HashSet<>(Arrays.asList("Google")));
        Set<String> availableProviders = PreferenceManager.getDefaultSharedPreferences(this).getStringSet("available-search-providers", getSearchProviders());

        for (String searchProvider : selectedProviders) {
            SearchPojo pojo = new SearchPojo();
            pojo.query = query;
            pojo.relevance = 10;
            pojo.url = getProviderUrl(availableProviders, searchProvider);
            pojo.name = searchProvider;
            if (pojo.url != null) {
                pojos.add(pojo);
            }
        }

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
                pojos.add(pojo);
            }
        }
        return pojos;
    }

    public static Set<String> getSearchProviders() {
        return defaultSearchProviders;
    }
}
