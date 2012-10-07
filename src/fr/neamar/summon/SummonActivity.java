package fr.neamar.summon;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.record.RecordAdapter;

public class SummonActivity extends Activity {

	private static final int MENU_SETTINGS = Menu.FIRST;

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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Initialize UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getWindow().setBackgroundDrawable(
				getResources().getDrawable(R.drawable.background_holo_dark));

		listView = (ListView) findViewById(R.id.resultListView);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View arg1,
					int position, long id) {
				adapter.onClick(position);
			}
		});

		// Initialize datas
		dataHandler = new DataHandler(getApplicationContext());

		// Create adapter for records
		adapter = new RecordAdapter(getApplicationContext(), R.layout.item_app,
				new ArrayList<Record>());
		listView.setAdapter(adapter);

		// Listen to changes
		final EditText searchEditText = (EditText) findViewById(R.id.searchEditText);
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
		CountDownTimer t = new CountDownTimer(2000, 400) {

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

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				return true;
			case KeyEvent.KEYCODE_HOME:
				return true;
			}
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				return true;
			case KeyEvent.KEYCODE_HOME:
				return true;
			}
		}

		return super.dispatchKeyEvent(event);
	}

	/**
	 * Empty text field on resume
	 */
	protected void onResume() {
		EditText searchEditText = (EditText) findViewById(R.id.searchEditText);
		searchEditText.setText("");

		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setIntent(
						new Intent(android.provider.Settings.ACTION_SETTINGS));

		return true;
	}

	/**
	 * This function gets called on changes. It will ask all the providers for
	 * datas
	 * 
	 * @param s
	 */
	public void updateRecords(String query) {
		adapter.clear();

		// Ask for records
		ArrayList<Record> records = dataHandler.getRecords(query);
		for (int i = 0; i < Math.min(MAX_RECORDS, records.size()); i++) {
			adapter.add(records.get(i));
		}

		// Reset scrolling to top
		listView.setSelectionAfterHeaderView();
	}
}
