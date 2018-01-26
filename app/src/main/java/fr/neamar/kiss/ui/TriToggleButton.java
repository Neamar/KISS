package fr.neamar.kiss.ui;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import java.util.ArrayList;

/**
 * Created by TBog on 1/26/2018.
 */

public class TriToggleButton extends CompoundButton
{
	private int mStateCurrent = 0;
	private int mStateMax = 2;
	private final ArrayList<CharSequence> mTextForState = new ArrayList<>( 0 );

	public TriToggleButton( Context context )
	{
		this( context, null );
	}

	public TriToggleButton( Context context, AttributeSet attrs )
	{
		this( context, attrs, 0 );
	}

	public TriToggleButton( Context context, AttributeSet attrs, int defStyleAttr )
	{
		super( context, attrs, defStyleAttr );
	}

	public int getState()
	{
		return mStateCurrent;
	}

	public void setState( int newState )
	{
		if ( newState > mStateMax )
			newState = mStateMax;
		if ( mStateCurrent == newState )
			return;
		mStateCurrent = newState;
		syncTextState();
		// we toggle so the listeners get called
		super.toggle();
	}

	public void setStateCount( int stateCount )
	{
		int newStateMax = stateCount - 1;
		if ( newStateMax == mStateMax )
			return;
		mStateMax = newStateMax;
		//TODO: resize mTextForState and check if mStateCurrent is valid
	}

	private void syncTextState()
	{
		int state = getState();
		if( state < mTextForState.size() )
		{
			setText( mTextForState.get( state ) );
		}
	}

	@Override
	public void toggle()
	{
		if( mStateCurrent < mStateMax )
		{
			setState( mStateCurrent + 1 );
		}
		else
		{
			setState( 0 );
		}
	}

	public void setTextForState( int state, CharSequence tag )
	{
		mTextForState.ensureCapacity( mStateMax + 1 );
		while ( mTextForState.size() <= mStateMax )
			mTextForState.add( "" );
		mTextForState.set( state, tag );
		syncTextState();
	}

	public CharSequence getTextForState( int state )
	{
		return mTextForState.get( state );
	}

	static class SavedState extends BaseSavedState {
		int state;

		/**
		 * Constructor called from {@link CompoundButton#onSaveInstanceState()}
		 */
		SavedState(Parcelable superState) {
			super(superState);
		}

		/**
		 * Constructor called from {@link #CREATOR}
		 */
		private SavedState(Parcel in) {
			super(in);
			state = (Integer)in.readValue(null);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeValue(state);
		}

		@Override
		public String toString() {
			return "TriToggleButton.SavedState{"
				   + Integer.toHexString(System.identityHashCode(this))
				   + " state=" + state + "}";
		}

		public static final Parcelable.Creator<SavedState> CREATOR
				= new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		SavedState ss = new SavedState(superState);

		ss.state = getState();
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;

		super.onRestoreInstanceState(ss.getSuperState());
		setState(ss.state);
		requestLayout();
	}
}
