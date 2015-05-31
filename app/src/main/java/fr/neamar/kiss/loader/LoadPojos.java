package fr.neamar.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.util.ArrayList;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.dataprovider.Provider;

public abstract class LoadPojos<T> extends AsyncTask<Void, Void, ArrayList<T>> {

	protected Provider<T> provider;
	protected final Context context;
	protected String pojoScheme = "(none)://";

	public void setProvider(Provider<T> provider) {
		this.provider = provider;
	}

	public String getPojoScheme() {
		return pojoScheme;
	}

	public LoadPojos(Context context, String pojoScheme) {
		super();
		this.context = context;
		this.pojoScheme = pojoScheme;
	}

	@Override
	protected void onPostExecute(ArrayList<T> result) {
		super.onPostExecute(result);
		provider.loadOver(result);
		Intent i = new Intent(MainActivity.LOAD_OVER);
		context.sendBroadcast(i);
	}

}
