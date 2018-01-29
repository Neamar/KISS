package fr.neamar.kiss.searcher;


import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.result.Result;

public abstract class Searcher extends AsyncTask<Void, Result, Void>
{
	static final        int             DEFAULT_MAX_RESULTS   = 50;
	static final        int             DEFAULT_REFRESH_TIMER = 150;
	// define a different thread than the default AsyncTask thread or else we will block everything else that uses AsyncTask while we search
	public static final ExecutorService SEARCH_THREAD         = Executors.newSingleThreadExecutor();

	private long start;
	private String query;
	final         WeakReference<MainActivity> activityWeakReference;
	private final PriorityQueue<Pojo>         processedPojos;
	private final RefreshTask                 refreshTask;
	private       int                         refreshCounter;
	// It's better to have a Handler than a Timer. Note that java.util.Timer (and TimerTask) will be deprecated in JDK 9
	private final Handler                     handler;

	public interface DataObserver
	{
		void beforeChange();
		void afterChange();
	}

	static class RefreshTask implements Runnable
	{
		volatile int runCounter = 0;

		@Override
		public void run()
		{
			runCounter += 1;
		}
	}

	/**
	 * Constructor
	 *
	 * @param activity
	 */
	Searcher( MainActivity activity, String query )
	{
		super();
		this.query = query;
		this.activityWeakReference = new WeakReference<>( activity );
		this.processedPojos = new PriorityQueue<>( DEFAULT_MAX_RESULTS, new PojoComparator() );
		this.refreshTask = new RefreshTask();
		this.refreshCounter = 0;
		// This handler should run on the Ui thread. That's the only thread that can't be blocked.
		this.handler = new Handler( Looper.getMainLooper() );
	}

	protected int getMaxResultCount()
	{
		return DEFAULT_MAX_RESULTS;
	}

	/**
	 * This is called from the background thread by the providers
	 */
	public boolean addResult( Pojo... pojos )
	{
		if( isCancelled() )
			return false;

		MainActivity activity = activityWeakReference.get();
		if( activity == null )
			return false;

		this.processedPojos.addAll( Arrays.asList( pojos ) );
		int maxResults = getMaxResultCount();
		while( this.processedPojos.size() > maxResults )
			this.processedPojos.poll();

		int counter = this.refreshTask.runCounter;
		// if timer passed, refresh list
		if( this.refreshCounter < counter )
		{
			this.refreshCounter = counter;

			// clone the PriorityQueue so we can extract the pojos in sorted order and still keep them in the original list
			PriorityQueue<Pojo> queue   = new PriorityQueue<>( this.processedPojos );
			Collection<Result>  results = new ArrayList<>( queue.size() );
			while( queue.peek() != null )
			{
				results.add( Result.fromPojo( activity, queue.poll() ) );
			}
			// make the adapter update from the main thread
			publishProgress( results.toArray( new Result[0] ) );
		}

		return true;
	}

	@CallSuper
	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		start = System.currentTimeMillis();

		MainActivity activity = activityWeakReference.get();
		if( activity == null )
			return;

		this.handler.postDelayed( this.refreshTask, DEFAULT_REFRESH_TIMER );
		activity.displayLoader( true );
	}

	@Override
	protected void onProgressUpdate( Result... results )
	{
		MainActivity activity = activityWeakReference.get();
		if( activity == null )
			return;

		activity.beforeChange();

		activity.adapter.clear();
		activity.adapter.addAll( results );

		activity.afterChange();
	}

	@Override
	protected void onPostExecute( Void param )
	{
		this.handler.removeCallbacks( this.refreshTask );

		MainActivity activity = activityWeakReference.get();
		if( activity == null )
			return;

		activity.displayLoader( false );

		if( this.processedPojos.isEmpty() )
		{
			activity.adapter.clear();
		}
		else
		{
			PriorityQueue<Pojo> queue   = this.processedPojos;
			Collection<Result>  results = new ArrayList<>( queue.size() );
			while( queue.peek() != null )
			{
				results.add( Result.fromPojo( activity, queue.poll() ) );
			}
			activity.beforeChange();

			activity.adapter.clear();
			activity.adapter.addAll( results );

			activity.afterChange();
		}

		activity.resetTask();

		long time = System.currentTimeMillis() - start;
		Log.v("Timing", "Time to run query `" + query + "` to completion: " + time + "ms");
	}
}
