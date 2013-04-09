package fr.neamar.summon;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import fr.neamar.summon.adapter.RecordAdapter;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.task.UpdateRecords;

public class SummonActivity extends ListActivity implements QueryInterface {

	public static String START_LOAD = "fr.neamar.summon.START_LOAD";
	public static String LOAD_OVER = "fr.neamar.summon.LOAD_OVER";
	public static String FULL_LOAD_OVER = "fr.neamar.summon.FULL_LOAD_OVER";
	public static String NB_PROVIDERS = "nb_providers";
	private BroadcastReceiver mReceiver;

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

	private MenuItem clear;

	/**
	 * Store user preferences
	 */
	SharedPreferences prefs;

	/** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Initialize UI
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("themeDark", false)) {
			setTheme(R.style.SummonThemeDark);
		}
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);

		IntentFilter intentFilter = new IntentFilter(START_LOAD);
		IntentFilter intentFilterBis = new IntentFilter(LOAD_OVER);
		IntentFilter intentFilterTer = new IntentFilter(FULL_LOAD_OVER);
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equalsIgnoreCase(LOAD_OVER)) {
					// Invalidate menu for favorites generation
					invalidateOptionsMenu();

					updateRecords(searchEditText.getText().toString());
				} else if (intent.getAction().equalsIgnoreCase(FULL_LOAD_OVER)) {
					// Invalidate menu for favorites generation
					invalidateOptionsMenu();
					setProgressBarIndeterminateVisibility(false);
				} else if (intent.getAction().equalsIgnoreCase(START_LOAD)) {
					setProgressBarIndeterminateVisibility(true);
				}
			}
		};
		this.registerReceiver(mReceiver, intentFilter);
		this.registerReceiver(mReceiver, intentFilterBis);
		this.registerReceiver(mReceiver, intentFilterTer);
		SummonApplication.initDataHandler(this);

		// Initialize preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		setContentView(R.layout.main);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				&& prefs.getBoolean("small-screen", false)) {
			getActionBar().hide();
		}

		// Create adapter for records
		adapter = new RecordAdapter(this, this, R.layout.item_app, new ArrayList<Record>());
		setListAdapter(adapter);

		this.searchEditText = (EditText) findViewById(R.id.searchEditText);

		// Listen to changes
		searchEditText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {

			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateRecords(s.toString());
				if (clear != null) {
					if (!searchEditText.getText().toString().equalsIgnoreCase("")) {
						clear.setVisible(true);
					} else {
						clear.setVisible(false);
					}
				}
			}
		});

		// On validate, launch first record
		searchEditText.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				RecordAdapter adapter = ((RecordAdapter) getListView().getAdapter());

				adapter.onClick(adapter.getCount() - 1, v);

				return true;
			}
		});
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		adapter.onClick(position, v);
	}

	/**
	 * Empty text field on resume and show keyboard
	 */
	protected void onResume() {

		if (prefs.getBoolean("layout-updated", false)) {
			// Restart current activity to refresh view, since some preferences
			// might require using a new UI
			prefs.edit().putBoolean("layout-updated", false).commit();
			Intent i = getApplicationContext().getPackageManager().getLaunchIntentForPackage(
					getApplicationContext().getPackageName());
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(i);
		}

		if (clear != null) {
			if (searchEditText != null && !searchEditText.getText().toString().equalsIgnoreCase("")) {
				clear.setVisible(true);
			} else {
				clear.setVisible(false);
			}
		}

		IntentFilter intentFilter = new IntentFilter(START_LOAD);
		IntentFilter intentFilterBis = new IntentFilter(LOAD_OVER);
		IntentFilter intentFilterTer = new IntentFilter(FULL_LOAD_OVER);
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equalsIgnoreCase(LOAD_OVER)) {
					updateRecords(searchEditText.getText().toString());
				} else if (intent.getAction().equalsIgnoreCase(FULL_LOAD_OVER)) {
					setProgressBarIndeterminateVisibility(false);
				} else if (intent.getAction().equalsIgnoreCase(START_LOAD)) {
					setProgressBarIndeterminateVisibility(true);
				}
			}
		};
		getListView().setLongClickable(true);
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View arg1, int pos, long id) {
				((RecordAdapter) parent.getAdapter()).onLongClick(pos);
				return true;
			}
		});

		// registering our receiver
		this.registerReceiver(mReceiver, intentFilter);
		this.registerReceiver(mReceiver, intentFilterBis);
		this.registerReceiver(mReceiver, intentFilterTer);
		// Display keyboard
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				searchEditText.requestFocus();
				InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);

		updateRecords(searchEditText.getText().toString());

		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// unregister our receiver
		this.unregisterReceiver(this.mReceiver);
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
		// Handle favorites
		if (item.getItemId() < 5) {
			Holder holder = SummonApplication.getDataHandler(this).getFavorites(this)
					.get(item.getItemId());
			Record record = Record.fromHolder(this, holder);
			record.fastLaunch(this);
		}

		switch (item.getItemId()) {
		case R.id.favorites:
			// Populate option menu
			// Favorites button
			SubMenu favorites = item.getSubMenu();
			favorites.clear();
			ArrayList<Holder> favorites_holder = SummonApplication.getDataHandler(this)
					.getFavorites(this);
			for (int i = 0; i < favorites_holder.size(); i++) {
				Holder holder = favorites_holder.get(i);
				MenuItem favorite = favorites.add(Menu.NONE, i, i, holder.name);

				Record record = Record.fromHolder(this, holder);
				Drawable drawable = record.getDrawable(this);
				if (drawable != null)
					favorite.setIcon(drawable);
			}
			
			if(favorites_holder.size() == 0)
			{
				Toast toast = Toast.makeText(this, getString(R.string.menu_favorites_empty), Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.TOP, 0, 20);
				toast.show();
			}
			return true;
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

		// "Clear" button
		clear = menu.findItem(R.id.clear);
		if (clear != null) {
			if (searchEditText != null && !searchEditText.getText().toString().equalsIgnoreCase("")) {
				clear.setVisible(true);
			} else {
				clear.setVisible(false);
			}
		}
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
