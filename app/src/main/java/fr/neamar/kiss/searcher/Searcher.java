package fr.neamar.kiss.searcher;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.CallSuper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.RelevanceComparator;
import fr.neamar.kiss.result.Result;

public abstract class Searcher extends AsyncTask<Void, Result<?>, Void> {

    private static final String TAG = Searcher.class.getSimpleName();

    // define a different thread than the default AsyncTask thread or else we will block everything else that uses AsyncTask while we search
    public static final ExecutorService SEARCH_THREAD = Executors.newSingleThreadExecutor();
    static final int DEFAULT_MAX_RESULTS = 50;
    final WeakReference<MainActivity> activityWeakReference;
    private final PriorityQueue<Pojo> processedPojos;
    private long start;
    /**
     * Set to true when we are simply refreshing current results (scroll will not be reset)
     * When false, we reset the scroll back to the last item in the list
     */
    private final boolean isRefresh;
    protected final String query;

    Searcher(MainActivity activity, String query, boolean isRefresh) {
        super();
        this.isRefresh = isRefresh;
        this.query = query == null ? null : query.trim();
        this.activityWeakReference = new WeakReference<>(activity);
        this.processedPojos = getPojoProcessor(activity);
    }

    PriorityQueue<Pojo> getPojoProcessor(Context context) {
        return new PriorityQueue<>(DEFAULT_MAX_RESULTS, new RelevanceComparator());
    }

    protected int getMaxResultCount() {
        return DEFAULT_MAX_RESULTS;
    }

    /**
     * Add single pojo to results.
     * This is called from the background thread by the providers.
     */
    public final boolean addResult(Pojo pojos) {
        return addResults(Collections.singletonList(pojos));
    }

    /**
     * Add one or more pojos to results.
     * This is called from the background thread by the providers.
     */
    public boolean addResults(List<? extends Pojo> pojos) {
        if (isCancelled())
            return false;

        return this.processedPojos.addAll(pojos);
    }

    @CallSuper
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        start = System.currentTimeMillis();

        displayActivityLoader();
    }

    protected void displayActivityLoader() {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        activity.displayLoader(true);
    }

    private void hideActivityLoader(MainActivity activity) {
        // Loader should still be displayed until all the providers have finished loading
        activity.displayLoader(!KissApplication.getApplication(activity).getDataHandler().allProvidersHaveLoaded);
    }

    @Override
    protected void onPostExecute(Void param) {
        if (isCancelled()) {
            return;
        }

        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        hideActivityLoader(activity);

        if (this.processedPojos.isEmpty()) {
            activity.adapter.clear();
        } else {
            PriorityQueue<Pojo> queue = this.processedPojos;
            int maxResults = getMaxResultCount();
            while (queue.size() > maxResults)
                queue.poll();
            List<Result<?>> results = new ArrayList<>(queue.size());
            while (queue.peek() != null) {
                results.add(Result.fromPojo(activity, queue.poll()));
            }

            activity.beforeListChange();

            activity.adapter.updateResults(activity, results, isRefresh, query);

            activity.afterListChange();
        }

        activity.resetTask();

        long time = System.currentTimeMillis() - start;
        Log.v(TAG, "Time to run query `" + query + "` on " + getClass().getSimpleName() + " to completion: " + time + "ms");
    }

    @Override
    protected void onCancelled(Void unused) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        hideActivityLoader(activity);
    }
}
