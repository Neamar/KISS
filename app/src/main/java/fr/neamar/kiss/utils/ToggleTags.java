package fr.neamar.kiss.utils;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	private final ArrayList<String>     hiddenTagList;
	private final ArrayList<String>     togglableTagList;
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

	public ToggleTags( View toggleBar, ToggleUpdatedListener listener )
	{
		super();

		toggleBarView = toggleBar;
		toggleContainer = (ViewGroup)toggleBar.findViewById( R.id.tags_toggle_list );
		hiddenTagList = new ArrayList<>( 5 );
		togglableTagList = new ArrayList<>( 5 );
		toggleUpdatedListener = listener;
	}

	public void saveHiddenTags( SharedPreferences prefs )
	{
		prefs.edit()
			 .putString( "hidden-tags-list", TextUtils.join( " ", hiddenTagList ) )
			 .apply();
	}

	public void saveTogglableTags( SharedPreferences prefs )
	{
		prefs.edit()
			 .putString( "togglable-tags-list", TextUtils.join( " ", togglableTagList ) )
			 .apply();
	}

	public void loadTags( SharedPreferences prefs )
	{
		setHiddenTags( TextUtils.split( prefs.getString( "hidden-tags-list", "" ), " " ) );
		setTogglableTags( TextUtils.split( prefs.getString( "togglable-tags-list", "" ), " " ) );
		if( togglableTagList.isEmpty() )
		{
			TagsHandler tagsHandler = KissApplication.getDataHandler( toggleBarView.getContext() )
													 .getTagsHandler();
			List<String> list = tagsHandler.getAllTagsAsList();
			try
			{
				togglableTagList.addAll( list.subList( 0, 5 ) );
			} catch( IndexOutOfBoundsException ignored )
			{
				togglableTagList.addAll( list );
			}
		}
		refreshContainer();
	}

	private void refreshContainer()
	{
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

	public void showBar()
	{
		toggleBarView.setVisibility( View.VISIBLE );
	}

	public void hideBar()
	{
		toggleBarView.setVisibility( View.GONE );
	}

	void setHiddenTags( String[] list )
	{
		hiddenTagList.clear();
		Collections.addAll( hiddenTagList, list );
	}

	void setTogglableTags( String[] list )
	{
		togglableTagList.clear();
		Collections.addAll( togglableTagList, list );
	}

	public ArrayList<String> getHiddenTags()
	{
		return hiddenTagList;
	}
}
