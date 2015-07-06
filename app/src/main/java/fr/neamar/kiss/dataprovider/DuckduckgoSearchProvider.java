package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadDuckduckgoSearchPojos;
import fr.neamar.kiss.pojo.DuckduckgoSearchPojo;
import fr.neamar.kiss.pojo.Pojo;

public class DuckduckgoSearchProvider extends Provider<DuckduckgoSearchPojo> {

    public DuckduckgoSearchProvider(Context context) {
        super(new LoadDuckduckgoSearchPojos(context));
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> pojos = new ArrayList<>();

        DuckduckgoSearchPojo pojo = new DuckduckgoSearchPojo();
        pojo.query = query;
        pojo.relevance = 10;
        pojos.add(pojo);
        return pojos;
    }
}
