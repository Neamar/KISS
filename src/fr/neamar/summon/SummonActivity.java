package fr.neamar.summon;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import fr.neamar.summon.adapter.RecordAdapter;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.task.UpdateRecords;

public class SummonActivity extends ListActivity implements QueryInterface {

	/**
	 * Adapter to display records
	 */
	public RecordAdapter adapter;

	/**
	 * Search text in the view
	 */
	private EditText searchEditText;

	/**
	 * Task launched on text change
	 */
	private UpdateRecords updateRecords;

	/**
	 * Store user preferences
	 */
	SharedPreferences prefs;

	/** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Initialize UI
		super.onCreate(savedInstanceState);

		// Initialize preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (prefs.getBoolean("invert-ui", false))
			setContentView(R.layout.main_inverted);
		else
			setContentView(R.layout.main);
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
				prefs.getBoolean("small-screen", false)){
			getActionBar().hide();
		}
			

		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View arg1,
					int position, long id) {
				adapter.onClick(position, arg1);
			}
		});

		// Create adapter for records
		adapter = new RecordAdapter(this, this, R.layout.item_app,
				new ArrayList<Record>());
		setListAdapter(adapter);

		this.searchEditText = (EditText) findViewById(R.id.searchEditText);

		// Listen to changes
		searchEditText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				updateRecords(s.toString());
			}
		});

		// On validate, launch first record
		searchEditText.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				RecordAdapter adapter = ((RecordAdapter) getListView()
						.getAdapter());

				if (prefs.getBoolean("invert-ui", false))
					adapter.onClick(0, v);
				else
					adapter.onClick(adapter.getCount() - 1, v);

				return true;
			}
		});

		// Some providers take time to load. So, on startup, we rebuild results
		// every 400ms to avoid missing records
		CountDownTimer t = new CountDownTimer(3200, 400) {

			@Override
			public void onTick(long millisUntilFinished) {
				updateRecords(searchEditText.getText().toString());
			}

			@Override
			public void onFinish() {

			}
		};
		t.start();
	}

	/**
	 * Empty text field on resume and show keyboard
	 */
	protected void onResume() {
		if (prefs.getBoolean("preferences-updated", false)) {

			// Restart current activity to refresh view, since some preferences
			// might require using a new UI
			prefs.edit().putBoolean("preferences-updated", false).commit();
			Intent intent = getIntent();
			overridePendingTransition(0, 0);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			finish();
			overridePendingTransition(0, 0);
			startActivity(intent);
		}

		// Display keyboard
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				searchEditText.requestFocus();
				InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.showSoftInput(searchEditText,
						InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);

		super.onResume();
	}

	@Override
	public void onBackPressed() {
		Intent i = new Intent();
		i.setAction(Intent.ACTION_MAIN);
		i.addCategory(Intent.CATEGORY_HOME);
		PackageManager pm = this.getPackageManager();
		ResolveInfo ri = pm.resolveActivity(i, 0);
		ActivityInfo ai = ri.activityInfo;
		searchEditText.setText("");
		if (!ai.packageName.equalsIgnoreCase(this.getPackageName())) {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
			return true;
		case R.id.preferences:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.clear:
			searchEditText.setText("");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_settings, menu);
		return true;
	}

	/**
	 * This function gets called on changes. It will ask all the providers for
	 * datas
	 * 
	 * @param s
	 */
	public void updateRecords(String query) {
		if (updateRecords != null) {
			updateRecords.cancel(true);
		}
		updateRecords = new UpdateRecords(this);
		updateRecords.execute(query);
	}

	public void resetTask() {
		updateRecords = null;
	}

	/**
	 * Call this function when we're leaving the activity We can't use
	 * onPause(), since it may be called for a configuration change
	 */
	public void launchOccured() {
		// We made a choice on the list,
		// now we can cleanup the filter:
		searchEditText.setText("");
	}
}
