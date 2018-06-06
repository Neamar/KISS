package fr.neamar.kiss.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import fr.neamar.kiss.R;

import java.util.ArrayList;

/**
 * ListPopup wrapper for widget managing
 */
public class WidgetMenu {
    private ListPopup mPopup = null;
    private final Adapter mAdapter;
    private final OnClickListener mListener;

    public WidgetMenu(OnClickListener listener) {
        mListener = listener;
        mAdapter = new Adapter(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPopup != null)
                    mPopup.dismiss();
                mListener.onWidgetEdit((Integer)v.getTag());
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPopup != null)
                    mPopup.dismiss();
                mListener.onWidgetRemove((Integer)v.getTag());
            }
        });
    }

    public void add(Context context, @StringRes int stringRes) {
        WidgetAdd item = new WidgetAdd(context, stringRes);
        mAdapter.add(item);
        //mAdapter.add(new MenuDivider());
    }

    public void add(int appWidgetId, String name) {
        WidgetInfoBtn item = new WidgetInfoBtn(appWidgetId, name);
        mAdapter.add(item);
    }

    public ListPopup show(View anchor) {
        mPopup = new ListPopup(anchor.getContext());
        mPopup.setOnItemClickListener(new ListPopup.OnItemClickListener() {
            @Override
            public void onItemClick(ListAdapter adapter, View view, int position) {
                mListener.onWidgetAdd();
            }
        });
        mPopup.setAdapter(mAdapter);
        mPopup.showCentered(anchor);
        return mPopup;
    }

    public interface OnClickListener {
        void onWidgetAdd();
        void onWidgetEdit(int appWidgetId);
        void onWidgetRemove(int appWidgetId);
    }


    interface MenuItem {
        @LayoutRes
        int getLayoutResource();
    }

    static class MenuDivider implements WidgetMenu.MenuItem {
        @Override
        public int getLayoutResource() {
            return R.layout.popup_divider;
        }
    }

    static class WidgetAdd implements WidgetMenu.MenuItem {
        final String name;

        WidgetAdd(Context context, @StringRes int resName) {
            this.name = context.getString(resName);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_list_item;
        }
    }

    static class WidgetInfoBtn implements WidgetMenu.MenuItem {
        final int appWidgetId;
        final String name;

        WidgetInfoBtn(int appWidgetId, String name) {
            this.appWidgetId = appWidgetId;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_widget;
        }
    }

    static class Adapter extends BaseAdapter {
        final ArrayList<WidgetMenu.MenuItem> list = new ArrayList<>(0);
        private final View.OnClickListener mEditListener;
        private final View.OnClickListener mRemoveListener;

        Adapter(View.OnClickListener editListener, View.OnClickListener removeListener) {
            this.mEditListener = editListener;
            this.mRemoveListener = removeListener;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public WidgetMenu.MenuItem getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            WidgetMenu.MenuItem item = getItem(position);
            String text = item.toString();
            convertView = LayoutInflater.from(parent.getContext()).inflate(item.getLayoutResource(), parent, false);
            if (item instanceof MenuDivider) {
                return convertView;
            }

            TextView textView = convertView.findViewById(android.R.id.text1);
            textView.setText(text);

            if (item instanceof WidgetInfoBtn) {
                View btn = convertView.findViewById(android.R.id.button1);
                btn.setTag(((WidgetInfoBtn) item).appWidgetId);
                btn.setOnClickListener(mEditListener);

                btn = convertView.findViewById(android.R.id.button2);
                btn.setTag(((WidgetInfoBtn) item).appWidgetId);
                btn.setOnClickListener(mRemoveListener);
            }
            return convertView;
        }

        public void add(WidgetMenu.MenuItem item) {
            list.add(item);
            notifyDataSetChanged();
        }

        public void add(int index, WidgetMenu.MenuItem item) {
            list.add(index, item);
            notifyDataSetChanged();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            WidgetMenu.MenuItem item = list.get(position);
            return item instanceof WidgetMenu.WidgetAdd;
        }
    }
}
