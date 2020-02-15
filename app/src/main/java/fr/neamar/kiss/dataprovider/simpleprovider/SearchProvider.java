package fr.neamar.kiss.dataprovider.simpleprovider;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.URLUtil;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;

public class SearchProvider extends SimpleProvider {
    private static final String URL_REGEX = "^(?:[a-z]+://)?(?:[a-z0-9-]|[^\\x00-\\x7F])+(?:[.](?:[a-z0-9-]|[^\\x00-\\x7F])+)+.*$";
    public static final Pattern urlPattern = Pattern.compile(URL_REGEX);
    private final SharedPreferences prefs;

    public static Set<String> getDefaultSearchProviders(Context context) {
        String[] defaultSearchProviders = context.getResources().getStringArray(R.array.defaultSearchProviders);
        return new HashSet<>(Arrays.asList(defaultSearchProviders));
    }

    private final ArrayList<SearchPojo> searchProviders = new ArrayList<>();
    private final Context context;

    public SearchProvider(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        reload();
    }

    @Override
    public void reload() {
        searchProviders.clear();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> selectedProviders = prefs.getStringSet("selected-search-provider-names", new HashSet<>(Collections.singletonList("Google")));
        Set<String> availableProviders = prefs.getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(context));

        // Get default search engine
        String defaultSearchEngine = prefs.getString("default-search-provider", "Google");

        assert selectedProviders != null;
        assert availableProviders != null;
        assert defaultSearchEngine != null;
        for (String searchProvider : selectedProviders) {
            String url = getProviderUrl(availableProviders, searchProvider);
            SearchPojo pojo = new SearchPojo("", url, SearchPojo.SEARCH_QUERY);
            // Super low relevance, should never be displayed before anything
            pojo.relevance = -500;
            if (defaultSearchEngine.equals(searchProvider))
                // Display default search engine slightly higher
                pojo.relevance += 1;

            pojo.setName(searchProvider, false);
            if (pojo.url != null) {
                searchProviders.add(pojo);
            }
        }
    }

    @Override
    public void requestResults(String s, Searcher searcher) {
        searcher.addResult(getResults(s).toArray(new Pojo[0]));
    }

    private ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> records = new ArrayList<>();

        if (prefs.getBoolean("enable-search", true)) {
            for (SearchPojo pojo : searchProviders) {
                pojo.query = query;
                records.add(pojo);
            }
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
                SearchPojo pojo = new SearchPojo("search://url-access","", guessedUrl, SearchPojo.URL_QUERY);
                pojo.relevance = 50;
                pojo.setName(guessedUrl, false);
                records.add(pojo);
            }
        }
        return records;
    }

    @Nullable
    @SuppressWarnings("StringSplitter")
    // Find the URL associated with specified providerName
    private String getProviderUrl(Set<String> searchProviders, String searchProviderName) {
        for (String nameAndUrl : searchProviders) {
            if (nameAndUrl.contains(searchProviderName + "|")) {
                String[] arrayNameAndUrl = nameAndUrl.split("\\|");
                // sanity check
                if (arrayNameAndUrl.length == 2) {
                    return arrayNameAndUrl[1];
                }
            }
        }
        return null;
    }
}
