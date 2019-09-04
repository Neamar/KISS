package fr.neamar.kiss.loader;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fr.neamar.kiss.dataprovider.Provider;
import fr.neamar.kiss.pojo.Pojo;

public abstract class LoadPojos<T extends Pojo> extends AsyncTask<Void, Void, ArrayList<T>> {

    public static final ExecutorService IMAGE_EXCECUTOR
            = new ThreadPoolExecutor(0, 1, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    final WeakReference<Context> context;
    String pojoScheme = "(none)://";
    private WeakReference<Provider<T>> provider;

    LoadPojos(Context context, String pojoScheme) {
        this.context = new WeakReference<>(context);
        this.pojoScheme = pojoScheme;
    }

    public void setProvider(Provider<T> provider) {
        this.provider = new WeakReference<>(provider);
    }

    public String getPojoScheme() {
        return pojoScheme;
    }

    @Override
    protected void onPostExecute(ArrayList<T> result) {
        super.onPostExecute(result);
        if(provider != null) {
            provider.get().loadOver(result);
        }
    }

}
