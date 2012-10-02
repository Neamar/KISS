package fr.neamar.summon;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.record.RecordAdapter;

public class SummonActivity extends Activity {
	private ArrayList<Record> records = new ArrayList<Record>();
	private RecordAdapter adapter;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ListView listView = (ListView) findViewById(R.id.resultListView);

        adapter = new RecordAdapter(getApplicationContext(), R.layout.item_app, records);
        listView.setAdapter(adapter);
        
        Record r = new Record("neamar", "NEAMAR@neamar.fr");
        adapter.add(r);
    }
}