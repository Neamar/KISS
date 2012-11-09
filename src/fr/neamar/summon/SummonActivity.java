package fr.neamar.summon;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.record.RecordAdapter;

public class SummonActivity extends Activity {

	private static final int MENU_SETTINGS = Menu.FIRST;
	private static final int MENU_PREFERENCES = MENU_SETTINGS + 1;

	private final int MAX_RECORDS = 15;

	/**
	 * Adapter to display records
	 */
	private RecordAdapter adapter;

	/**
	 * Object handling all data queries
	 */
	private DataHandler dataHandler;

	/**
	 * List view displaying records
	 */
	private ListView listView;

	/**
	 * Store current query
	 */
	private String currentQuery;

	/**
	 * Set to true if activity was just rebuilt because of configuration
	 * changes. Allows not to empty textfield during onResume().
	 */
	private Boolean flagConfigurationChanged = false;

	/**
	 * Search text in the view
	 */
	private EditText searchEditText;

	/**
	 * Store user preferences
	 */
	SharedPreferences prefs;

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Initialize UI
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Initialize preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (prefs.getBoolean("invert-ui", false))
			setContentView(R.layout.main_inverted);
		else
			setContentView(R.layout.main);

		listView = (ListView) findViewById(R.id.resultListView);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View arg1,
					int position, long id) {
				adapter.onClick(position, arg1);
			}
		});

		// (re-)Initialize datas
		dataHandler = (DataHandler) getLastNonConfigurationInstance();
		if (dataHandler != null) {
			flagConfigurationChanged = true;
		} else {
			dataHandler = new DataHandler(getApplicationContext());
		}

		// Create adapter for records
		adapter = new RecordAdapter(getApplicationContext(), R.layout.item_app,
				new ArrayList<Record>());
		listView.setAdapter(adapter);

		// Listen to changes
		this.searchEditText = (EditText) findViewById(R.id.searchEditText);
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

		// Reset textfield (will display history)
		if (!flagConfigurationChanged)
			searchEditText.setText("");
		else
			flagConfigurationChanged = false; // Reset flag

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
	public Object onRetainNonConfigurationInstance() {
		return dataHandler;
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
        if(!ai.packageName.equalsIgnoreCase(this.getPackageName())){
        	super.onBackPressed();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setIntent(
						new Intent(android.provider.Settings.ACTION_SETTINGS));

		menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences).setIntent(
				new Intent(this, SettingsActivity.class));

		return true;
	}

	/**
	 * This function gets called on changes. It will ask all the providers for
	 * datas
	 * 
	 * @param s
	 */
	public void updateRecords(String query) {
		currentQuery = query;
		Thread resultThread = new Thread(new Runnable() {

			@Override
			public void run() {
				String workingOnQuery = currentQuery;

				// Ask for records
				final ArrayList<Holder> holders = dataHandler
						.getResults(workingOnQuery);

				// Another search have already been made
				if (workingOnQuery != currentQuery)
					return;

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						adapter.clear();

						if (holders == null) {
							// First use of the app. TODO : Display something
							// useful.
						} else {
							if (prefs.getBoolean("invert-ui", false)) {
								for (int i = 0; i < Math.min(MAX_RECORDS,
										holders.size()); i++) {
									adapter.add(Record.fromHolder(holders
											.get(i)));
								}
							} else {
								for (int i = Math.min(MAX_RECORDS,
										holders.size()) - 1; i >= 0; i--) {
									adapter.add(Record.fromHolder(holders
											.get(i)));
								}
							}
							// Reset scrolling to top
							listView.setSelectionAfterHeaderView();
						}
					}
				});

			}
		});
		resultThread.start();
	}
}
