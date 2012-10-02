package fr.neamar.summon.record;

import fr.neamar.summon.R;
import android.content.Context;
import android.view.View;

public class AppRecord extends Record {

	public AppRecord(String packageName) {
		super();

	}

	@Override
	public View display(Context context) {
		View v = inflateFromId(context, R.layout.item_app);

		return v;
	}

}
