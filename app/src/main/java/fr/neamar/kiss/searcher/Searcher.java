package fr.neamar.kiss.searcher;


import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.result.Result;

public abstract class Searcher extends AsyncTask<Void, Pojo, List<Pojo>> {
    static final int DEFAULT_MAX_RESULTS = 25;
    private long start;
    private String query;

    final         MainActivity        activity;
    private final PriorityQueue<Pojo> processedPojos;

    Searcher(MainActivity activity, String query) {
        super();
        this.query = query;
        this.activity = activity;
        this.processedPojos = new PriorityQueue<>( DEFAULT_MAX_RESULTS, new PojoComparator(true) );
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        start = System.currentTimeMillis();
    }

    /**
     * This is called from the background thread by the provides
     */
    public void addResult( Pojo... pojos )
    {
        publishProgress( pojos );
    }

    @Override
    protected void onProgressUpdate( Pojo... pojos )
    {
        super.onProgressUpdate( pojos );

        this.processedPojos.addAll( Arrays.asList( pojos ) );
        while ( this.processedPojos.size() > DEFAULT_MAX_RESULTS )
            this.processedPojos.poll();

        Collection<Result> results = new ArrayList<>( DEFAULT_MAX_RESULTS );
        for ( Pojo pojo : this.processedPojos )
        {
            results.add( Result.fromPojo( activity, pojo ) );
        }
        activity.adapter.setNotifyOnChange( false );
        activity.adapter.clear();
        activity.adapter.addAll( results );
        activity.adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostExecute(List<Pojo> pojos) {
        super.onPostExecute(pojos);

        if ( this.processedPojos.isEmpty() )
        {
            activity.adapter.clear();
        }

        if (pojos != null) {
            Collection<Result> results = new ArrayList<>();

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
