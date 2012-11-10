package fr.neamar.summon.lite;

import fr.neamar.summon.lite.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.edit().putBoolean("preferences-updated", true).commit();
	}
}
