package fr.neamar.kiss.searcher;


import android.os.AsyncTask;

import java.util.ArrayList;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.Result;

public abstract class Searcher extends AsyncTask<String, Void, ArrayList<Pojo>> {
    private final int MAX_RECORDS = 15;

    protected final MainActivity activity;

    public Searcher(MainActivity activity) {
        super();
        this.activity = activity;
    }

    @Override
    protected void onPostExecute(ArrayList<Pojo> pojos) {
        super.onPostExecute(pojos);
        activity.adapter.clear();

        if (pojos != null) {
            for (int i = Math.min(MAX_RECORDS, pojos.size()) - 1; i >= 0; i--) {
                activity.adapter.add(Result.fromPojo(activity, pojos.get(i)));
            }
        }
        activity.resetTask();
    }
}
