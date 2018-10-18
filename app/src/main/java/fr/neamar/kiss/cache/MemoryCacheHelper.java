package fr.neamar.kiss.cache;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.utils.UserHandle;

public class MemoryCacheHelper {

    private static final HashMap<AppIconHandle, Drawable> sAppIconCache = new HashMap<>();

    public static <T> void getAppIconDrawable(@NonNull Context context, ComponentName className, UserHandle userHandle, @NonNull OnDrawableReady<T> callback) {
        new AsyncAppIconLoad(context, className, userHandle, callback).execute();
    }

    /**
     * This is called from the async task. To prevent accidents, do not access sAppIconCache without a sync block
     *
     * @param context    context to use
     * @param className  app
     * @param userHandle android.os.UserHandle
     * @return app icon drawable from cache
     */
    public static Drawable getAppIconDrawable(@NonNull Context context, ComponentName className, UserHandle userHandle) {
        AppIconHandle handle = new AppIconHandle(className, userHandle);
        Drawable drawable;
        synchronized (sAppIconCache) {
            drawable = sAppIconCache.get(handle);
            if (drawable == null) {
                // if the drawable cached for this app is null don't bother trying to find it again
                if (sAppIconCache.containsKey(handle))
                    return null;
                drawable = KissApplication.getApplication(context).getIconsHandler()
                        .getDrawableIconForPackage(className, userHandle);
                sAppIconCache.put(handle, drawable);
            }
        }
        return drawable;
    }

    public static void cacheAppIconDrawable(@NonNull Context context, ComponentName className, UserHandle userHandle) {
        new AsyncAppIconLoad(context, className, userHandle, null).execute();
    }

    private static class AsyncAppIconLoad extends AsyncTask<Void, Void, Drawable> {
        final WeakReference<Context> contextRef;
        final ComponentName className;
        final UserHandle userHandle;
        final OnDrawableReady callback;

        public AsyncAppIconLoad(@NonNull Context context, ComponentName className, UserHandle userHandle, @Nullable OnDrawableReady callback) {
            this.contextRef = new WeakReference<>(context);
            this.className = className;
            this.userHandle = userHandle;
            this.callback = callback;
        }

        @Override
        protected Drawable doInBackground(Void... voids) {
            Context context = contextRef.get();
            if (isCancelled() || context == null)
                return null;
            return getAppIconDrawable(context, className, userHandle);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            super.onPostExecute(drawable);
            if (isCancelled() || callback == null)
                return;
            callback.onDrawableReady(drawable);
        }
    }

    static class AppIconHandle implements Comparable<AppIconHandle> {
        final ComponentName componentName;
        final UserHandle userHandle;

        AppIconHandle(ComponentName componentName, UserHandle userHandle) {
            this.componentName = componentName;
            this.userHandle = userHandle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AppIconHandle handle = (AppIconHandle) o;

            if (componentName != null ? !componentName.equals(handle.componentName) : handle.componentName != null)
                return false;
            return userHandle != null ? userHandle.equals(handle.userHandle) : handle.userHandle == null;
        }

        @Override
        public int hashCode() {
            int result = componentName != null ? componentName.hashCode() : 0;
            result = 31 * result + (userHandle != null ? userHandle.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(@NonNull AppIconHandle o) {
            return 0;
        }
    }

    public abstract static class OnDrawableReady<T> {
        public T data;

        public OnDrawableReady(T data) {
            this.data = data;
        }

        public abstract void onDrawableReady(@Nullable Drawable drawable);
    }
}
