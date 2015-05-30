package fr.neamar.kiss.loader;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.kiss.pojo.PhonePojo;

public class LoadPhonePojos extends LoadPojos<PhonePojo> {

	public LoadPhonePojos(Context context) {
		super(context, "none://");
	}

	@Override
	protected ArrayList<PhonePojo> doInBackground(Void... params) {
		return null;
	}
}
