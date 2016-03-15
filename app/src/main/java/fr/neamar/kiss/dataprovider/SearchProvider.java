package fr.neamar.kiss.dataprovider;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import fr.neamar.kiss.loader.LoadSearchPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchProvider extends Provider<SearchPojo> {
    private SharedPreferences prefs;
    private static final LinkedHashMap<String,String> searchProviderUrls = new LinkedHashMap<>();

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
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            Set<String> selectedProviders = PreferenceManager.getDefaultSharedPreferences(this).getStringSet("search-providers", new HashSet<String>(Arrays.asList("Google")));
            for (String searchProvider : selectedProviders) {
                SearchPojo pojo = new SearchPojo();
                pojo.query = query;
                pojo.relevance = 10;
                pojo.url = searchProviderUrls.get(searchProvider);
                pojo.name = searchProvider;
                pojos.add(pojo);
            }
        }
        else {

            SearchPojo pojo = new SearchPojo();
            pojo.query = query;
            pojo.relevance = 10;
            pojo.name="Google";
            pojo.url = searchProviderUrls.get("Google");
            pojos.add(pojo);
        }
        return pojos;
    }

    public static String[] getSearchProviders() {
        return searchProviderUrls.keySet().toArray(new String[searchProviderUrls.size()]);
    }
}
