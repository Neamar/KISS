package fr.neamar.kiss.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.HashMap;

/**
 * Created by TBog on 11/16/2017.
 */

public class AnimatedListView extends BlockableListView
{
	final static int MOVE_DURATION = 100;

	private final HashMap<Long, Integer> mItemMap = new HashMap<>();

	public AnimatedListView( Context context )
	{
		super( context );
	}

	public AnimatedListView( Context context, AttributeSet attrs )
	{
		super( context, attrs );
	}

	public AnimatedListView( Context context, AttributeSet attrs, int defStyleAttr )
	{
		super( context, attrs, defStyleAttr );
	}

	public void prepareChangeAnim()
	{
		mItemMap.clear();

		// store positions before the update
		int firstVisiblePosition = this.getFirstVisiblePosition();
		int nCount = Math.min( this.getChildCount(), getAdapter().getCount() - firstVisiblePosition );
		for( int i = 0; i < nCount; i += 1 )
		{
			View child    = this.getChildAt( i );
			int  position = firstVisiblePosition + i;
			long itemId   = getAdapter().getItemId( position );
			mItemMap.put( itemId, child.getTop() );
		}

		blockTouchEvents();
	}

	public void animateChange()
	{
		if ( mItemMap.isEmpty() )
			return;
		
		final ViewTreeObserver observer = this.getViewTreeObserver();
		if ( !observer.isAlive() )
			return;

		// postpone animation to after the layout is computed
		observer.addOnPreDrawListener( new ViewTreeObserver.OnPreDrawListener()
		{
			@Override
			public boolean onPreDraw()
			{
				if ( !observer.isAlive() )
					return true;
				observer.removeOnPreDrawListener( this );
				AnimatedListView listView = AnimatedListView.this;

				boolean animationRunning = false;

				// this is called after the layout is updated to the new list
				int firstVisiblePosition = listView.getFirstVisiblePosition();
				int nCount = Math.min( listView.getChildCount(), getAdapter().getCount() - firstVisiblePosition );
				for( int i = 0; i < nCount; i += 1 )
				{
					int     position        = firstVisiblePosition + i;
					long    itemId          = getAdapter().getItemId( position );
					Integer topBeforeLayout = mItemMap.get( itemId );
					View    child           = listView.getChildAt( i );
					int     topAfterLayout  = child.getTop();
					int     delta;
					if( topBeforeLayout != null )
					{
						// this view may have moved
						delta = topBeforeLayout - topAfterLayout;
					}
					else
					{
						// this is a new view
						if ( i == 0 )
						{
							delta = -child.getHeight() - listView.getDividerHeight();
						}
						else
						{
							delta = 0;

							child.setAlpha( 0.f );
							child.animate()
								 .setDuration( MOVE_DURATION )
								 .alpha( 1.f );
							animationRunning = true;
						}
					}
					if( delta != 0 )
					{
						child.setTranslationY( delta );
						child.animate()
							 .setDuration( MOVE_DURATION )
							 .translationY( 0 )
							 .setInterpolator( new AccelerateDecelerateInterpolator() );
						animationRunning = true;
					}
				}

				if ( animationRunning )
				{
					listView.postDelayed( new Runnable()
					{
						@Override
						public void run()
						{
							AnimatedListView.this.unblockTouchEvents();
						}
					}, MOVE_DURATION );
				}
				else
				{
					AnimatedListView.this.unblockTouchEvents();
				}

				return false;
			}
		} );
	}
}
