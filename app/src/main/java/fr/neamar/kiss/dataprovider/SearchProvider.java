package fr.neamar.kiss.dataprovider;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.URLUtil;

import java.net.MalformedURLException;
import java.net.URL;
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
    public static final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";

    private static final Map<String,String> searchProviderUrls = new LinkedHashMap<>();

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
        }
        else {

            SearchPojo pojo = new SearchPojo();
            pojo.query = query;
            pojo.relevance = 10;
            pojo.name="Google";
            pojo.url = searchProviderUrls.get("Google");
            pojos.add(pojo);
        }

        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(query);//replace with string to compare
        try {
            if(m.find()) {
                String url = new URL ("https://" + query).toString();
                if(URLUtil.isValidUrl (url)) {
                    SearchPojo pojo = new SearchPojo();
                    pojo.query = "";
                    pojo.relevance = 50;
                    pojo.name = query;
                    pojo.url = url;
                    pojo.direct = true;
                    pojos.add(pojo);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return pojos;
    }

    public static String[] getSearchProviders() {
        return searchProviderUrls.keySet().toArray(new String[searchProviderUrls.size()]);
    }
}
