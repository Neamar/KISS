package fr.neamar.kiss.loader;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.pojo.MathPojo;

public class LoadMathPojos extends LoadPojos<MathPojo> {
    public LoadMathPojos(Context context) {
        super(context, "calc://");
    }

    @Override
    protected ArrayList<MathPojo> doInBackground(Void... voids) {
        return null;
    }
}
