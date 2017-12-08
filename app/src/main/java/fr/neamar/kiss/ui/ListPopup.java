package fr.neamar.kiss.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;

import fr.neamar.kiss.R;
import fr.neamar.kiss.utils.SystemUiVisibilityHelper;

/**
 * Created by TBog on 12/6/2017.
 */
public class ListPopup extends PopupWindow
{
	private final View.OnClickListener     mClickListener;
	private       OnItemClickListener      mItemClickListener;
	private       DataSetObserver          mObserver;
	private       ListAdapter              mAdapter;
	private       SystemUiVisibilityHelper mSystemUiVisibilityHelper;

	public static class Item
	{
		@StringRes
		public final int    stringId;
		final        String string;

		public Item( Context context, @StringRes int stringId )
		{
			super();
			this.stringId = stringId;
			this.string = context.getResources()
								 .getString( stringId );
		}

		@Override
		public String toString()
		{
			return this.string;
		}
	}

	public interface OnItemClickListener
	{
		void onItemClick( ListAdapter adapter, View view, int position );
	}

	protected class LayoutVertical extends LinearLayout
	{
		public LayoutVertical( Context context )
		{
			super( context );
			setOrientation( VERTICAL );
		}

		@Override
		public boolean dispatchTouchEvent( MotionEvent event )
		{
			// act as a modal, if we click outside dismiss the popup
			final int x = (int) event.getX();
			final int y = (int) event.getY();
			if ((event.getAction() == MotionEvent.ACTION_DOWN)
				&& ((x < 0) || (x >= getWidth()) || (y < 0) || (y >= getHeight()))) {
				dismiss();
				return true;
			} else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
				dismiss();
				return true;
			}
			return super.dispatchTouchEvent( event );
		}


	}

	public ListPopup( Context context )
	{
		super( context, null, android.R.attr.popupMenuStyle );
		LayoutVertical layout = new LayoutVertical( context );
		setContentView( layout );
		mItemClickListener = null;
		mClickListener = new View.OnClickListener()
		{
			@Override
			public void onClick( View v )
			{
				dismiss();
				if( mItemClickListener != null )
				{
					LinearLayout layout   = (LinearLayout)getContentView();
					int          position = layout.indexOfChild( v );
					mItemClickListener.onItemClick( mAdapter, v, position );
				}
			}
		};
	}

	public void setOnItemClickListener( OnItemClickListener onItemClickListener )
	{
		mItemClickListener = onItemClickListener;
	}

	public void setVisibilityHelper( SystemUiVisibilityHelper systemUiVisibility )
	{
		mSystemUiVisibilityHelper = systemUiVisibility;
	}

	private class PopupDataSetObserver extends DataSetObserver
	{
		@Override
		public void onChanged()
		{
			if( isShowing() )
			{
				// Resize the popup to fit new content
				updateItems();
				update();
			}
		}

		@Override
		public void onInvalidated()
		{
			dismiss();
		}
	}

	/**
	 * Sets the adapter that provides the data and the views to represent the data
	 * in this popup window.
	 *
	 * @param adapter The adapter to use to create this window's content.
	 */
	public void setAdapter( ListAdapter adapter )
	{
		if( mObserver == null )
		{
			mObserver = new PopupDataSetObserver();
		}
		else if( mAdapter != null )
		{
			mAdapter.unregisterDataSetObserver( mObserver );
		}
		mAdapter = adapter;
		if( mAdapter != null )
		{
			adapter.registerDataSetObserver( mObserver );
		}
	}

	public ListAdapter getAdapter()
	{
		return mAdapter;
	}

	private void updateItems()
	{
		LinearLayout layout = (LinearLayout)getContentView();
		layout.removeAllViews();
		int adapterCount = mAdapter.getCount();
		for( int i = 0; i < adapterCount; i += 1 )
		{
			View view = mAdapter.getView( i, null, layout );
			layout.addView( view );
			view.setOnClickListener( mClickListener );
		}
	}

	public void show( View anchor )
	{
		updateItems();

		if( mSystemUiVisibilityHelper != null )
			mSystemUiVisibilityHelper.copyVisibility( getContentView() );

		// don't steal the focus, this will prevent the keyboard from changing
		setFocusable( false );
		// draw over stuff if needed
		setClippingEnabled( false );

		final Rect displayFrame = new Rect();
		anchor.getWindowVisibleDisplayFrame( displayFrame );

		final int[] anchorPos = new int[2];
		anchor.getLocationOnScreen( anchorPos );

		final int distanceToBottom = displayFrame.bottom - (anchorPos[1] + anchor.getHeight());

		getContentView().measure( View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ),
				View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ) );

		int xOffset = anchorPos[0] + anchor.getPaddingLeft();
		if( xOffset + getContentView().getMeasuredWidth() > displayFrame.right )
			xOffset = displayFrame.right - getContentView().getMeasuredWidth();

		int yOffset;
		if( distanceToBottom > getContentView().getMeasuredHeight() )
		{
			// show below anchor
			yOffset = anchorPos[1] + anchor.getHeight() / 2;
			setAnimationStyle( R.style.PopupAnimationTop );
		}
		else
		{
			// show above anchor
			yOffset = anchorPos[1] + anchor.getHeight() / 2 - getContentView().getMeasuredHeight();
			setAnimationStyle( R.style.PopupAnimationBottom );
		}

		showAtLocation( anchor, Gravity.START | Gravity.TOP, xOffset, yOffset );
	}
}
