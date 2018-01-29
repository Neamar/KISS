package fr.neamar.kiss.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by TBog on 12/5/2017.
 */

public class SearchEditText extends EditText
{
	private OnEditorActionListener mEditorListener;

	public SearchEditText( Context context )
	{
		super( context );
	}

	public SearchEditText( Context context, AttributeSet attrs )
	{
		super( context, attrs );
	}

	public SearchEditText( Context context, AttributeSet attrs, int defStyleAttr )
	{
		super( context, attrs, defStyleAttr );
	}

	@Override
	public void setOnEditorActionListener( OnEditorActionListener listener )
	{
		mEditorListener = listener;
		super.setOnEditorActionListener( listener );
	}

	@Override
	public boolean onKeyPreIme( int keyCode, KeyEvent event )
	{
		if( event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP )
			if( mEditorListener != null && mEditorListener.onEditorAction( this, android.R.id.closeButton, event ) )
				return true;
		return super.onKeyPreIme( keyCode, event );
	}
}
