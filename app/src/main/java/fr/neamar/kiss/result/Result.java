package fr.neamar.kiss.result;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UiTweaks;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.pojo.PhonePojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.pojo.SettingsPojo;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.pojo.TogglesPojo;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.ui.ListPopup;

public abstract class Result {
    /**
     * Current information pojo
     */
    Pojo pojo = null;

    public static Result fromPojo(QueryInterface parent, Pojo pojo) {
        if ( pojo instanceof PojoWithTags && parent.showRelevance() )
        {
            PojoWithTags tagsPojo = (PojoWithTags)pojo;
            int relevance = pojo.relevance - 1;
            if ( tagsPojo.displayTags != null && tagsPojo.displayTags.length() > 2 && "(".equals( tagsPojo.displayTags.substring( 0, 1 ) ) )
            {
                try
                {
                    relevance = NumberFormat.getIntegerInstance()
                                            .parse( tagsPojo.displayTags.substring( 1 ) )
                                            .intValue();
                } catch( Exception ignore )
                {}
            }
            if( relevance != pojo.relevance )
            {
                String tags = tagsPojo.getTags();
                if( tags == null || tags.isEmpty() )
                    tagsPojo.displayTags = "<small>(" + pojo.relevance + ")</small> ";
                else
                    tagsPojo.displayTags = "<small>(" + pojo.relevance + ")</small> " + tagsPojo.displayTags;
            }
        }

        if (pojo instanceof AppPojo)
            return new AppResult((AppPojo) pojo);
        else if (pojo instanceof ContactsPojo)
            return new ContactsResult(parent, (ContactsPojo) pojo);
        else if (pojo instanceof SearchPojo)
            return new SearchResult((SearchPojo) pojo);
        else if (pojo instanceof SettingsPojo)
            return new SettingsResult((SettingsPojo) pojo);
        else if (pojo instanceof TogglesPojo)
            return new TogglesResult((TogglesPojo) pojo);
        else if (pojo instanceof PhonePojo)
            return new PhoneResult((PhonePojo) pojo);
        else if (pojo instanceof ShortcutsPojo)
            return new ShortcutsResult((ShortcutsPojo) pojo);


        throw new RuntimeException("Unable to create a result from POJO");
    }

    static class AsyncSetImage extends AsyncTask<Void, Void, Drawable>
    {
        final private WeakReference<ImageView> imageViewWeakReference;
        final private WeakReference<Result>    appResultWeakReference;
        AsyncSetImage( ImageView image, Result result )
        {
            super();
            image.setTag( this );
            this.imageViewWeakReference = new WeakReference<>( image );
            this.appResultWeakReference = new WeakReference<>( result );
        }

        @Override
        protected Drawable doInBackground( Void... voids )
        {
            ImageView image = imageViewWeakReference.get();
            if ( isCancelled() || image == null || image.getTag() != this )
                return null;
            Result result = appResultWeakReference.get();
            if ( result == null )
                return null;
            return result.getDrawable( image.getContext() );
        }

        @Override
        protected void onPostExecute( Drawable drawable )
        {
            ImageView image = imageViewWeakReference.get();
            if ( isCancelled() || image == null || drawable == null )
                return;
            image.setImageDrawable( drawable );
            image.setTag( null );
        }
    }

    @Override
    public String toString()
    {
        if ( pojo != null )
            return pojo.getName();
        return super.toString();
    }

    /**
     * How to display this record ?
     *
     * @param context     android context
     * @param convertView a view to be recycled
     * @return a view to display as item
     */
    public abstract View display(Context context, int position, View convertView);

    /**
     * How to display the popup menu
     *
     * @return a PopupMenu object
     */
    public ListPopup getPopupMenu( final Context context, final RecordAdapter parent, View parentView) {
        ArrayAdapter<ListPopup.Item> adapter = new ArrayAdapter<>( context, R.layout.popup_list_item );
        ListPopup menu = buildPopupMenu(context, adapter, parent, parentView);

        menu.setOnItemClickListener(new ListPopup.OnItemClickListener() {
            @Override
            public void onItemClick( ListAdapter adapter, View view, int position )
            {
                @StringRes int stringId = ((ListPopup.Item)adapter.getItem( position )).stringId;
                popupMenuClickHandler( view.getContext(), parent, stringId );
            }
        });

        return menu;
    }

    /**
     * Default popup menu implementation, can be overridden by children class to display a more specific menu
     *
     * @return an inflated, listener-free PopupMenu
     */
    ListPopup buildPopupMenu( Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView ) {
        adapter.add( new ListPopup.Item( context, R.string.menu_remove ) );
        adapter.add( new ListPopup.Item( context, R.string.menu_favorites_add ) );
        adapter.add( new ListPopup.Item( context, R.string.menu_favorites_remove ) );
        return inflatePopupMenu(adapter, context );
    }

    protected ListPopup inflatePopupMenu( ArrayAdapter<ListPopup.Item> adapter, Context context ) {
        ListPopup menu = new ListPopup( context );
        menu.setAdapter( adapter );

        // If app already pinned, do not display the "add to favorite" option
        // otherwise don't show the "remove favorite button"
        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");
        if (favApps.contains(this.pojo.id + ";")) {
            for ( int i = 0; i < adapter.getCount(); i += 1 )
            {
                ListPopup.Item item = adapter.getItem( i );
                if( item.stringId == R.string.menu_favorites_add )
                    adapter.remove( item );
            }
        } else {
            for ( int i = 0; i < adapter.getCount(); i += 1 )
            {
                ListPopup.Item item = adapter.getItem( i );
                if( item.stringId == R.string.menu_favorites_remove )
                    adapter.remove( item );
            }
        }

        return menu;
    }

    /**
     * Handler for popup menu action.
     * Default implementation only handle remove from history action.
     *
     * @return Works in the same way as onOptionsItemSelected, return true if the action has been handled, false otherwise
     */
    Boolean popupMenuClickHandler( Context context, RecordAdapter parent, @StringRes int stringId ) {
        switch ( stringId ) {
            case R.string.menu_remove:
                removeItem(context, parent);
                return true;
            case R.string.menu_favorites_add:
                launchAddToFavorites(context, pojo);
                break;
            case R.string.menu_favorites_remove:
                launchRemoveFromFavorites(context, pojo);
                break;
        }

        //Update Search to reflect favorite add, if the "exclude favorites" option is active
        ((MainActivity) context).updateRecords();

        return false;
    }

    private void launchAddToFavorites(Context context, Pojo app) {
        String msg = context.getResources().getString(R.string.toast_favorites_added);
        KissApplication.getDataHandler(context).addToFavorites((MainActivity) context, app.id);
        Toast.makeText(context, String.format(msg, app.getName()), Toast.LENGTH_SHORT).show();
    }

    private void launchRemoveFromFavorites(Context context, Pojo app) {
        String msg = context.getResources().getString(R.string.toast_favorites_removed);
        KissApplication.getDataHandler(context).removeFromFavorites((MainActivity) context, app.id);
        Toast.makeText(context, String.format(msg, app.getName()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Remove the current result from the list
     *
     * @param context android context
     * @param parent  adapter on which to remove the item
     */
    private void removeItem(Context context, RecordAdapter parent) {
        Toast.makeText(context, R.string.removed_item, Toast.LENGTH_SHORT).show();
        parent.removeResult(this);
    }

    public final void launch(Context context, View v) {
        Log.i("log", "Launching " + pojo.id);

        recordLaunch(context);

        // Launch
        doLaunch(context, v);
    }

    /**
     * How to launch this record ? Most probably, will fire an intent. This
     * function must call recordLaunch()
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
        this.launch(context, v);
    }

    /**
     * Return the icon for this Result, or null if non existing.
     *
     * @param context android context
     */
    public Drawable getDrawable(Context context) {
        return null;
    }

    boolean isDrawableCached()
    {
        return false;
    }

    void setAsyncDrawable( ImageView view )
    {
        if ( view.getTag() instanceof AsyncSetImage )
        {
            ((AsyncSetImage)view.getTag()).cancel( true );
            view.setTag( null );
        }
        if( isDrawableCached() )
        {
            view.setImageDrawable(getDrawable(view.getContext()));
        }
        else
        {
            view.setTag( new AsyncSetImage( view, this ).execute() );
        }
    }

    /**
     * Helper function to get a view
     *
     * @param context android context
     * @param id      id to inflate
     * @return the view specified by the id
     */
    View inflateFromId(Context context, int id) {
        LayoutInflater vi = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return vi.inflate(id, null);
    }

    /**
     * Enrich text for display. Put text requiring highlighting between {}
     *
     * @param text to highlight
     * @return text displayable on a textview
     */
    static Spanned enrichText(String text, Context context) {
        return Html.fromHtml(text.replaceAll("\\{", "<font color=" + UiTweaks.getPrimaryColor(context) + ">").replaceAll("\\}", "</font>"));
    }

    /**
     * Put this item in application history
     *
     * @param context android context
     */
    void recordLaunch(Context context) {
        // Save in history
        KissApplication.getDataHandler(context).addToHistory(pojo.id);
    }

    public void deleteRecord(Context context) {
        DBHelper.removeFromHistory(context, pojo.id);
    }

    /*
     * Get fill color from theme
     *
     */
    public int getThemeFillColor(Context context) {
        int[] attrs = new int[]{R.attr.resultColor /* index 0 */};
        TypedArray ta = context.obtainStyledAttributes(attrs);
        int color = ta.getColor(0, Color.WHITE);
        ta.recycle();
        return color;
    }

    public long getUniqueId()
    {
        // we can consider hashCode unique enough in this context
        return this.pojo.id.hashCode();
    }
}
