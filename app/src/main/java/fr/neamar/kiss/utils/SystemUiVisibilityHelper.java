package fr.neamar.kiss.utils;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import fr.neamar.kiss.MainActivity;

/**
 * Created by TBog on 12/4/2017.
 */

public class SystemUiVisibilityHelper implements View.OnSystemUiVisibilityChangeListener
{
	private final MainActivity      mMainActivity;
	private final Handler           mHandler;
	private       SharedPreferences prefs;
	private boolean                 mKeyboardVisible;
	private boolean                 mIsScrolling;

	// This is used to emulate SYSTEM_UI_FLAG_IMMERSIVE_STICKY
	private final Runnable autoApplySystemUiRunnable = new Runnable() {
		@Override
		public void run() {
			if ( !mKeyboardVisible )
				applySystemUi();
		}
	};

	public SystemUiVisibilityHelper( MainActivity activity )
	{
		mMainActivity = activity;
		mHandler = new Handler( Looper.getMainLooper() );
		prefs = PreferenceManager.getDefaultSharedPreferences( activity );
		View decorView = mMainActivity.getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener( this );
		mKeyboardVisible = false;
		mIsScrolling = false;
	}

	public void onWindowFocusChanged( boolean hasFocus )
	{
		if ( hasFocus )
		{
			if ( mIsScrolling )
				applyScrollSystemUi();
			else
				applySystemUi();
		}
	}

	public void onKeyboardVisibilityChanged( boolean isVisible )
	{
		mKeyboardVisible = isVisible;
		if ( isVisible )
		{
			mHandler.removeCallbacks( autoApplySystemUiRunnable );
		}
	}

	public void applySystemUi()
	{
		applySystemUi( isPreferenceFullscreen(), isPreferenceImmersive() );
	}

	public void applySystemUi( boolean fullscreen, boolean immersive )
	{
		int visibility = 0;
		if ( fullscreen || immersive )
		{
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
			{
				visibility = visibility
							 | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION; // hide nav bar
			}
			else
			{
				visibility = visibility
							 | View.SYSTEM_UI_FLAG_LOW_PROFILE
							 | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION; // hide nav bar
			}
		}
		if ( fullscreen )
		{
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN )
			{
				visibility = visibility
							 //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							 | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							 | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
			}
		}
		if ( immersive )
		{
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
			{
				visibility = visibility
							 | View.SYSTEM_UI_FLAG_IMMERSIVE;
			}
		}
		View decorView = mMainActivity.getWindow().getDecorView();
		decorView.setSystemUiVisibility( visibility );
		//decorView.setFitsSystemWindows( true );
	}

	public void applyScrollSystemUi()
	{
		mIsScrolling = true;
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
		{
			if ( isPreferenceImmersive() )
				applySystemUi( isPreferenceFullscreen(), true );
		}
	}

	public void resetScroll()
	{
		mIsScrolling = false;
		if ( !mKeyboardVisible )
			mHandler.post( autoApplySystemUiRunnable );
	}

	private boolean isPreferenceFullscreen()
	{
		return prefs.getBoolean("pref-fullscreen", false);
	}

	private boolean isPreferenceImmersive()
	{
		return prefs.getBoolean("pref-immersive", false);
	}

	@Override
	public void onSystemUiVisibilityChange( int visibility )
	{
		StringBuilder sb = new StringBuilder();
		sb.append( String.format( "onSystemUiVisibilityChange %x", visibility ) );

		if ( (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0 )
			sb.append( "\n SYSTEM_UI_FLAG_HIDE_NAVIGATION" );

		if ( (visibility & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) != 0 )
			sb.append( "\n SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN" );
		if ( (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0 )
			sb.append( "\n SYSTEM_UI_FLAG_FULLSCREEN" );

		if ( (visibility & View.SYSTEM_UI_FLAG_IMMERSIVE) != 0 )
			sb.append( "\n SYSTEM_UI_FLAG_IMMERSIVE" );
		if ( (visibility & View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0 )
			sb.append( "\n SYSTEM_UI_FLAG_IMMERSIVE_STICKY" );

		Log.d("TBog", sb.toString() );

		if ( (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0 )
		{
			applySystemUi();
		}

		if ( !mKeyboardVisible && !mIsScrolling && visibility == 0 )
		{
			mHandler.postDelayed( autoApplySystemUiRunnable, 1500 );
		}
	}

	public void copyVisibility( View contentView )
	{
		View decorView = mMainActivity.getWindow().getDecorView();
		int visibility = decorView.getSystemUiVisibility();
		contentView.setSystemUiVisibility( visibility );
	}
}
