package fr.neamar.kiss.shortcut;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.utils.ShortcutUtil;

@TargetApi(Build.VERSION_CODES.O)
public class SaveSingleOreoShortcutAsync extends AsyncTask<Void, Integer, Boolean> {

    private static String TAG = "SaveAllOreoShortcutsAsync";
    private final WeakReference<Context> context;
    private final WeakReference<DataHandler> dataHandler;
    private Intent intent;

    public SaveSingleOreoShortcutAsync(@NonNull Context context, @NonNull Intent intent) {
        this.context = new WeakReference<>(context);
        this.dataHandler = new WeakReference<>(KissApplication.getApplication(context).getDataHandler());
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

        Context context = this.context.get();
        if (context == null) {
            cancel(true);
            return null;
        }

        // Create Pojo
        ShortcutRecord record = ShortcutUtil.createShortcutRecord(context, shortcutInfo, false);
        if (record == null) {
            return false;
        }

        final DataHandler dataHandler = this.dataHandler.get();
        if (dataHandler == null) {
            cancel(true);
            return null;
        }

        // Add shortcut to the DataHandler
        if(dataHandler.addShortcut(record)){
            try {
                pinItemRequest.accept();
            }
            catch(IllegalStateException e) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] == -1) {
            Toast.makeText(context.get(), R.string.cant_pin_shortcut, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPostExecute(@NonNull Boolean success) {
        if (success) {
            Log.i(TAG, "Shortcut added to KISS");

            ShortcutsProvider provider = this.dataHandler.get().getShortcutsProvider();
            if (provider != null) {
                provider.reload();
            }
        }
    }

}
