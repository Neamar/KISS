package fr.neamar.kiss.dataprovider;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.loader.LoadPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.utils.Log;

public abstract class Provider<T extends Pojo> extends Service implements IProvider<T> {
    private final static String TAG = Provider.class.getSimpleName();

    /**
     * Binder given to clients
     */
    private final IBinder binder = new LocalBinder();
    /**
     * Storage for search items used by this provider
     */
    private List<T> pojos = new ArrayList<>();

    /**
     * Scheme used to build ids for the pojos created by this provider
     */
    private String pojoScheme = "(none)://";

    private ExecutorService executorService;
    private final List<LoadPojos<T>> runningLoaders = new ArrayList<>();
    private boolean loaded = false;

    /**
     * (Re-)load the providers resources when the provider has been completely initialized
     * by the Android system
     */
    @Override
    public void onCreate() {
        super.onCreate();

        if (isRestrictedMode()) {
            Log.w(TAG, "Not running in KissApplication, process is likely in restricted mode (backup/restore)");
            stopSelf();
            return;
        }
        this.executorService = Executors.newSingleThreadExecutor();

        this.reload();
    }

    /**
     * During a full backup or restore, Android may start the process in restricted mode with a plain Application,
     * and restart sticky providers in it. Providers can't work there.
     */
    boolean isRestrictedMode() {
        return !(getApplication() instanceof KissApplication);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.executorService.shutdown();
        cancelInitialize(null);
    }

    void initialize(LoadPojos<T> loader) {
        cancelInitialize(loader);

        loader.setProvider(this);
        this.pojoScheme = loader.getPojoScheme();
        this.runningLoaders.add((LoadPojos<T>) loader.executeOnExecutor(executorService));
    }

    /**
     * Cancel running {@link LoadPojos<T>} tasks and set to null.
     */
    private void cancelInitialize(LoadPojos<T> newLoader) {
        List<LoadPojos<T>> loadersToCancel = this.runningLoaders.stream()
                .filter(loader -> loader.isCancellableBy(newLoader))
                .collect(Collectors.toList());
        loadersToCancel.forEach(loader -> {
            loader.cancel(false);
            loader.setProvider(null);
            this.runningLoaders.remove(loader);
            this.loaded = false;
        });
    }

    @Override
    public void reload(ReloadFilter filter) {
        // Handled at subclass level
        if (!pojos.isEmpty()) {
            Log.v(TAG, "Reloading provider: " + this.getClass().getSimpleName() + " (filter=" + filter+ ")");
        }
    }

    @Override
    public boolean isLoaded() {
        return this.loaded;
    }

    public void loadOver(LoadPojos<T> finishedLoader, List<T> results, ReloadFilter filter) {
        finishedLoader.setProvider(null);

        if (this.runningLoaders.remove(finishedLoader)) {
            if (filter == null) {
                this.pojos = results;
            } else {
                this.pojos = filter.merge(this.pojos, results);
            }
        }
        if (this.runningLoaders.isEmpty()) {
            this.loaded = true;
        }
        if (isLoaded()) {
            KissApplication.getApplication(this).getDataHandler().notifyMainActivity(MainActivity.LOAD_OVER);
        }
    }

    /**
     * Tells whether or not this provider may be able to find the pojo with
     * specified id
     *
     * @param id id we're looking for
     * @return true if the provider can handle the query ; does not guarantee it
     * will!
     */
    @Override
    public boolean mayFindById(String id) {
        return id.startsWith(pojoScheme);
    }

    /**
     * Try to find a record by its id
     *
     * @param id id we're looking for
     * @return null if not found
     */
    @Override
    @Nullable
    public Pojo findById(String id) {
        for (T pojo : pojos) {
            if (pojo.id.equals(id)) {
                return pojo;
            }
        }

        return null;
    }

    @Override
    public List<T> getPojos() {
        return Collections.unmodifiableList(pojos);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public IProvider<T> getService() {
            // Return this instance of the provider so that clients can call public methods
            return Provider.this;
        }
    }
}
