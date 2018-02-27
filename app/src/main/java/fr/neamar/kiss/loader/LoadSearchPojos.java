package fr.neamar.kiss.loader;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.neamar.kiss.dataprovider.SearchProvider;
import fr.neamar.kiss.pojo.SearchPojo;

public class LoadSearchPojos extends LoadPojos<SearchPojo> {

    public LoadSearchPojos(Context context) {
        super(context, "none://");
    }


    private String getProviderUrl(Set<String> searchProviders, String searchProviderName) {
        for (String nameAndUrl : searchProviders) {
            if (nameAndUrl.contains(searchProviderName + "|")) {
                return nameAndUrl.split("\\|")[1];
            }
        }
        return null;
    }

    @Override
    protected ArrayList<SearchPojo> doInBackground(Void... params) {
        ArrayList<SearchPojo> pojos = new ArrayList<>();

        if(context.get() == null) {
            return pojos;
        }

        Set<String> selectedProviders = PreferenceManager.getDefaultSharedPreferences(context.get()).getStringSet("selected-search-provider-names", new HashSet<>(Collections.singletonList("Google")));
        Set<String> availableProviders = PreferenceManager.getDefaultSharedPreferences(context.get()).getStringSet("available-search-providers", SearchProvider.getSearchProviders(context.get()));

        for (String searchProvider : selectedProviders) {
            SearchPojo pojo = new SearchPojo();
            // Super low relevance, should never be displayed before anything
            pojo.relevance = -500;
            pojo.url = getProviderUrl(availableProviders, searchProvider);
            pojo.setName(searchProvider, false);
            if (pojo.url != null) {
                pojos.add(pojo);
            }
        }
        return pojos;
    }
}
