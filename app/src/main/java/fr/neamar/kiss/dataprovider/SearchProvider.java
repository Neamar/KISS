package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadSearchPojos;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class SearchProvider extends Provider<SearchPojo> {

    @Override
    public void reload() {
        this.initialize(new LoadSearchPojos(this));
    }


    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> records = new ArrayList<>();

        for (Pojo pojo : pojos) {
            ((SearchPojo) pojo).query = query;
            // consider search pojos less relevant than apps
            pojo.relevance = 1;
            records.add(pojo);
        }

        return records;
    }
}
