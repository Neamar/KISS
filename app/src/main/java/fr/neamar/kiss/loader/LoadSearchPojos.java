package fr.neamar.kiss.loader;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.pojo.SearchPojo;

public class LoadSearchPojos extends LoadPojos<SearchPojo> {

    public LoadSearchPojos(Context context) {
        super(context, "none://");
    }

    @Override
    protected ArrayList<SearchPojo> doInBackground(Void... params) {
        return null;
    }
}
