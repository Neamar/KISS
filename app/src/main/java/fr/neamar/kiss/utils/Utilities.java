package fr.neamar.kiss.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Utilities {

    /**
     * Return a valid activity or null given a context
     *
     * @param ctx context
     * @return an activity or null
     */
    @Nullable
    public static Activity getActivity(@Nullable Context ctx) {
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                Activity act = (Activity) ctx;
                if (act.isFinishing()) {
                    return null;
                }
                if (act.isDestroyed()) {
                    return null;
                }
                return act;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    public static <T> Utilities.AsyncRun<T> runAsync(@NonNull Function<AsyncRun<T>, T> background, @Nullable BiConsumer<AsyncRun<T>, T> after) {
        return runAsync(background, after, getDefaultExecutor());
    }

    public static <T> Utilities.AsyncRun<T> runAsync(@NonNull Function<AsyncRun<T>, T> background, @Nullable BiConsumer<AsyncRun<T>, T> after, @NonNull Executor exec) {
        return (AsyncRun<T>) new Utilities.AsyncRun<>(background, after).executeOnExecutor(exec);
    }

    /**
     * Get default executor for async operations.
     * From android Q AsyncTask.THREAD_POOL_EXECUTOR supports fallback if queue gets too long.
     * Else we use more safe AsyncTask.SERIAL_EXECUTOR.
     *
     * @return Executor
     */
    public static Executor getDefaultExecutor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AsyncTask.THREAD_POOL_EXECUTOR;
        } else {
            return AsyncTask.SERIAL_EXECUTOR;
        }
    }

    public static class AsyncRun<Result> extends AsyncTask<Void, Void, Result> {
        private final Function<AsyncRun<Result>, Result> mBackground;
        private final BiConsumer<AsyncRun<Result>, Result> mAfter;

        public AsyncRun(@NonNull Function<AsyncRun<Result>, Result> background, @Nullable BiConsumer<AsyncRun<Result>, Result> after) {
            super();
            mBackground = background;
            mAfter = after;
        }

        @Override
        protected Result doInBackground(Void... voids) {
            return mBackground.apply(this);
        }

        @Override
        protected void onCancelled(Result result) {
            if (mAfter != null)
                mAfter.accept(this, result);
        }

        @Override
        protected void onPostExecute(Result result) {
            if (mAfter != null && !isCancelled()) {
                mAfter.accept(this, result);
            }
        }

        public boolean cancel() {
            return cancel(false);
        }
    }
}
