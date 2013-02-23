package fr.neamar.summon.lite.task;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import fr.neamar.summon.lite.SummonActivity;
import fr.neamar.summon.lite.dataprovider.Provider;

public abstract class LoadHolders<T> extends AsyncTask<Void, Void, ArrayList<T> > {

	protected Provider<T> provider;
	protected Context context;
	protected String holderScheme = "(none)://";
	
	public void setProvider(Provider<T> provider){
		this.provider = provider;
	}
	
	public String getHolderScheme() {
		return holderScheme;
	}

	public LoadHolders(Context context, String holderScheme) {
		super();
		this.context = context;
		this.holderScheme = holderScheme;
	}

	@Override
	protected void onPostExecute(ArrayList<T> result) {
		super.onPostExecute(result);
        provider.loadOver(result);
		Intent i = new Intent(SummonActivity.LOAD_OVER);
        context.sendBroadcast(i);
	}	
	
}
