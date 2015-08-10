package fr.neamar.kiss.loader;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.pojo.DuckduckgoSearchPojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class LoadDuckduckgoSearchPojos extends LoadPojos<DuckduckgoSearchPojo> {

    public LoadDuckduckgoSearchPojos(Context context) {
        super(context, "none://");
    }

    @Override
    protected ArrayList<DuckduckgoSearchPojo> doInBackground(Void... params) {
        return null;
    }
}
