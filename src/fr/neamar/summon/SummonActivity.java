package fr.neamar.summon;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import fr.neamar.summon.dataprovider.AppProvider;
import fr.neamar.summon.dataprovider.ContactProvider;
import fr.neamar.summon.dataprovider.Provider;
import fr.neamar.summon.dataprovider.SearchProvider;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.record.RecordAdapter;
import fr.neamar.summon.record.RecordComparator;

public class SummonActivity extends Activity {

	private static final int MENU_SETTINGS = Menu.FIRST;

	/**
	 * Adapter to display records
	 */
	private RecordAdapter adapter;

	/**
	 * Pointer to current activity
	 */
	private SummonActivity summonActivity = this;

	/**
	 * List all knowns providers
	 */
	private ArrayList<Provider> providers = new ArrayList<Provider>();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Initialize UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ListView listView = (ListView) findViewById(R.id.resultListView);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View arg1,
					int position, long id) {
				adapter.onClick(position);
			}
		});

		// Initialize providers
		providers.add(new AppProvider(getApplicationContext()));
		providers.add(new ContactProvider(getApplicationContext()));
		providers.add(new SearchProvider(getApplicationContext()));

		// Create adapter for records
		adapter = new RecordAdapter(getApplicationContext(), R.layout.item_app,
				new ArrayList<Record>());
		listView.setAdapter(adapter);

		// Listen to changes
		EditText searchEditText = (EditText) findViewById(R.id.searchEditText);
		searchEditText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				summonActivity.updateRecords(s.toString());
			}
		});
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

		if (query.isEmpty()) {
			// Searching for nothing...
			return;
		}

		// Ask all providers for datas
		ArrayList<Record> allRecords = new ArrayList<Record>();

		for (int i = 0; i < providers.size(); i++) {
			ArrayList<Record> records = providers.get(i).getRecords(query);
			for (int j = 0; j < records.size(); j++) {
				allRecords.add(records.get(j));
			}
		}

		Collections.sort(allRecords, new RecordComparator());

		for (int i = 0; i < Math.min(15, allRecords.size()); i++) {
			adapter.add(allRecords.get(i));
		}
	}
}
