package fr.neamar.kiss.searcher;


import android.os.AsyncTask;
import android.support.annotation.CallSuper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.result.Result;

public abstract class Searcher extends AsyncTask<Void, Result, Void>
{
	static final int DEFAULT_MAX_RESULTS   = 25;
	static final int DEFAULT_REFRESH_TIMER = 150;

	final         MainActivity        activity;
	private final PriorityQueue<Pojo> processedPojos;
	private final RefreshTask         refreshTask;
	private       int                 refreshCounter;
	private final Timer               timer;

	class RefreshTask extends TimerTask
	{
		volatile int runCounter = 0;

		@Override
		public void run()
		{
			runCounter += 1;
		}
	}


	Searcher( MainActivity activity )
	{
		super();
		this.activity = activity;
		this.processedPojos = new PriorityQueue<>( DEFAULT_MAX_RESULTS, new PojoComparator( true ) );
		this.refreshTask = new RefreshTask();
		this.refreshCounter = 0;
		this.timer = new Timer( );
	}

	/**
	 * This is called from the background thread by the providers
	 */
	public boolean addResult( Pojo... pojos )
	{
		if( isCancelled() )
			return false;

		this.processedPojos.addAll( Arrays.asList( pojos ) );
		while( this.processedPojos.size() > DEFAULT_MAX_RESULTS )
			this.processedPojos.poll();

		int counter = this.refreshTask.runCounter;
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
		this.timer.scheduleAtFixedRate( this.refreshTask, DEFAULT_REFRESH_TIMER, DEFAULT_REFRESH_TIMER );
	}

	@Override
	protected void onProgressUpdate( Result... results )
	{
		activity.adapter.setNotifyOnChange( false );
		activity.adapter.clear();
		activity.adapter.addAll( results );
		activity.adapter.notifyDataSetChanged();
	}

	@Override
	protected void onPostExecute( Void param )
	{
		super.onPostExecute( param );

		this.timer.cancel();

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
			activity.adapter.setNotifyOnChange( false );
			activity.adapter.clear();
			activity.adapter.addAll( results );
			activity.adapter.notifyDataSetChanged();
		}

		activity.resetTask();
	}
}
