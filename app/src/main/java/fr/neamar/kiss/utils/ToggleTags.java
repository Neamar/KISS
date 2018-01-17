package fr.neamar.kiss.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.TagsHandler;

/**
 * Created by TBog on 1/11/2018.
 */

public class ToggleTags
{
	private final View                  toggleBarView;
	private final ViewGroup             toggleContainer;
	private final Set<String>           hiddenTagList;
	private final Set<String>           togglableTagList;
	private final ToggleUpdatedListener toggleUpdatedListener;
	private final CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
		{
			String tag = ((ToggleButton)buttonView).getTextOff()
												   .toString();
			if( isChecked )
			{
				// add the tag to the hidden list only once
				if( !hiddenTagList.contains( tag ) )
				{
					hiddenTagList.add( tag );
					toggleUpdatedListener.onToggleUpdated();
				}
			}
			else
			{
				hiddenTagList.remove( tag );
				toggleUpdatedListener.onToggleUpdated();
			}
		}
	};

	public interface ToggleUpdatedListener
	{
		void onToggleUpdated();
	}

	public ToggleTags()
	{
		super();

		toggleBarView = null;
		toggleContainer = null;
		hiddenTagList = new TreeSet<>();
		togglableTagList = new TreeSet<>();
		toggleUpdatedListener = null;
	}

	public ToggleTags( View toggleBar, ToggleUpdatedListener listener )
	{
		super();

		toggleBarView = toggleBar;
		toggleContainer = (ViewGroup)toggleBar.findViewById( R.id.tags_toggle_list );
		hiddenTagList = new TreeSet<>();
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
			TagsHandler tagsHandler = KissApplication.getDataHandler( context )
													 .getTagsHandler();
			Set<String> list = tagsHandler.getAllTagsAsSet();
			for( String tag : list )
			{
				togglableTagList.add( tag );
				if( togglableTagList.size() >= 5 )
					break;
			}
		}
		refreshContainer();
	}

	private void refreshContainer()
	{
		// make sure we don't have hidden tags that we can't toggle
		for( Iterator<String> iterator = hiddenTagList.iterator(); iterator.hasNext(); )
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
			ToggleButton button = (ToggleButton)LayoutInflater.from( toggleContainer.getContext() )
															  .inflate( R.layout.tags_toggle_item, toggleContainer, false );
			button.setTextOn( "\u2013" + tag ); // U+2013 en dash
			button.setTextOff( tag );
			button.setChecked( hiddenTagList.contains( tag ) );
			button.setOnCheckedChangeListener( checkedChangeListener );

			toggleContainer.addView( button );
		}
	}

	public void showBar( SharedPreferences prefs )
	{
		if ( prefs.getBoolean( "pref-toggle-tags", false ) )
			toggleBarView.setVisibility( View.VISIBLE );
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
}
