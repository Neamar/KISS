package fr.neamar.kiss.searcher;


import android.os.AsyncTask;

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
    protected static final int DEFAULT_MAX_RESULTS = 25;

    final         MainActivity        activity;
    private final PriorityQueue<Pojo> processedPojos;

    Searcher(MainActivity activity) {
        super();
        this.activity = activity;
        this.processedPojos = new PriorityQueue<>( DEFAULT_MAX_RESULTS, new PojoComparator(true) );
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
    }
}
