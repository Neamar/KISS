package fr.neamar.kiss.customicon;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

import fr.neamar.kiss.utils.ViewHolderAdapter;

public class IconViewHolder extends ViewHolderAdapter.ViewHolder<IconData> {
    public final static Executor EXECUTOR_LOAD_ICON = AsyncTask.SERIAL_EXECUTOR;
    private final ImageView icon;
    private AsyncLoad loader = null;

    public IconViewHolder(View view) {
        super(view);
        icon = view.findViewById(android.R.id.icon);
    }

    @Override
    protected void setContent(IconData content, int position, @NonNull ViewHolderAdapter<IconData, ? extends ViewHolderAdapter.ViewHolder<IconData>> adapter) {
        if (loader != null)
            loader.cancel(false);
        loader = new AsyncLoad(this);
        loader.executeOnExecutor(EXECUTOR_LOAD_ICON, content);
    }

    static class AsyncLoad extends AsyncTask<IconData, Void, Drawable> {
        private final WeakReference<IconViewHolder> holder;

        protected AsyncLoad(IconViewHolder holder) {
            super();
            this.holder = new WeakReference<>(holder);
        }

        @Override
        protected void onPreExecute() {
            IconViewHolder h = holder.get();
            if (h == null || h.loader != this)
                return;
            h.icon.setImageDrawable(null);
        }

        @Override
        protected Drawable doInBackground(IconData... iconData) {
            return iconData[0].getIcon();
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            IconViewHolder h = holder.get();
            if (h == null || h.loader != this)
                return;
            h.loader = null;
            h.icon.setImageDrawable(drawable);
            h.icon.setScaleX(0f);
            h.icon.setScaleY(0f);
            h.icon.setRotation((drawable.hashCode() & 1) == 1 ? 180f : -180f);
            h.icon.animate().scaleX(1f).scaleY(1f).rotation(0f).start();
        }
    }
}
