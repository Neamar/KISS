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

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.URIUtils;
import fr.neamar.kiss.utils.URLUtils;

public class SearchProvider extends SimpleProvider {
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

        if (URLUtils.matchesUrlPattern(query) && URLUtil.isValidUrl(query)) {
            // Open valid URLs directly (if I type http://something.com for instance)
            SearchPojo pojo = createUrlQuerySearchPojo(query);
            records.add(pojo);
        } else if (URIUtils.isValidUri(query, context).isValid) {
            // Open uri directly by an app that can handle it (if i type gemini://oppen.digital/ariane/ for gemini browser)
            // https://github.com/Neamar/KISS/issues/1786
            SearchPojo pojo = new SearchPojo("search://uri-access", query, "", SearchPojo.URI_QUERY);
            pojo.relevance = -100;
            pojo.setName(query, false);
            records.add(pojo);
        } else {
            // search for url pattern
            if (URLUtils.matchesUrlPattern(query)) {
                // guess url (if I type something.com for instance)
                String guessedUrl = URLUtil.guessUrl(query);
                if (URLUtil.isValidUrl(guessedUrl)) {
                    SearchPojo pojo = createUrlQuerySearchPojo(guessedUrl);
                    records.add(pojo);
                }
            }
        }

        return records;
    }

    /**
     * create SearchPojo with type {@link SearchPojo#URI_QUERY} for direct access to url
     *
     * @param url
     * @return the search pojo
     */
    private SearchPojo createUrlQuerySearchPojo(String url) {
        // URLUtil returns an http URL... we'll upgrade it to HTTPS
        // to avoid security issues on open networks,
        // technological problems when using HSTS
        // and do one less redirection to https
        // (tradeoff: non https URL will break, but they shouldn't exist anymore)
        url = url.replace("http://", "https://");

        SearchPojo pojo = new SearchPojo("search://url-access", "", url, SearchPojo.URL_QUERY);
        pojo.relevance = 50;
        pojo.setName(url, false);
        return pojo;
    }

    @Nullable
    @SuppressWarnings("StringSplitter")
    // Find the URL associated with specified providerName
    private static String getProviderUrl(Set<String> searchProviders, String searchProviderName) {
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

    @Nullable
    public static SearchPojo getDefaultSearch(final String query, final Context context, @Nullable SharedPreferences pref) {
        pref = pref != null ? pref : PreferenceManager.getDefaultSharedPreferences(context);
        String defaultSearchEngine = pref.getString("default-search-provider", "Google");
        Set<String> availableProviders = pref.getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(context));
        String url = getProviderUrl(availableProviders, defaultSearchEngine);
        return url != null ? new SearchPojo(query, url, SearchPojo.SEARCH_QUERY) : null;
    }
}
