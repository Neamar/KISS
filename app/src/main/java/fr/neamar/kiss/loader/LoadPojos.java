package fr.neamar.kiss.loader;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.dataprovider.Provider;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;
import fr.neamar.kiss.utils.Log;

public abstract class LoadPojos<T extends Pojo> extends AsyncTask<Void, Void, List<T>> {

    private static final String TAG = LoadPojos.class.getSimpleName();

    protected final WeakReference<Context> context;
    protected final String pojoScheme;
    private WeakReference<Provider<T>> providerReference;
    private long start;

    LoadPojos(Context context, String pojoScheme) {
        super();
        this.context = new WeakReference<>(context);
        this.pojoScheme = pojoScheme;
    }

    public void setProvider(Provider<T> provider) {
        this.providerReference = new WeakReference<>(provider);
    }

    public String getPojoScheme() {
        return pojoScheme;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        start = System.currentTimeMillis();
        Log.i(TAG, "Starting " + this.getClass().getSimpleName());
    }

    @Override
    protected void onPostExecute(List<T> result) {
        super.onPostExecute(result);
        if (providerReference != null) {
            Provider<T> provider = providerReference.get();
            if (provider != null && !isCancelled()) {
                // Applied here rather than in the background, so that tags edited during the load are kept
                TagsHandler tagsHandler = KissApplication.getApplication(provider).getDataHandler().getTagsHandler();
                for (T pojo : result) {
                    if (pojo instanceof PojoWithTags) {
                        ((PojoWithTags) pojo).setTags(tagsHandler.getTags(pojo.id));
                    }
                }
                provider.loadOver(result);
            }
        }
        long time = System.currentTimeMillis() - start;
        Log.d(TAG, "Time to run " + this.getClass().getSimpleName() + ": " + time + "ms");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        long time = System.currentTimeMillis() - start;
        Log.d(TAG, "Cancelled " + this.getClass().getSimpleName() + " after " + time + "ms");
    }

}
