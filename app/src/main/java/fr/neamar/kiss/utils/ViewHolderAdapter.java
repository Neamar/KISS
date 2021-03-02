package fr.neamar.kiss.utils;

import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import fr.neamar.kiss.BuildConfig;

/**
 * Adapter class that implements the View holder pattern.
 * The ViewHolder is held as a tag in the list item view.
 *
 * @param <T>  Type of data to send to the ViewHolder
 * @param <VH> ViewHolder class
 */
public abstract class ViewHolderAdapter<T, VH extends ViewHolderAdapter.ViewHolder<T>> extends BaseAdapter {
    @NonNull
    final Class<VH> mViewHolderClass;
    @LayoutRes
    final int mListItemLayout;

    protected ViewHolderAdapter(@NonNull Class<VH> viewHolderClass, @LayoutRes int listItemLayout) {
        mViewHolderClass = viewHolderClass;
        mListItemLayout = listItemLayout;
    }

    @LayoutRes
    protected int getItemViewTypeLayout(int viewType) {
        return mListItemLayout;
    }

    @Override
    public abstract T getItem(int position);

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Nullable
    protected VH getNewViewHolder(View view) {
        VH holder = null;
        try {
            holder = mViewHolderClass.getDeclaredConstructor(View.class).newInstance(view);
        } catch (Exception e) {
            Log.e("VHA", "ViewHolder can't be instantiated (make sure class and constructor are public)", e);
        }
        return holder;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            int viewType = getItemViewType(position);
            if (BuildConfig.DEBUG) {
                int viewTypeCount = getViewTypeCount();
                if (viewType >= viewTypeCount)
                    throw new IllegalStateException("ViewType " + viewType + " >= ViewTypeCount " + viewTypeCount);
            }
            @LayoutRes
            int itemLayout = getItemViewTypeLayout(viewType);
            view = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        } else {
            view = convertView;
        }

        Object tag = view.getTag();
        VH holder = mViewHolderClass.isInstance(tag) ? mViewHolderClass.cast(tag) : getNewViewHolder(view);
        if (holder != null) {
            T content = getItem(position);
            holder.setContent(content, position, this);
        }
        return view;

    }

    public static abstract class ViewHolder<T> {
        protected ViewHolder(View view) {
            view.setTag(this);
        }

        protected abstract void setContent(T content, int position, @NonNull ViewHolderAdapter<T, ? extends ViewHolder<T>> adapter);
    }

    public static abstract class LoadAsyncData<T> extends AsyncTask<Void, Void, Collection<T>> {
        private final ViewHolderAdapter<T, ? extends ViewHolder<T>> adapter;
        private final LoadInBackground<T> task;

        public interface LoadInBackground<T> {
            @Nullable
            Collection<T> loadInBackground();
        }

        public LoadAsyncData(@NonNull ViewHolderAdapter<T, ? extends ViewHolder<T>> adapter, @NonNull LoadInBackground<T> loadInBackground) {
            super();
            this.adapter = adapter;
            task = loadInBackground;
        }

        @Override
        protected Collection<T> doInBackground(Void... voids) {
            return task.loadInBackground();
        }

        @Override
        protected void onPostExecute(Collection<T> data) {
            if (data == null)
                return;
            //adapter.addAll(data);
            onDataLoadFinished(adapter, data);
        }

        protected abstract void onDataLoadFinished(@NonNull ViewHolderAdapter<T, ? extends ViewHolder<T>> adapter, @NonNull Collection<T> data);
    }
}
