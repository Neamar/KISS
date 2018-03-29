package fr.neamar.kiss.searcher;


import android.content.Context;
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

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.result.Result;

public abstract class Searcher extends AsyncTask<Void, Result, Void> {
    // define a different thread than the default AsyncTask thread or else we will block everything else that uses AsyncTask while we search
    public static final ExecutorService SEARCH_THREAD = Executors.newSingleThreadExecutor();
    static final int DEFAULT_MAX_RESULTS = 50;
    private static final int DEFAULT_REFRESH_TIMER = 150;
    final WeakReference<MainActivity> activityWeakReference;
    private final PriorityQueue<Pojo> processedPojos;
    private final RefreshTask refreshTask;
    // It's better to have a Handler than a Timer. Note that java.util.Timer (and TimerTask) will be deprecated in JDK 9
    private final Handler handler;
    private long start;
    private final String query;

    Searcher(MainActivity activity, String query) {
        super();
        this.query = query;
        this.activityWeakReference = new WeakReference<>(activity);
        this.processedPojos = getPojoProcessor(activity);
        this.refreshTask = new RefreshTask(this);
        // This handler should run on the Ui thread. That's the only thread that can't be blocked.
        this.handler = new Handler(Looper.getMainLooper());
    }

    PriorityQueue<Pojo> getPojoProcessor(Context context) {
        return new PriorityQueue<>(DEFAULT_MAX_RESULTS, new PojoComparator());
    }

    int getMaxResultCount() {
        return DEFAULT_MAX_RESULTS;
    }

    Collection<Result> queueToList(PriorityQueue<Pojo> priorityQueue) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return new ArrayList<>();

        Collection<Result> results = new ArrayList<>(priorityQueue.size());
        while (priorityQueue.peek() != null) {
            results.add(Result.fromPojo(activity, priorityQueue.poll()));
        }

        return results;
    }

    /**
     * This is called from the background thread by the providers
     */
    public boolean addResult(Pojo... pojos) {
        if (isCancelled())
            return false;

        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return false;

        this.processedPojos.addAll(Arrays.asList(pojos));
        int maxResults = getMaxResultCount();

        // Trim the list
        while (this.processedPojos.size() > maxResults)
            this.processedPojos.poll();

        return true;
    }

    @CallSuper
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        start = System.currentTimeMillis();

        this.handler.postDelayed(this.refreshTask, DEFAULT_REFRESH_TIMER);
        displayActivityLoader();
    }

    void displayActivityLoader() {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        activity.displayLoader(true);
    }

    @Override
    protected void onProgressUpdate(Result... results) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        activity.beforeListChange();

        activity.adapter.clear();
        activity.adapter.addAll(results);

        activity.afterListChange();
    }

    @Override
    protected void onPostExecute(Void param) {
        this.handler.removeCallbacks(this.refreshTask);

        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        // Loader should still be displayed until all the providers have finished loading
        activity.displayLoader(!KissApplication.getApplication(activity).getDataHandler().allProvidersHaveLoaded);

        if (this.processedPojos.isEmpty()) {
            activity.adapter.clear();
        } else {
            Collection<Result> results = queueToList(this.processedPojos);

            activity.beforeListChange();

            activity.adapter.clear();
            activity.adapter.addAll(results);

            activity.afterListChange();
        }

        activity.resetTask();

        long time = System.currentTimeMillis() - start;
        Log.v("Timing", "Time to run query `" + query + "` to completion: " + time + "ms");
    }

    public interface DataObserver {
        void beforeListChange();

        void afterListChange();
    }
    
    static class RefreshTask implements Runnable {
        Searcher searcher;

        RefreshTask(Searcher searcher) {
            this.searcher = searcher;
        }

        @Override
        // Periodically update the results displayed in the list
        public void run() {
            Log.e("WTF", "Refreshing");
            // clone the PriorityQueue so we can extract the pojos in sorted order and still keep them in the original list
            PriorityQueue<Pojo> queue = new PriorityQueue<>(searcher.processedPojos);
            searcher.publishProgress(searcher.queueToList(queue).toArray(new Result[0]));
        }
    }
}
