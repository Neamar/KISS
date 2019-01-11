package fr.neamar.kiss.shortcut;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.Locale;

import androidx.annotation.NonNull;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.pojo.ShortcutsPojo;

public class SaveShortcutAsync extends AsyncTask<Void, Void, Boolean> {
	final static private String TAG = "SaveShortcutAsync";

	private final WeakReference<Context> context;
	private final WeakReference<DataHandler> dataHandler;
	private Intent data;

	public SaveShortcutAsync(@NonNull Context context, @NonNull Intent data) {
		this.context = new WeakReference<>(context);
		this.dataHandler = new WeakReference<>(KissApplication.getApplication(context).getDataHandler());
		this.data = data;
	}

	@Override
	protected void onPreExecute() {
		//Skipping mostly redundant null check for this.dataHandler
		if (dataHandler.get().getShortcutsProvider() == null){
			cancel(true);
		}
	}

	@Override
	protected Boolean doInBackground(Void... voids) {
		String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
		Log.d(TAG, "Received shortcut " + name);

		Intent target = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
		if (target.getAction() == null) {
			target.setAction(Intent.ACTION_VIEW);
		}

		// convert target intent to parsable uri
		String intentUri = target.toUri(0);
		String packageName = null;
		String resourceName = null;

		// get embedded icon
		Bitmap icon = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
		if (icon != null) {
			Log.d(TAG, "Shortcut " + name + " has embedded icon");
		} else {
			Intent.ShortcutIconResource sir = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);

			if (sir != null) {
				Log.d(TAG, "Received icon package name " + sir.packageName);
				Log.d(TAG, "Received icon resource name " + sir.resourceName);

				packageName = sir.packageName;
				resourceName = sir.resourceName;
			} else {
				//invalid shortcut
				Log.d(TAG, "Invalid shortcut " + name + ", ignoring");
				cancel(true);
				return null;
			}
		}

		try {
			Intent intent = Intent.parseUri(intentUri, 0);
			if (intent.getCategories() != null && intent.getCategories().contains(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(intent.getAction())) {
				// The Play Store has an option to create shortcut for new apps,
				// However, KISS already displays all apps, so we discard the shortcut to avoid duplicates.
				Log.d(TAG, "Shortcut for launcher app, discarded.");
				cancel(true);
				return null;
			}
		} catch (URISyntaxException e) {
			// Invalid intentUri: skip
			// (should logically not happen)
			e.printStackTrace();
			cancel(true);
			return null;
		}

		String id = ShortcutsPojo.SCHEME + name.toLowerCase(Locale.ROOT);
		ShortcutsPojo pojo = new ShortcutsPojo(id, packageName, resourceName, intentUri, icon);

		pojo.setName(name);

		final DataHandler dataHandler = this.dataHandler.get();
		if(dataHandler == null) {
			cancel(true);
			return null;
		}

		return dataHandler.addShortcut(pojo);
	}

	@Override
	protected void onPostExecute(Boolean success) {
		final Context context = this.context.get();
		if(context != null && success != null && success) {
			Toast.makeText(context, R.string.shortcut_added, Toast.LENGTH_SHORT).show();
		}
	}
}
