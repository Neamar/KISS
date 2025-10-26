package fr.neamar.kiss.searcher;

import androidx.annotation.NonNull;

import fr.neamar.kiss.MainActivity;

public class SearchHandler {

    private static volatile SearchHandler instance;

    public static SearchHandler getInstance() {
        if (instance == null) {
            synchronized (SearchHandler.class) {
                if (instance == null) {
                    instance = new SearchHandler();
                }
            }
        }
        return instance;
    }

    private SearchHandler() {
    }

    /**
     * Last search type, needed for refresh
     */
    private Searcher.Type lastSearchType;

    /**
     * Last search query, needed for refresh.
     */
    private String lastSearchQuery;

    /**
     * Running search task.
     */
    private Searcher runningSearch;

    /**
     * Create search task and execute.
     *
     * @param type      of search
     * @param activity  the main activity
     * @param query     the search query
     * @param isRefresh true, if refresh of last search is needed
     */
    public void search(@NonNull Searcher.Type type, @NonNull MainActivity activity, String query, boolean isRefresh) {
        cancelSearch();

        runningSearch = createSearcher(type, activity, query, isRefresh);
        runningSearch.setSearchDoneCallback((searcher, isCancelled) -> {
            if (runningSearch == searcher) {
                resetRunningSearch();
            }
        });
        runningSearch.executeOnExecutor(Searcher.SEARCH_THREAD);
    }

    /**
     * Cancel last search if still running.
     */
    public void cancelSearch() {
        if (runningSearch != null) {
            runningSearch.cancel(true);
            resetRunningSearch();
        }
    }

    private void resetRunningSearch() {
        runningSearch = null;
    }

    @NonNull
    private Searcher createSearcher(@NonNull Searcher.Type type, @NonNull MainActivity activity, String query, boolean isRefresh) {
        if (isRefresh && lastSearchType != null) {
            type = this.lastSearchType;
            query = this.lastSearchQuery;
        } else {
            this.lastSearchType = type;
            this.lastSearchQuery = query;
        }

        switch (type) {
            case APPLICATION:
                return new ApplicationsSearcher(activity, isRefresh);
            case QUERY:
                return new QuerySearcher(activity, query, isRefresh);
            case NULL:
                return new NullSearcher(activity);
            case HISTORY:
                return new HistorySearcher(activity, isRefresh);
            case TAGGED:
                return new TagsSearcher(activity, query);
            case UNTAGGED:
                return new UntaggedSearcher(activity);
            default:
                throw new UnsupportedOperationException();
        }
    }
}
