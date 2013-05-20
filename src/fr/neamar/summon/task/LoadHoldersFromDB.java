package fr.neamar.summon.task;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

import fr.neamar.summon.SummonActivity;

public abstract class LoadHoldersFromDB<T> extends LoadHolders<T> {

    public LoadHoldersFromDB(Context context, String holderScheme) {
        super(context, holderScheme);
    }

    @Override
	protected void onPostExecute(ArrayList<T> result) {
		provider.loadOver(result);
        Intent i = new Intent(SummonActivity.LOAD_DB_OVER);
        context.sendBroadcast(i);
	}

}
