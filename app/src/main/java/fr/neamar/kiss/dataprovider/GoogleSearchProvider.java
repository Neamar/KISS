package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadGoogleSearchPojos;
import fr.neamar.kiss.pojo.GoogleSearchPojo;
import fr.neamar.kiss.pojo.Pojo;

public class GoogleSearchProvider extends Provider<GoogleSearchPojo> {

    public GoogleSearchProvider(Context context) {
        super(new LoadGoogleSearchPojos(context));
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> pojos = new ArrayList<>();

        GoogleSearchPojo pojo = new GoogleSearchPojo();
        pojo.query = query;
        pojo.relevance = 10;
        pojos.add(pojo);
        return pojos;
    }
}
