package fr.neamar.kiss.loader;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.pojo.GoogleSearchPojo;
import fr.neamar.kiss.pojo.SearchPojo;

public class LoadGoogleSearchPojos extends LoadPojos<GoogleSearchPojo> {

    public LoadGoogleSearchPojos(Context context) {
        super(context, "none://");
    }

    @Override
    protected ArrayList<GoogleSearchPojo> doInBackground(Void... params) {
        return null;
    }
}
