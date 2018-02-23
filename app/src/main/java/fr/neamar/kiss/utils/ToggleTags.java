package fr.neamar.kiss.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.ui.TriToggleButton;

/**
 * Created by TBog on 1/11/2018.
 */

public class ToggleTags
{
	private static final int STATE_DEFAULT = 0;
	private static final int STATE_HIDE    = 1;
	private static final int STATE_SHOW    = 2;

	private final View                  toggleBarView;
	private final ViewGroup             toggleContainer;
	private final Set<String>           hiddenTagList;
	private final Set<String>           mustShowTagList;
	private final Set<String>           togglableTagList;
	private final ToggleUpdatedListener toggleUpdatedListener;

	private boolean 					barNeedsRefresh = true;
	private ListPopup					popupMenu		= null;

	private final CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
		{
			// force the popupMenu to repopulate
			if ( popupMenu != null && !popupMenu.isShowing() )
				popupMenu = null;

			TriToggleButton button = (TriToggleButton)buttonView;
			String tag = button.getTag().toString();
			if( button.getState() == STATE_HIDE )
			{
				mustShowTagList.remove( tag );
				if( hiddenTagList.add( tag ) )
					toggleUpdatedListener.onToggleUpdated();
			}
			else if( button.getState() == STATE_SHOW )
			{
				hiddenTagList.remove( tag );
				if( mustShowTagList.add( tag ) )
					toggleUpdatedListener.onToggleUpdated();
			}
			else
			{
				boolean callListener = false;
				if ( hiddenTagList.remove( tag ) )
					callListener = true;
				if ( mustShowTagList.remove( tag ) )
					callListener = true;

				if( callListener )
					toggleUpdatedListener.onToggleUpdated();
			}
		}
	};

    public interface ToggleUpdatedListener
	{
		void onToggleUpdated();
		void showMatchingTags( @Nullable String tag );
		void showHistory();
	}

	public ToggleTags()
	{
		super();

		toggleBarView = null;
		toggleContainer = null;
		hiddenTagList = new TreeSet<>();
		mustShowTagList = new TreeSet<>();
		togglableTagList = new TreeSet<>();
		toggleUpdatedListener = null;
	}

	public ToggleTags( View toggleBar, ToggleUpdatedListener listener )
	{
		super();

		toggleBarView = toggleBar;
		toggleContainer = (ViewGroup)toggleBar.findViewById( R.id.tags_toggle_list );
		hiddenTagList = new TreeSet<>();
		mustShowTagList = new TreeSet<>();
		togglableTagList = new TreeSet<>();
		toggleUpdatedListener = listener;
	}

	public void saveHiddenTags( SharedPreferences prefs )
	{
		prefs.edit()
			 .putStringSet( "hidden-tags-list", hiddenTagList )
			 .apply();
	}

	public void loadTags( SharedPreferences prefs, Context context )
	{
		setHiddenTags( getHiddenTags( prefs ) );
		setTogglableTags( getTogglableTags( prefs ) );
		if( togglableTagList.isEmpty() )
		{
            TagsHandler tagsHandler = KissApplication.getApplication(toggleBarView.getContext()).getDataHandler().getTagsHandler();
			Set<String> list = tagsHandler.getAllTagsAsSet();
			for( String tag : list )
			{
				togglableTagList.add( tag );
				if( togglableTagList.size() >= 5 )
					break;
			}
		}
		barNeedsRefresh = true;
	}

	private void updateBarContent()
	{
		// make sure we don't have hidden tags that we can't toggle
		for( Iterator<String> iterator = hiddenTagList.iterator(); iterator.hasNext(); )
		{
			String tag = iterator.next();
			if( !togglableTagList.contains( tag ) )
				iterator.remove();
		}

		// make sure we don't have "must show" tags that we can't toggle
		for( Iterator<String> iterator = mustShowTagList.iterator(); iterator.hasNext(); )
		{
			String tag = iterator.next();
			if( !togglableTagList.contains( tag ) )
				iterator.remove();
		}

		if( toggleContainer == null )
			return;
		toggleContainer.removeAllViews();
		for( String tag : togglableTagList )
		{
			TriToggleButton button = (TriToggleButton)LayoutInflater.from( toggleContainer.getContext() )
																 .inflate( R.layout.tags_toggle_item, toggleContainer, false );
			//button.setTextForState( STATE_HIDE, "\u2013" + tag ); // U+2013 en dash
			button.setTextForState( STATE_HIDE, "\u2796" + tag ); // Unicode Character 'HEAVY MINUS SIGN' (U+2796)
			button.setTextForState( STATE_DEFAULT, tag );
			button.setTextForState( STATE_SHOW, "\u2795" + tag ); // Unicode Character 'HEAVY PLUS SIGN' (U+2795)
			button.setState( hiddenTagList.contains( tag ) ? STATE_HIDE : (mustShowTagList.contains( tag ) ? STATE_SHOW : STATE_DEFAULT) );
			button.setTag( tag );
			button.setOnCheckedChangeListener( checkedChangeListener );

			toggleContainer.addView( button );
		}
		
		barNeedsRefresh = false;
	}

	public void showBar( SharedPreferences prefs )
	{
		if ( popupMenu != null && popupMenu.isShowing() ) {
			// the popup menu has priority
			return;
		}
		if ( prefs.getBoolean( "pref-toggle-tags", false ) ) {
			if ( barNeedsRefresh )
				updateBarContent();
			toggleBarView.setVisibility(View.VISIBLE);
		}
	}

	public void hideBar()
	{
		toggleBarView.setVisibility( View.GONE );
	}

	private void setHiddenTags( Set<String> list )
	{
		hiddenTagList.clear();
		if( list != null )
			hiddenTagList.addAll( list );
	}

	private void setTogglableTags( Set<String> list )
	{
		togglableTagList.clear();
		if( list != null )
			togglableTagList.addAll( list );
	}

	public Set<String> getHiddenTags()
	{
		return hiddenTagList;
	}

	public Set<String> getMustShowTags()
	{
		return mustShowTagList;
	}

	public Set<String> getTogglableTags()
	{
		return togglableTagList;
	}

	static Set<String> getTogglableTags( SharedPreferences prefs )
	{
		return prefs.getStringSet( "pref-toggle-tags-list", null );
	}

	static Set<String> getHiddenTags( SharedPreferences prefs )
	{
		return prefs.getStringSet( "hidden-tags-list", null );
	}

	interface MenuItem {
    	@LayoutRes int getLayoutResource();
	}

	static class MenuItemDivider implements MenuItem {
		@Override
		public int getLayoutResource() {
			return R.layout.popup_divider;
		}
	}

	static class MenuItemTitle implements MenuItem {
		final String name;

		MenuItemTitle(Context context, @StringRes int nameRes) {
			this.name = context.getString(nameRes);
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public int getLayoutResource() {
			return R.layout.popup_title;
		}
	}

	static class MenuItemTag implements MenuItem
	{
		final String tag;
		final boolean showButton;

		MenuItemTag(String tag, boolean showButton) {
			this.tag = tag;
			this.showButton = showButton;
		}

		@Override
		public String toString() {
			return tag;
		}

		@Override
		public int getLayoutResource() {
			return showButton ? R.layout.popup_tag_menu : R.layout.popup_list_item;
		}
	}

	static class MenuItemBtn implements MenuItem
	{
		final int nameRes;
		final String name;

		MenuItemBtn(Context context, @StringRes int nameRes) {
			this.nameRes = nameRes;
			this.name = context.getString(nameRes);
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public int getLayoutResource() {
			return R.layout.popup_list_item;
		}
	}

	class MenuAdapter extends BaseAdapter
	{
		final ArrayList<MenuItem> list = new ArrayList<>(0);

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public MenuItem getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("ViewHolder")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MenuItem item = getItem(position);
			String text = item.toString();
			convertView = LayoutInflater.from(parent.getContext()).inflate(item.getLayoutResource(), parent, false);
			if (item instanceof MenuItemDivider) {
				return convertView;
			} else if (item instanceof MenuItemTag) {
				TriToggleButton btn = (TriToggleButton) convertView.findViewById(R.id.toggle);
				if (btn != null) {
					btn.setTextForState(STATE_HIDE, "\u2718"); // ✘	heavy ballot x
					btn.setTextForState(STATE_DEFAULT, "\u2325"); // ⌥	option key
					btn.setTextForState(STATE_SHOW, "\u2714"); // ✔	heavy check mark
					btn.setState(hiddenTagList.contains(text) ? STATE_HIDE : (mustShowTagList.contains(text) ? STATE_SHOW : STATE_DEFAULT));
					btn.setTag(text);
					btn.setOnCheckedChangeListener(checkedChangeListener);
				}
			}

			TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
			textView.setText(text);

			return convertView;
		}

		public void add(MenuItem item) {
			list.add(item);
			notifyDataSetChanged();
		}

		public void add(int index, MenuItem item) {
			list.add(index, item);
			notifyDataSetChanged();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			MenuItem item = list.get(position);
			return (item instanceof MenuItemTag) || (item instanceof MenuItemBtn);
		}
	}

	public ListPopup showMenu(final View anchor ) {
    	if ( popupMenu != null ) {
			popupMenu.show(anchor);
			return popupMenu;
		}

    	Context context = anchor.getContext();
		popupMenu = new ListPopup(context);
		MenuAdapter adapter = new MenuAdapter();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		//build menu
		boolean triStateBtn = prefs.getBoolean("pref-tags-menu-3state", true);
		adapter.add(new MenuItemTitle(context, R.string.popup_tags_title));
		for ( String tag : togglableTagList )
		{
			adapter.add(new MenuItemTag(tag, triStateBtn));
		}

		// remember where the title should go
		int actionsTitlePosition = adapter.getCount();
		if ( triStateBtn )
			adapter.add(new MenuItemBtn(context, R.string.show_matching));
		if ( !prefs.getBoolean("history-onclick", false) )
			adapter.add(new MenuItemBtn(context, R.string.show_history));
		// insert title only if at least an action was added
		if ( actionsTitlePosition != adapter.getCount() )
			adapter.add(actionsTitlePosition, new MenuItemTitle(context, R.string.popup_tags_actions));

		adapter.add(new MenuItemDivider());
		adapter.add(new MenuItemBtn(context, R.string.ctx_menu));

		// set popup interaction rules
		popupMenu.setAdapter(adapter);
		popupMenu.setDismissOnItemClick(false);
		popupMenu.setOnItemClickListener(new ListPopup.OnItemClickListener() {
			@Override
			public void onItemClick(ListAdapter adapter, View view, int position) {
				Object adapterItem = adapter.getItem(position);
				if ( adapterItem instanceof MenuItemTag )
				{
					MenuItemTag item = (MenuItemTag) adapterItem;
					// show only apps that match this tag
					toggleUpdatedListener.showMatchingTags( item.tag );
				}
				else if ( adapterItem instanceof MenuItemBtn )
				{
					switch (((MenuItemBtn) adapterItem).nameRes)
					{
						case R.string.ctx_menu:
							if ( popupMenu != null )
								popupMenu.dismiss();
							popupMenu = null;
							anchor.showContextMenu();
							break;
						case R.string.show_history:
							toggleUpdatedListener.showHistory();
							break;
						case R.string.show_matching:
							// show all matching
							toggleUpdatedListener.showMatchingTags( null );
							break;
					}
				}
			}
		});

		// 1. it's difficult to sync while both are visible
		// 2. there is no need to have both visible
		hideBar();

		popupMenu.show(anchor);
		return popupMenu;
	}
}
