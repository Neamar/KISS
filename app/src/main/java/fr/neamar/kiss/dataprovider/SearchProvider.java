package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadSearchPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchProvider extends Provider<SearchPojo> {

    @Override
    public void reload() {
        this.initialize(new LoadSearchPojos(this));
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> pojos = new ArrayList<>();

        SearchPojo pojo = new SearchPojo();
        pojo.query = query;
        pojo.relevance = 10;
        pojos.add(pojo);
        return pojos;
    }
}
