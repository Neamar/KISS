package fr.neamar.kiss.loader;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;

import fr.neamar.kiss.dataprovider.Provider;
import fr.neamar.kiss.pojo.Pojo;

public abstract class LoadPojos<T extends Pojo> extends AsyncTask<Void, Void, List<T>> {

    final WeakReference<Context> context;
    final String pojoScheme;
    private WeakReference<Provider<T>> providerReference;

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
    protected void onPostExecute(List<T> result) {
        super.onPostExecute(result);
        if (providerReference != null) {
            Provider<T> provider = providerReference.get();
            if (provider != null && !isCancelled()) {
                provider.loadOver(result);
            }
        }
    }

}
