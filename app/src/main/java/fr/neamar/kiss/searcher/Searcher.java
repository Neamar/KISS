package fr.neamar.kiss.searcher;


import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.Result;

public abstract class Searcher extends AsyncTask<Void, Void, List<Pojo>> {
    // define a different thread than the default AsyncTask thread or else we will block everything else that uses AsyncTask while we search
    public static final ExecutorService SEARCH_THREAD       = Executors.newSingleThreadExecutor();

    static final int DEFAULT_MAX_RESULTS = 25;

    private long start;
    private String query;

    final MainActivity activity;

    Searcher(MainActivity activity, String query) {
        super();
        this.activity = activity;
        this.query = query;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        start = System.currentTimeMillis();
    }

    @Override
    protected void onPostExecute(List<Pojo> pojos) {
        super.onPostExecute(pojos);
        activity.adapter.clear();

        Collection<Result> results = new ArrayList<>();

        if (pojos != null) {
            for (int i = pojos.size() - 1; i >= 0; i--) {
                results.add(Result.fromPojo(activity, pojos.get(i)));
            }

            activity.adapter.addAll(results);
        }
        activity.resetTask();

        long time = System.currentTimeMillis() - start;
        Log.v("Timing", "Time to run query `" + query + "` to completion: " + time + "ms");
    }
}
