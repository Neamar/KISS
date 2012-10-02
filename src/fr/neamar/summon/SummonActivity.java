package fr.neamar.summon;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;
import fr.neamar.summon.record.AppRecord;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.record.RecordAdapter;

public class SummonActivity extends Activity {
	private ArrayList<Record> records = new ArrayList<Record>();
	private RecordAdapter adapter;
	private SummonActivity summonActivity = this;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Initialize UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ListView listView = (ListView) findViewById(R.id.resultListView);

		//Create adapter for records
		adapter = new RecordAdapter(getApplicationContext(), R.layout.item_app,
				records);
		listView.setAdapter(adapter);

		//Listen to changes
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
	 * This function gets called on changes.
	 * It will ask all the providers for datas
	 * @param s
	 */
	public void updateRecords(String s) {
		adapter.clear();
		Record r = new AppRecord("fr.om");
		adapter.add(r);
	}
}