package fr.neamar.summon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("themeDark", false)){
			setTheme(R.style.SummonThemeDark);
		}
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		//Reload the DataHandler since Providers preferences might have changed
		SummonApplication.resetDataHandler(this);
		
		if(key.equalsIgnoreCase("invert-ui") || key.equalsIgnoreCase("themeDark") || key.equalsIgnoreCase("small-screen")){
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			prefs.edit().putBoolean("layout-updated", true).commit();

			// Restart current activity to refresh view, since some preferences
			// might require using a new UI
			Intent intent = new Intent(this, getClass());
	        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
	        finish();
	        overridePendingTransition(0, 0);
	        startActivity(intent);
	        overridePendingTransition(0, 0);
		}
	}
}
