package fr.neamar.kiss.shortcut;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;

@RequiresApi(Build.VERSION_CODES.O)
public class SaveSingleOreoShortcutAsync extends AsyncTask<Void, Integer, Boolean> {

    private static final String TAG = SaveSingleOreoShortcutAsync.class.getSimpleName();
    private final WeakReference<Context> context;
    private final Intent intent;

    public SaveSingleOreoShortcutAsync(@NonNull Context context, @NonNull Intent intent) {
        this.context = new WeakReference<>(context);
        this.intent = intent;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        final LauncherApps.PinItemRequest pinItemRequest = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        final ShortcutInfo shortcutInfo = pinItemRequest.getShortcutInfo();

        if (shortcutInfo == null) {
            cancel(true);
            return null;
        }

        if (!pinItemRequest.isValid()) {
            return false;
        }
        if (!pinItemRequest.accept()) {
            return false;
        }

        Context context = this.context.get();
        if (context == null) {
            cancel(true);
            return null;
        }

        final DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();

        // Add shortcut to the DataHandler
        return dataHandler.updateShortcut(shortcutInfo, false);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] == -1) {
            Context context = this.context.get();
            if (context != null) {
                Toast.makeText(context, R.string.cant_pin_shortcut, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPostExecute(@NonNull Boolean success) {
        if (success) {
            Log.i(TAG, "Shortcut added to KISS");

            Context context = this.context.get();
            if (context != null) {
                DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
                ShortcutsProvider provider = dataHandler.getShortcutsProvider();
                if (provider != null) {
                    provider.reload();
                }
            }
        }
    }

}
