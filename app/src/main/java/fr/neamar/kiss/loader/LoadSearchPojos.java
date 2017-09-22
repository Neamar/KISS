package fr.neamar.kiss.loader;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import fr.neamar.kiss.dataprovider.SearchProvider;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class LoadSearchPojos extends LoadPojos<SearchPojo> {

    public LoadSearchPojos(Context context) {
        super(context, "none://");
    }


    private String getProviderUrl(Set<String> searchProviders, String searchProviderName) {
        for (String nameAndUrl : searchProviders) {
            if (nameAndUrl.contains(searchProviderName+"|")) {
                return nameAndUrl.split("\\|")[1];
            }
        }
        return null;
    }

    @Override
    protected ArrayList<SearchPojo> doInBackground(Void... params) {
        ArrayList<SearchPojo> pojos = new ArrayList<>();
        Set<String> selectedProviders = PreferenceManager.getDefaultSharedPreferences(this.context).getStringSet("selected-search-provider-names", new HashSet<>(Arrays.asList("Google")));
        Set<String> availableProviders = PreferenceManager.getDefaultSharedPreferences(this.context).getStringSet("available-search-providers", SearchProvider.getSearchProviders(this.context));

        for (String searchProvider : selectedProviders) {
            SearchPojo pojo = new SearchPojo();
            pojo.relevance = 10;
            pojo.url = getProviderUrl(availableProviders, searchProvider);
            pojo.name = searchProvider;
            if (pojo.url != null) {
                pojos.add(pojo);
            }
        }
        return pojos;
    }
}
