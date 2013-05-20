package fr.neamar.summon.task;

import android.content.Context;

import java.util.ArrayList;

public abstract class LoadHoldersFromDB<T> extends LoadHolders<T> {

    public LoadHoldersFromDB(Context context, String holderScheme) {
        super(context, holderScheme);
    }

    @Override
	protected void onPostExecute(ArrayList<T> result) {
		provider.loadOver(result);
	}

}
