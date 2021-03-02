package fr.neamar.kiss.utils;

import android.util.Log;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class ViewHolderListAdapter<T, VH extends ViewHolderAdapter.ViewHolder<T>> extends ViewHolderAdapter<T, VH> {
    @NonNull
    protected final List<T> mList;

    protected ViewHolderListAdapter(@NonNull Class<VH> viewHolderClass, int listItemLayout, @NonNull List<T> list) {
        super(viewHolderClass, listItemLayout);
        mList = list;
    }

    @LayoutRes
    protected int getItemViewTypeLayout(int viewType) {
        return mListItemLayout;
    }

    @Override
    public T getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    public void addItems(Collection<? extends T> items) {
        mList.addAll(items);
        notifyDataSetChanged();
    }

    public void addItem(T item) {
        mList.add(item);
        notifyDataSetChanged();
    }

    @Nullable
    public LoadAsyncList<T> newLoadAsyncList(@NonNull Class<LoadAsyncList<T>> loadAsyncClass, @NonNull LoadAsyncData.LoadInBackground<T> loadInBackground) {
        LoadAsyncList<T> loadAsync = null;
        try {
            loadAsync = loadAsyncClass.getDeclaredConstructor(this.getClass(), loadInBackground.getClass()).newInstance(this, loadInBackground);
        } catch (Exception e) {
            Log.e("VHLA", "LoadAsync can't be instantiated (make sure class and constructor are public)", e);
        }
        return loadAsync;
    }

    @NonNull
    public LoadAsyncList<T> newLoadAsyncList(@NonNull LoadAsyncData.LoadInBackground<T> loadInBackground) {
        return new LoadAsyncList<>(this, loadInBackground);
    }

    public static class LoadAsyncList<T> extends LoadAsyncData<T> {

        public LoadAsyncList(@NonNull ViewHolderListAdapter<T, ? extends ViewHolder<T>> adapter, @NonNull LoadInBackground<T> loadInBackground) {
            super(adapter, loadInBackground);
        }

        @Override
        protected void onDataLoadFinished(@NonNull ViewHolderAdapter<T, ? extends ViewHolder<T>> adapter, @NonNull Collection<T> data) {
            ViewHolderListAdapter<T, ? extends ViewHolder<T>> listAdapter;
            //noinspection unchecked
            listAdapter = (ViewHolderListAdapter<T, ? extends ViewHolder<T>>) adapter;
            listAdapter.addItems(data);
        }
    }
}
