package fr.neamar.kiss.task;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.kiss.holder.PhoneHolder;

public class LoadPhoneHolders extends LoadHolders<PhoneHolder> {

	public LoadPhoneHolders(Context context) {
		super(context, "none://");
	}

	@Override
	protected ArrayList<PhoneHolder> doInBackground(Void... params) {
		return null;
	}
}
