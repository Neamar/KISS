package fr.neamar.kiss.result;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleableRes;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.pojo.PhonePojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.pojo.SettingPojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;
import fr.neamar.kiss.utils.fuzzy.MatchInfo;

public abstract class Result<T extends Pojo> {

    /**
     * Current information pojo
     */
    @NonNull
    protected final T pojo;

    Result(@NonNull T pojo) {
        this.pojo = pojo;
    }

    public static Result<?> fromPojo(QueryInterface parent, @NonNull Pojo pojo) {
        if (pojo instanceof AppPojo)
            return new AppResult((AppPojo) pojo);
        else if (pojo instanceof ContactsPojo)
            return new ContactsResult(parent, (ContactsPojo) pojo);
        else if (pojo instanceof SearchPojo)
            return new SearchResult((SearchPojo) pojo);
        else if (pojo instanceof SettingPojo)
            return new SettingsResult((SettingPojo) pojo);
        else if (pojo instanceof PhonePojo)
            return new PhoneResult((PhonePojo) pojo);
        else if (pojo instanceof ShortcutPojo)
            return new ShortcutsResult((ShortcutPojo) pojo);
        else if (pojo instanceof TagDummyPojo)
            return new TagDummyResult((TagDummyPojo) pojo);

        throw new UnsupportedOperationException("Unable to create a result from POJO");
    }

    public String getPojoId() {
        return pojo.id;
    }

    @Override
    public String toString() {
        return pojo.getName();
    }

    /**
     * How to display this record ?
     *
     * @param context     android context
     * @param convertView a view to be recycled
     * @param parent      view that provides a set of LayoutParams values
     * @param fuzzyScore  information for highlighting search result
     * @return a view to display as item
     */
    @NonNull
    public abstract View display(Context context, View convertView, @NonNull ViewGroup parent, FuzzyScore fuzzyScore);

    @NonNull
    public View inflateFavorite(@NonNull Context context, @NonNull ViewGroup parent) {
        View favoriteView = LayoutInflater.from(context).inflate(R.layout.favorite_item, parent, false);
        ImageView favoriteImage = favoriteView.findViewById(R.id.favorite);
        setAsyncDrawable(favoriteImage, R.drawable.ic_launcher_white);
        favoriteView.setContentDescription(pojo.getName());
        return favoriteView;
    }

    void displayHighlighted(String text, List<Pair<Integer, Integer>> positions, TextView view, Context context) {
        SpannableString enriched = new SpannableString(text);
        int primaryColor = UIColors.getPrimaryColor(context);

        for (Pair<Integer, Integer> position : positions) {
            enriched.setSpan(
                    new ForegroundColorSpan(primaryColor),
                    position.first,
                    position.second,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
            );
        }
        view.setText(enriched);
    }

    boolean displayHighlighted(StringNormalizer.Result normalized, String text, FuzzyScore fuzzyScore,
                               TextView view, Context context) {
        MatchInfo matchInfo = fuzzyScore.match(normalized.codePoints);

        if (!matchInfo.match) {
            view.setText(text);
            return false;
        }

        SpannableString enriched = new SpannableString(text);
        int primaryColor = UIColors.getPrimaryColor(context);

        for (Pair<Integer, Integer> position : matchInfo.getMatchedSequences()) {
            enriched.setSpan(
                    new ForegroundColorSpan(primaryColor),
                    normalized.mapPosition(position.first),
                    normalized.mapPosition(position.second),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
            );
        }
        view.setText(enriched);
        return true;
    }

    public String getSection() {
        try {
            // get the normalized first letter of the pojo
            // Ensure accented characters are never displayed. (É => E)
            String ch = Character.toString((char) pojo.normalizedName.codePoints[0]);
            // convert to uppercase otherwise lowercase a -z will be sorted
            // after upper A-Z
            return ch.toUpperCase(Locale.getDefault());
        } catch (ArrayIndexOutOfBoundsException e) {
            // Normalized name is empty.
            return "-";
        }
    }

    /**
     * How to display the popup menu
     *
     * @return a PopupMenu object
     */
    public ListPopup getPopupMenu(final Context context, final RecordAdapter parent, final View parentView) {
        ArrayAdapter<ListPopup.Item> popupMenuAdapter = new ArrayAdapter<>(context, R.layout.popup_list_item);
        ListPopup menu = buildPopupMenu(context, popupMenuAdapter, parent, parentView);

        menu.setOnItemClickListener((adapter, view, position) -> {
            @StringRes int stringId = ((ListPopup.Item) adapter.getItem(position)).stringId;
            popupMenuClickHandler(view.getContext(), parent, stringId, parentView);
        });

        return menu;
    }

    /**
     * Default popup menu implementation, can be overridden by children class to display a more specific menu
     *
     * @return an inflated, listener-free PopupMenu
     */
    ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        return inflatePopupMenu(adapter, context);
    }

    ListPopup inflatePopupMenu(ArrayAdapter<ListPopup.Item> adapter, Context context) {
        ListPopup menu = new ListPopup(context);
        menu.setAdapter(adapter);

        // If app already pinned, do not display the "add to favorite" option
        // otherwise don't show the "remove favorite button"
        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");
        if (favApps.contains(this.pojo.id + ";")) {
            for (int i = 0; i < adapter.getCount(); i += 1) {
                ListPopup.Item item = adapter.getItem(i);
                assert item != null;
                if (item.stringId == R.string.menu_favorites_add) {
                    adapter.remove(item);
                }
            }
        } else {
            for (int i = 0; i < adapter.getCount(); i += 1) {
                ListPopup.Item item = adapter.getItem(i);
                assert item != null;
                if (item.stringId == R.string.menu_favorites_remove) {
                    adapter.remove(item);
                }
            }
        }

        if (BuildConfig.DEBUG) {
            adapter.add(new ListPopup.Item("Relevance: " + pojo.relevance));
        }

        return menu;
    }

    /**
     * Handler for popup menu action.
     * Default implementation only handle remove from history action.
     *
     * @return Works in the same way as onOptionsItemSelected, return true if the action has been handled, false otherwise
     */
    boolean popupMenuClickHandler(Context context, RecordAdapter parent, @StringRes int stringId, View parentView) {
        if (stringId == R.string.menu_remove) {
            removeFromResultsAndHistory(context, parent);
            return true;
        } else if (stringId == R.string.menu_favorites_add) {
            launchAddToFavorites(context, pojo);
        } else if (stringId == R.string.menu_favorites_remove) {
            launchRemoveFromFavorites(context, pojo);
        }

        MainActivity mainActivity = (MainActivity) context;
        // Update favorite bar
        mainActivity.onFavoriteChange();
        mainActivity.launchOccurred();
        // Update Search to reflect favorite add, if the "exclude favorites" option is active
        if (mainActivity.prefs.getBoolean("exclude-favorites-history", false) && mainActivity.isViewingSearchResults()) {
            mainActivity.updateSearchRecords();
        }

        return false;
    }

    private void launchAddToFavorites(Context context, Pojo pojo) {
        String msg = context.getResources().getString(R.string.toast_favorites_added);
        KissApplication.getApplication(context).getDataHandler().addToFavorites(pojo.getFavoriteId());
        Toast.makeText(context, String.format(msg, pojo.getName()), Toast.LENGTH_SHORT).show();
    }

    private void launchRemoveFromFavorites(Context context, Pojo pojo) {
        String msg = context.getResources().getString(R.string.toast_favorites_removed);
        KissApplication.getApplication(context).getDataHandler().removeFromFavorites(pojo.getFavoriteId());
        Toast.makeText(context, String.format(msg, pojo.getName()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Remove the current result from the list
     *
     * @param context android context
     * @param parent  adapter on which to remove the item
     */
    private void removeFromResultsAndHistory(Context context, RecordAdapter parent) {
        removeFromHistory(context);
        Toast.makeText(context, R.string.removed_item, Toast.LENGTH_SHORT).show();
        parent.removeResult(context, this);
    }

    public final void launch(Context context, View v, @Nullable QueryInterface queryInterface) {
        Log.i(this.getClass().getSimpleName(), "Launching " + pojo.id);

        // Launch
        doLaunch(context, v);

        recordLaunch(context, queryInterface);
    }

    protected final void recordLaunch(Context context, @Nullable QueryInterface queryInterface) {
        // Save in history
        if (canAddToHistory()) {
            KissApplication.getApplication(context).getDataHandler().addToHistory(pojo.getHistoryId());
        }
        // Record the launch after some period,
        // * to ensure the animation runs smoothly
        // * to avoid a flickering -- launchOccurred will refresh the list
        // Thus TOUCH_DELAY * 3
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (queryInterface != null) {
                queryInterface.launchOccurred();
            }
        }, KissApplication.TOUCH_DELAY * 3);
    }

    /**
     * to check if launch can be added to history
     * @return true, if launch can be added to history
     */
    protected boolean canAddToHistory() {
        return !pojo.isDisabled();
    }

    /**
     * How to launch this record ? Most probably, will fire an intent.
     *
     * @param context android context
     */
    protected abstract void doLaunch(Context context, View v);

    /**
     * How to launch this record "quickly" ? Most probably, same as doLaunch().
     * Override to define another behavior.
     *
     * @param context android context
     */
    public void fastLaunch(Context context, View v) {
        this.launch(context, v, null);
    }

    /**
     * Return the icon for this Result, or null if non existing.
     *
     * @param context android context
     */
    public Drawable getDrawable(Context context) {
        return null;
    }

    /**
     * Does the drawable changes regularly?
     * If so, it can't be kept in cache for long.
     *
     * @return true when dynamic
     */
    public boolean isDrawableDynamic() {
        return false;
    }

    boolean isDrawableCached() {
        return false;
    }

    void setDrawableCache(Drawable drawable) {
    }

    void setAsyncDrawable(ImageView view) {
        setAsyncDrawable(view, android.R.color.transparent);
    }

    void setAsyncDrawable(ImageView view, @DrawableRes int resId) {
        // getting this called multiple times in parallel may result in empty icons
        synchronized (this) {
            // the ImageView tag will store the async task if it's running
            if (view.getTag() instanceof AsyncSetImage) {
                AsyncSetImage asyncSetImage = (AsyncSetImage) view.getTag();
                if (this.equals(asyncSetImage.resultWeakReference.get())) {
                    // we are already loading the icon for this
                    return;
                } else {
                    asyncSetImage.cancel(true);
                    view.setTag(null);
                }
            }
            // the ImageView will store the Result after the AsyncTask finished
            else if (this.equals(view.getTag())) {
                ((Result<?>) view.getTag()).setDrawableCache(view.getDrawable());
                return;
            }
            if (isDrawableCached()) {
                view.setImageDrawable(getDrawable(view.getContext()));
                view.setTag(this);
            } else {
                // use AsyncTask.SERIAL_EXECUTOR explicitly for now
                // TODO: make execution parallel if needed/possible
                view.setTag(createAsyncSetImage(view, resId).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR));
            }
        }
    }

    private AsyncSetImage createAsyncSetImage(ImageView imageView, @DrawableRes int resId) {
        return new AsyncSetImage(imageView, this, resId);
    }

    /**
     * Helper function to get a view
     *
     * @param context android context
     * @param id      id to inflate
     * @param parent  view that provides a set of LayoutParams values
     * @return the view specified by the id
     */
    View inflateFromId(Context context, @LayoutRes int id, @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(id, parent, false);
    }

    void removeFromHistory(Context context) {
        DBHelper.removeFromHistory(context, pojo.getHistoryId());
    }

    /*
     * Get fill color from theme
     *
     */
    int getThemeFillColor(Context context) {
        @SuppressLint("ResourceType") @StyleableRes
        int[] attrs = new int[]{R.attr.resultColor /* index 0 */};
        TypedArray ta = context.obtainStyledAttributes(attrs);
        int color = ta.getColor(0, Color.WHITE);
        ta.recycle();
        return color;
    }

    public long getUniqueId() {
        // we can consider hashCode unique enough in this context
        return this.pojo.id.hashCode();
    }

    static class AsyncSetImage extends AsyncTask<Void, Void, Drawable> {
        final WeakReference<ImageView> imageViewWeakReference;
        final WeakReference<Result<?>> resultWeakReference;

        AsyncSetImage(ImageView image, Result<?> result, @DrawableRes int resId) {
            super();
            image.setTag(this);
            image.setImageResource(resId);
            this.imageViewWeakReference = new WeakReference<>(image);
            this.resultWeakReference = new WeakReference<>(result);
        }

        @Override
        protected Drawable doInBackground(Void... voids) {
            ImageView image = imageViewWeakReference.get();
            if (isCancelled() || image == null || image.getTag() != this) {
                imageViewWeakReference.clear();
                return null;
            }
            Result<?> result = resultWeakReference.get();
            if (result == null) {
                return null;
            }
            return result.getDrawable(image.getContext());
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            ImageView image = imageViewWeakReference.get();
            if (isCancelled() || image == null || drawable == null) {
                imageViewWeakReference.clear();
                return;
            }
            image.setImageDrawable(drawable);
            image.setTag(resultWeakReference.get());
        }
    }

    protected boolean isHideIcons(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("icons-hide", false);
    }

    protected boolean isTagsVisible(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("tags-visible", true);
    }

    protected boolean isSubIconVisible(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("subicon-visible", true);
    }

    protected void setSourceBounds(Intent intent, View view) {
        intent.setSourceBounds(getViewBounds(view));
    }

    protected Rect getViewBounds(View view) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return view.getClipBounds();
        }
        return null;
    }

}
