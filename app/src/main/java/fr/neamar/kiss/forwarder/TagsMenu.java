package fr.neamar.kiss.forwarder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.TagsHandler;
import fr.neamar.kiss.dataprovider.simpleprovider.TagsProvider;
import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.ui.ListPopup;

/**
 * Created by TBog on 5/8/2018.
 */
public class TagsMenu extends Forwarder {
    private final Set<String> tagList;
    private ListPopup popupMenu = null;

    public TagsMenu(MainActivity mainActivity) {
        super(mainActivity);
        tagList = new TreeSet<>();
    }

    public void onCreate() {
        loadTags();
    }

    public void onResume() {
        loadTags();
    }

    public boolean isTagMenuEnabled() {
        return prefs.getBoolean("pref-tags-menu", false);
    }

    public boolean isAutoDismiss()
    {
        return prefs.getBoolean("pref-tags-menu-dismiss", false);
    }

    private void loadTags() {
        if (isTagMenuEnabled())
        	setTags(getPrefTags(prefs, mainActivity));
        else
            setTags(null);
    }

    @NonNull
    public static Set<String> getPrefTags(SharedPreferences prefs, Context context) {
        Set<String> prefTags = getPrefTags(prefs);
        if (prefTags == null || prefTags.isEmpty()) {
            Set<String> tags = new HashSet<>(5);
            TagsHandler tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
            Set<String> list = tagsHandler.getAllTagsAsSet();
            for (String tag : list) {
                tags.add(tag);
                if (tags.size() >= 5)
                    break;
            }
            return tags;
        }
        return prefTags;
    }

    @Nullable
    static Set<String> getPrefTags(SharedPreferences prefs) {
        return prefs.getStringSet("pref-toggle-tags-list", null);
    }

    private void setTags(Set<String> list) {
        tagList.clear();
        if (list != null)
            tagList.addAll(list);
    }

    interface MenuItem {
        @LayoutRes
        int getLayoutResource();
    }

    static class MenuItemDivider implements TagsMenu.MenuItem {
        @Override
        public int getLayoutResource() {
            return R.layout.popup_divider;
        }
    }

    static class MenuItemTitle implements TagsMenu.MenuItem {
        final String name;

        MenuItemTitle(Context context, @StringRes int nameRes) {
            this.name = context.getString(nameRes);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_title;
        }
    }

    static class MenuItemTag implements TagsMenu.MenuItem {
        final String tag;

        MenuItemTag(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_list_item;
        }
    }

    static class MenuItemBtn implements TagsMenu.MenuItem {
        final int nameRes;
        final String name;

        MenuItemBtn(Context context, @StringRes int nameRes) {
            this.nameRes = nameRes;
            this.name = context.getString(nameRes);
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

    static class MenuAdapter extends BaseAdapter {
        final ArrayList<TagsMenu.MenuItem> list = new ArrayList<>(0);

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public TagsMenu.MenuItem getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TagsMenu.MenuItem item = getItem(position);
            String text = item.toString();
            convertView = LayoutInflater.from(parent.getContext()).inflate(item.getLayoutResource(), parent, false);
            if (item instanceof TagsMenu.MenuItemDivider) {
                return convertView;
            }

            TextView textView = convertView.findViewById(android.R.id.text1);
            textView.setText(text);

            return convertView;
        }

        public void add(TagsMenu.MenuItem item) {
            list.add(item);
            notifyDataSetChanged();
        }

        public void add(int index, TagsMenu.MenuItem item) {
            list.add(index, item);
            notifyDataSetChanged();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            TagsMenu.MenuItem item = list.get(position);
            return (item instanceof TagsMenu.MenuItemTag) || (item instanceof TagsMenu.MenuItemBtn);
        }
    }

    public ListPopup showMenu(final View anchor) {
        if (popupMenu != null) {
            popupMenu.show(anchor, 0f);
            return popupMenu;
        }

        Context context = anchor.getContext();
        popupMenu = new ListPopup(context);
        TagsMenu.MenuAdapter adapter = new TagsMenu.MenuAdapter();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        //build menu
        adapter.add(new TagsMenu.MenuItemTitle(context, R.string.popup_tags_title));
        for (String tag : tagList) {
            adapter.add(new TagsMenu.MenuItemTag(tag));
        }

        // remember where the title should go
        int actionsTitlePosition = adapter.getCount();
        if (!prefs.getBoolean("history-onclick", false))
            adapter.add(new TagsMenu.MenuItemBtn(context, R.string.show_history));
        if (prefs.getBoolean("pref-show-untagged", false))
            adapter.add(new TagsMenu.MenuItemBtn(context, R.string.show_untagged));
        // insert title only if at least an action was added
        if (actionsTitlePosition != adapter.getCount())
            adapter.add(actionsTitlePosition, new TagsMenu.MenuItemTitle(context, R.string.popup_tags_actions));

        adapter.add(new TagsMenu.MenuItemDivider());
        adapter.add(new TagsMenu.MenuItemBtn(context, R.string.ctx_menu));

        // set popup interaction rules
        popupMenu.setAdapter(adapter);
        popupMenu.setDismissOnItemClick( isAutoDismiss() );
        popupMenu.setOnItemClickListener(new ListPopup.OnItemClickListener() {
            @Override
            public void onItemClick(ListAdapter adapter, View view, int position) {
                Object adapterItem = adapter.getItem(position);
                if (adapterItem instanceof TagsMenu.MenuItemTag) {
                    TagsMenu.MenuItemTag item = (TagsMenu.MenuItemTag) adapterItem;
                    // show only apps that match this tag
                    mainActivity.showMatchingTags(item.tag);
                } else if (adapterItem instanceof TagsMenu.MenuItemBtn) {
                    switch (((TagsMenu.MenuItemBtn) adapterItem).nameRes) {
                        case R.string.ctx_menu:
                            if (popupMenu != null)
                                popupMenu.dismiss();
                            popupMenu = null;
                            anchor.showContextMenu();
                            break;
                        case R.string.show_history:
                            mainActivity.showHistory();
                            break;
                        case R.string.show_untagged:
                            mainActivity.showUntagged();
                            break;
                    }
                }
            }
        });
        popupMenu.setOnItemLongClickListener(new ListPopup.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(ListAdapter adapter, View view, int position) {
                Object adapterItem = adapter.getItem(position);
                if (adapterItem instanceof TagsMenu.MenuItemTag) {
                    TagsMenu.MenuItemTag item = (TagsMenu.MenuItemTag) adapterItem;
                    String msg = mainActivity.getResources().getString(R.string.toast_favorites_added);
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites(mainActivity, TagsProvider.generateUniqueId(item.tag));
                    Toast.makeText(mainActivity, String.format(msg, item.tag), Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        popupMenu.show(anchor, 0f);
        return popupMenu;
    }
}
