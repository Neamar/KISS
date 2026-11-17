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
                // Tags were read in the background: they may have been edited since
                TagsHandler tagsHandler = KissApplication.getApplication(provider).getDataHandler().getTagsHandler();
                for (T pojo : result) {
                    if (pojo instanceof PojoWithTags) {
                        ((PojoWithTags) pojo).setTags(tagsHandler.getTags(pojo.id));
                    }
                }
                provider.loadOver(result);
            }
        }
    }

}
