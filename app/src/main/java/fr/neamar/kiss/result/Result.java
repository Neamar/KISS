package fr.neamar.kiss.result;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleableRes;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
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
import fr.neamar.kiss.utils.ClipboardUtils;
import fr.neamar.kiss.utils.Utilities;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;
import fr.neamar.kiss.utils.fuzzy.MatchInfo;

public abstract class Result<T extends Pojo> {

    private static final String TAG = Result.class.getSimpleName();
    private static final int TAG_RUNNING_TASK = R.id.item_app_icon;

    /**
     * Current information pojo
     */
    @NonNull
    protected final T pojo;

    protected Result(@NonNull T pojo) {
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
        setAsyncDrawable(favoriteImage, 0);
        favoriteView.setContentDescription(pojo.getName());
        return favoriteView;
    }

    protected void displayHighlighted(String text, List<Pair<Integer, Integer>> positions, TextView view, Context context) {
        SpannableString enriched = new SpannableString(text);
        Set<String> resultHighlighting = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet("pref-result-highlighting", Collections.singleton("color"));

        if (!resultHighlighting.isEmpty()) {
            int primaryColor = UIColors.getPrimaryColor(context);
            int len = text.length();
            for (Pair<Integer, Integer> position : positions) {
                if (position.first <= len) {
                    for (String highlight : resultHighlighting) {
                        Object span = createSpan(highlight, primaryColor);
                        if (span != null) {
                            enriched.setSpan(
                                    span,
                                    position.first,
                                    Math.min(position.second, len),
                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                            );
                        }
                    }
                }
            }
        }
        view.setText(enriched);
    }

    protected boolean displayHighlighted(StringNormalizer.Result normalized, String text, FuzzyScore fuzzyScore,
                               TextView view, Context context) {
        MatchInfo matchInfo = fuzzyScore.match(normalized.codePoints);

        if (!matchInfo.match) {
            view.setText(text);
            return false;
        }

        displayHighlighted(text, getMatchedSequences(matchInfo, normalized), view, context);
        return true;
    }

    private List<Pair<Integer, Integer>> getMatchedSequences(MatchInfo matchInfo, StringNormalizer.Result normalized) {
        List<Integer> matchedIndices = matchInfo.getMatchedIndices();
        if (matchedIndices == null) {
            return Collections.emptyList();
        }

        // compute pair match indices
        List<Pair<Integer, Integer>> positions = new ArrayList<>(matchedIndices.size());
        int start = matchedIndices.get(0);
        int end = start + 1;
        for (int i = 1; i < matchedIndices.size(); i += 1) {
            if (end == matchedIndices.get(i)) {
                end += 1;
            } else {
                positions.add(new Pair<>(normalized.mapPosition(start), normalized.mapPosition(end)));
                start = matchedIndices.get(i);
                end = start + 1;
            }
        }
        positions.add(new Pair<>(normalized.mapPosition(start), normalized.mapPosition(end)));
        return positions;
    }

    private Object createSpan(String highlight, int primaryColor) {
        switch (highlight) {
            case "color":
                return new ForegroundColorSpan(primaryColor);
            case "bold":
                return new StyleSpan(Typeface.BOLD);
            case "italic":
                return new StyleSpan(Typeface.ITALIC);
            case "underline":
                return new UnderlineSpan();
            default:
                return null;
        }
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
        ListPopup menu = buildPopupMenu(context, popupMenuAdapter);

        menu.setOnItemClickListener((adapter, view, position) -> {
            ListPopup.Item item = (ListPopup.Item) adapter.getItem(position);
            @StringRes int stringId = item.stringId;
            boolean handled = popupMenuClickHandler(view.getContext(), parent, stringId, parentView);
            if (!handled && stringId == 0) {
                ClipboardUtils.setClipboard(view.getContext(), "Copied Text", item.toString());
                Toast.makeText(context, R.string.copy_confirmation, Toast.LENGTH_SHORT).show();
            }
        });

        return menu;
    }

    /**
     * Default popup menu implementation, can be overridden by children class to display a more specific menu
     *
     * @return an inflated, listener-free PopupMenu
     */
    ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter) {
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
//            adapter.add(new ListPopup.Item("ID: " + pojo.getHistoryId()));
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
            return true;
        } else if (stringId == R.string.menu_favorites_remove) {
            launchRemoveFromFavorites(context, pojo);
            return true;
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
     * Return the icon for this Result, or null if nonexisting.
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

    protected void setAsyncDrawable(ImageView view) {
        setAsyncDrawable(view, android.R.color.transparent);
    }

    protected void setAsyncDrawable(ImageView view, @DrawableRes int resId) {
        setAsyncDrawable(view, resId, false, this::isDrawableCached, this::getDrawable, this::setDrawableCache);
    }

    protected void setAsyncDrawable(@NonNull ImageView imageView,
                                    @DrawableRes int defaultResId,
                                    boolean invalidateDrawable,
                                    @NonNull Supplier<Boolean> isCachedSupplier,
                                    @NonNull Function<Context, Drawable> drawableGetter,
                                    @NonNull Consumer<Drawable> cachedDrawableSetter) {
        Utilities.AsyncRun<Drawable> taskToCancel = getTask(imageView);
        if (taskToCancel != null) {
            if (!taskToCancel.isCancelled()) {
                taskToCancel.cancel();
            }
            imageView.setTag(TAG_RUNNING_TASK, null);
        }

        if (isCachedSupplier.get()) {
            imageView.setImageDrawable(drawableGetter.apply(imageView.getContext()));
            imageView.setTag(TAG_RUNNING_TASK, null);
        } else {
            if (defaultResId != 0) {
                imageView.setImageResource(defaultResId);
            }
            Utilities.AsyncRun<Drawable> newTask = Utilities.runAsync((task) -> {
                if (task.isCancelled()) {
                    return null;
                }
                return drawableGetter.apply(imageView.getContext());
            }, (task, drawable) -> {
                if (task.isCancelled() || drawable == null) {
                    Log.w(TAG, "Cannot set drawable for " + getPojoId());
                } else {
                    imageView.setTag(TAG_RUNNING_TASK, null);
                    cachedDrawableSetter.accept(drawable);
                    imageView.setImageDrawable(drawable);
                    if (invalidateDrawable) {
                        imageView.invalidateDrawable(drawable);
                    }
                }
            });
            imageView.setTag(TAG_RUNNING_TASK, newTask);
        }
    }

    @SuppressWarnings("unchecked")
    private Utilities.AsyncRun<Drawable> getTask(ImageView view) {
        Object tag = view.getTag(TAG_RUNNING_TASK);
        return tag instanceof Utilities.AsyncRun ? (Utilities.AsyncRun<Drawable>) tag : null;
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
        @StyleableRes int[] attrs = new int[]{R.attr.resultColor};
        TypedArray ta = context.obtainStyledAttributes(attrs);
        int color = ta.getColor(0, Color.WHITE);
        ta.recycle();
        return color;
    }

    public long getUniqueId() {
        // we can consider hashCode unique enough in this context
        return this.pojo.id.hashCode();
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

    protected void setSourceBounds(@NonNull Intent intent, @Nullable View view) {
        intent.setSourceBounds(getViewBounds(view));
    }

    @Nullable
    protected Rect getViewBounds(@Nullable View view) {
        if (view != null) {
            return view.getClipBounds();
        }
        return null;
    }

    protected void setTranscriptModeAlwaysScroll(RecordAdapter adapter) {
        // We'll need to reset the list view to its previous transcript mode,
        // but it has to happen *after* the keyboard is hidden, otherwise scroll will be reset
        // Let's wait for half a second, that's ugly but we don't have any other option :(
        final Handler handler = new Handler();
        handler.postDelayed(() -> adapter.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL), 500);
    }

    protected void setTranscriptModeDisabled(RecordAdapter adapter) {
        adapter.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
    }
}
