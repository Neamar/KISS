package fr.neamar.kiss.searcher;


import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.pojo.PojoWithTags;
import fr.neamar.kiss.result.Result;

public abstract class Searcher extends AsyncTask<Void, Result, Void> {
    // define a different thread than the default AsyncTask thread or else we will block everything else that uses AsyncTask while we search
    public static final ExecutorService SEARCH_THREAD = Executors.newSingleThreadExecutor();
    static final Pattern patternTagSplit = Pattern.compile("\\s+");
    static final int DEFAULT_MAX_RESULTS = 50;
    final WeakReference<MainActivity> activityWeakReference;
    private final PriorityQueue<Pojo> processedPojos;
    private long start;
    protected final String query;

    Searcher(MainActivity activity, String query) {
        super();
        this.query = query;
        this.activityWeakReference = new WeakReference<>(activity);
        this.processedPojos = getPojoProcessor(activity);
    }

    PriorityQueue<Pojo> getPojoProcessor(Context context) {
        return new PriorityQueue<>(DEFAULT_MAX_RESULTS, new PojoComparator());
    }

    int getMaxResultCount() {
        return DEFAULT_MAX_RESULTS;
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

        Collections.addAll(this.processedPojos, pojos);
        int maxResults = getMaxResultCount();
        while (this.processedPojos.size() > maxResults)
            this.processedPojos.poll();

        return true;
    }

    @CallSuper
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        start = System.currentTimeMillis();

        displayActivityLoader();
    }

    void displayActivityLoader() {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        activity.displayLoader(true);
    }

    @Override
    protected void onPostExecute(Void param) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        // Loader should still be displayed until all the providers have finished loading
        activity.displayLoader(!KissApplication.getApplication(activity).getDataHandler().allProvidersHaveLoaded);

        if (this.processedPojos.isEmpty()) {
            activity.adapter.clear();
        } else {
            PriorityQueue<Pojo> queue = this.processedPojos;
            ArrayList<Result> results = new ArrayList<>(queue.size());
            while (queue.peek() != null) {
                results.add(Result.fromPojo(activity, queue.poll()));
            }
            activity.beforeListChange();

            activity.adapter.updateResults(results);

            activity.afterListChange();
        }

        activity.resetTask();

        long time = System.currentTimeMillis() - start;
        Log.v("Timing", "Time to run query `" + query + "` on " + getClass().getSimpleName() + " to completion: " + time + "ms");
    }

    static boolean isTagFilterOk(@Nullable MainActivity activity, @NonNull PojoWithTags pojo) {
        if (activity == null)
            return false;
        if (pojo.getTags() != null && !pojo.getTags().isEmpty()) {
            // split tags string so we can search faster
            TreeSet<String> tagList = null;
            boolean excludeTagsPresent = !activity.getExcludeTags().isEmpty();
            boolean includeTagsPresent = !activity.getIncludeTags().isEmpty();
            if ( excludeTagsPresent || includeTagsPresent )
            {
                tagList = new TreeSet<>();
                Collections.addAll(tagList, patternTagSplit.split(pojo.getTags()));
            }

            if ( excludeTagsPresent ) {
                // remove pojos that contain tags that should be hidden
                for (String tag : tagList) {
                    if (activity.getExcludeTags().contains(tag)) {
                        return false;
                    }
                }
            }
            if (includeTagsPresent) {
                // remove pojos if they don't have the include tags
                boolean bIncludeTagFound = false;
                for (String tag : activity.getIncludeTags()) {
                    if (tagList.contains(tag)) {
                        bIncludeTagFound = true;
                        break;
                    }
                }
                if (!bIncludeTagFound) {
                    return false;
                }
            }
        } else if (!activity.getIncludeTags().isEmpty()) {
            // if we have "must have" tags but the app has no tags, remove it
            return false;
        }
        return true;
    }


    public interface DataObserver {
        void beforeListChange();

        void afterListChange();
    }
}
