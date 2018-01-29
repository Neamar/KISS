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

	protected class ScrollView extends android.widget.ScrollView
	{
		public ScrollView( Context context )
		{
			super( context );
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

	public ListPopup( Context context )
	{
		super( context, null, android.R.attr.popupMenuStyle );
		LinearLayout layout     = new LinearLayout( context );
		layout.setOrientation( LinearLayout.VERTICAL );
		ScrollView     scrollView = new ScrollView( context );
		scrollView.addView( layout );
		setContentView( scrollView );
		setWidth( LinearLayout.LayoutParams.WRAP_CONTENT );
		setHeight( LinearLayout.LayoutParams.WRAP_CONTENT );
		mItemClickListener = null;
		mClickListener = new View.OnClickListener()
		{
			@Override
			public void onClick( View v )
			{
				dismiss();
				if( mItemClickListener != null )
				{
					LinearLayout layout   = getLinearLayout();
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
		mSystemUiVisibilityHelper.addPopup();
	}

	@Override
	public void dismiss()
	{
		super.dismiss();
		mSystemUiVisibilityHelper.popPopup();
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

	private LinearLayout getLinearLayout()
	{
		return (LinearLayout)((ScrollView)getContentView()).getChildAt( 0 );
	}

	private void updateItems()
	{
		LinearLayout layout = getLinearLayout();
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
		final int distanceToTop = anchorPos[1] - displayFrame.top;

		LinearLayout linearLayout = getLinearLayout();

		linearLayout.measure( View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ),
				View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ) );

		int xOffset = anchorPos[0] + anchor.getPaddingLeft();
		if( xOffset + linearLayout.getMeasuredWidth() > displayFrame.right )
			xOffset = displayFrame.right - linearLayout.getMeasuredWidth();

		int halfAnchorHeight = anchor.getHeight() / 2;
		int yOffset;
		if( distanceToBottom > linearLayout.getMeasuredHeight() )
		{
			// show below anchor
			yOffset = anchorPos[1] + halfAnchorHeight;
			setAnimationStyle( R.style.PopupAnimationTop );
		}
		else if ( distanceToTop > distanceToBottom )
		{
			// show above anchor
			yOffset = anchorPos[1] + halfAnchorHeight - linearLayout.getMeasuredHeight();
			setAnimationStyle( R.style.PopupAnimationBottom );
			if ( distanceToTop < linearLayout.getMeasuredHeight() )
			{
				// enable scroll
				setHeight( distanceToTop + halfAnchorHeight );
				yOffset += linearLayout.getMeasuredHeight() - distanceToTop - halfAnchorHeight;
			}
		}
		else
		{
			// show below anchor with scroll
			yOffset = anchorPos[1] + halfAnchorHeight;
			setAnimationStyle( R.style.PopupAnimationTop );
			setHeight( distanceToBottom + halfAnchorHeight );
		}

		showAtLocation( anchor, Gravity.START | Gravity.TOP, xOffset, yOffset );
	}
}
