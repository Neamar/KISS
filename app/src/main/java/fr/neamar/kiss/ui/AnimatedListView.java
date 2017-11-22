package fr.neamar.kiss.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;

import java.util.HashMap;

/**
 * Created by TBog on 11/16/2017.
 */

public class AnimatedListView extends BlockableListView implements AbsListView.RecyclerListener
{
	final static int MOVE_DURATION = 100;

	@Override
	public void onMovedToScrapHeap( View view )
	{
		//Log.d( "TBog", "onMovedToScrapHeap " + view.toString() );
	}

	static class ItemInfo
	{
		final int top;
		final int viewIndex;
		boolean validated;

		public ItemInfo( int index, int top )
		{
			this.viewIndex = index;
			this.top = top;
			this.validated = false;
		}
	}

	private final HashMap<Long, ItemInfo> mItemMap = new HashMap<>();

	public AnimatedListView( Context context )
	{
		super( context );
		Init();
	}

	public AnimatedListView( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		Init();
	}

	public AnimatedListView( Context context, AttributeSet attrs, int defStyleAttr )
	{
		super( context, attrs, defStyleAttr );
		Init();
	}

	void Init()
	{
		setRecyclerListener( this );
	}

	public void prepareChangeAnim()
	{
		mItemMap.clear();

		//Log.d( "TBog", "prepareChangeAnim" );

		// store positions before the update
		int firstVisiblePosition = this.getFirstVisiblePosition();
		int nCount               = Math.min( this.getChildCount(), getAdapter().getCount() - firstVisiblePosition );
		for( int i = 0; i < nCount; i += 1 )
		{
			View child    = this.getChildAt( i );
			child.clearAnimation();
			int  position = firstVisiblePosition + i;
			long itemId   = getAdapter().getItemId( position );
			mItemMap.put( itemId, new ItemInfo( i, child.getTop() ) );
//			Log.d( "TBog", "view #" + i + " pos: " + position + " key:" + itemId + " name: " + getAdapter().getItem( position )
//																										   .toString() + " view: " +
//						   child.toString() );
		}

		//blockTouchEvents();
	}

	public void animateChange()
	{
		//Log.d( "TBog", "animateChange" );

		if( mItemMap.isEmpty() )
			return;

		// check if we can use the ViewTreeObserver for animations
		final ViewTreeObserver observer = this.getViewTreeObserver();
		if( !observer.isAlive() )
			return;

//		// before layout, validate views that may change location
//		AnimatedListView listView             = AnimatedListView.this;
//		int              firstVisiblePosition = listView.getFirstVisiblePosition();
//		int              nCount               = Math.min( listView.getChildCount(), getAdapter().getCount() - firstVisiblePosition );
//		for( int i = 0; i < nCount; i += 1 )
//		{
//			int  position = firstVisiblePosition + i;
//			long itemId   = getAdapter().getItemId( position );
//			Log.d( "TBog", "view #" + i + " pos: " + position + " key:" + itemId + " name: " + getAdapter().getItem( position )
//																										   .toString() + " view: " +
//						   listView.getChildAt( i )
//								   .toString() );
//			ItemInfo info = mItemMap.get( itemId );
//			if( info != null )
//				info.validated = true;
//		}
//
//		// all items not validated are to be removed
//		for( Map.Entry<Long, ItemInfo> entry : mItemMap.entrySet() )
//		{
//			ItemInfo info = entry.getValue();
//			if( !info.validated )
//			{
//				final View child = listView.getChildAt( info.viewIndex );
//				Log.d( "TBog", "view #" + info.viewIndex + " key:" + entry.getKey() + " not validated; view: " + child.toString() );
//				child.setTranslationX( 0.f );
//				child.setRotation( 0.f );
//				child.animate()
//					 .setDuration( MOVE_DURATION )
//					 .translationX( 100.f )
//					 .rotation( 180.f );
//			}
//		}

		// postpone animation to after the layout is computed and views are rebound
		observer.addOnPreDrawListener( new ViewTreeObserver.OnPreDrawListener()
		{
			@Override
			public boolean onPreDraw()
			{
				if( !observer.isAlive() )
					return true;
				observer.removeOnPreDrawListener( this );
				AnimatedListView listView = AnimatedListView.this;

				boolean animationRunning = false;

				// this is called after the layout is updated to the new list
				int firstVisiblePosition = listView.getFirstVisiblePosition();
				int nCount               = Math.min( listView.getChildCount(), getAdapter().getCount() - firstVisiblePosition );
				for( int i = 0; i < nCount; i += 1 )
				{
					int  position       = firstVisiblePosition + i;
					long itemId         = getAdapter().getItemId( position );
					View child          = listView.getChildAt( i );
					int  topAfterLayout = child.getTop();
					int  delta;
					if( mItemMap.containsKey( itemId ) )
					{
						int topBeforeLayout = mItemMap.get( itemId ).top;
						// this view may have moved
						delta = topBeforeLayout - topAfterLayout;
					}
					else
					{
						// this is a new view
						if( i == 0 )
						{
							// the first visible position can slide from the top
							delta = -child.getHeight() - listView.getDividerHeight();
						}
						else
						{
							delta = 0;

							// animate new views
							child.setScaleY( 0.f );
							child.animate()
								 .setDuration( MOVE_DURATION )
								 .scaleY( 1.f );
							animationRunning = true;
						}
					}
					if( delta != 0 )
					{
						child.setTranslationY( delta );
						child.animate()
							 .setDuration( MOVE_DURATION )
							 .translationY( 0 );
						animationRunning = true;
					}
				}

				if( animationRunning )
				{
					//Log.d("TBog", "Animation finished");
//					listView.postDelayed( new Runnable()
//					{
//						@Override
//						public void run()
//						{
//							AnimatedListView.this.unblockTouchEvents();
//						}
//					}, MOVE_DURATION );
				}
				else
				{
//					AnimatedListView.this.unblockTouchEvents();
				}

				return false;
			}
		} );
	}
}
