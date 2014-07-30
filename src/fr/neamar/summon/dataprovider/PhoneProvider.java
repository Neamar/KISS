package fr.neamar.summon.dataprovider;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.holder.PhoneHolder;
import fr.neamar.summon.task.LoadPhoneHolders;

public class PhoneProvider extends Provider<PhoneHolder> {

	public PhoneProvider(Context context) {
		super(new LoadPhoneHolders(context));
	}

	public ArrayList<Holder> getResults(String query) {
		ArrayList<Holder> holders = new ArrayList<Holder>();

		PhoneHolder holder = new PhoneHolder();
		holder.phone = query;
		holder.relevance = 10;
		holders.add(holder);
		return holders;
	}
}
