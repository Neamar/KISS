package fr.neamar.kiss.forwarder;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG;
import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Pair;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

public class Favorites extends Forwarder {
    private static final String TAG = Favorites.class.getSimpleName();

    // Package used by Android when an Intent can be matched with more than one app
    private static final String DEFAULT_RESOLVER = "com.android.internal.app.ResolverActivity";
    private FavoriteAdapter favoriteAdapter;

    private static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void inflateFavorite(@NonNull Result<?> item) {
            item.inflateFavorite(itemView.getContext(), itemView);
        }
    }

    private static class FavoriteAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final List<Result<?>> results = new ArrayList<>();
        private OnItemClickListener mOnItemClickListener = null;
        private OnItemLongClickListener mOnItemLongClickListener = null;
        private int itemHeight = ViewGroup.LayoutParams.MATCH_PARENT;

        public FavoriteAdapter() {
            super();
            setHasStableIds(true);
        }

        public boolean moveItem(int fromPosition, int toPosition) {
            if (fromPosition != toPosition) {
                Result<?> result = results.remove(fromPosition);
                results.add(toPosition, result);
                notifyItemMoved(fromPosition, toPosition);
                return true;
            } else {
                return false;
            }
        }

        public void setFavorites(List<Result<?>> results) {
            this.results.clear();
            this.results.addAll(results);
            notifyDataSetChanged();
        }

        public void updateFavoritePositions(Context context) {
            List<Pair<String, Integer>> positions = new ArrayList<>();
            for (int i = 0; i < getItemCount(); i++) {
                positions.add(new Pair<>(getItem(i).getFavoriteId(), i));
            }
            KissApplication.getApplication(context).getDataHandler().setFavoritePositions(positions);
        }

        public interface OnItemClickListener {
            void onClick(View v, Result<?> result);
        }

        public interface OnItemLongClickListener {
            boolean onLongClick(View v, Result<?> result);
        }

        public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
            this.mOnItemClickListener = mOnItemClickListener;
        }

        public void setOnItemLongClickListener(OnItemLongClickListener mOnItemLongClickListener) {
            this.mOnItemLongClickListener = mOnItemLongClickListener;
        }

        @Override
        public int getItemViewType(int position) {
            return Result.getItemViewType(getItem(position));
        }

        private Result<?> getItem(int position) {
            return results.get(position);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = R.layout.favorite_item;
            if (viewType == 6) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
                if (!sharedPreferences.getBoolean("pref-fav-tags-drawable", false)) {
                    layout = R.layout.favorite_tag;
                }
            }

            View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Result<?> result = getItem(position);
            holder.itemView.getLayoutParams().height = itemHeight;
            holder.inflateFavorite(result);
            holder.itemView.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(v, result);
                }
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (mOnItemLongClickListener != null) {
                    return mOnItemLongClickListener.onLongClick(v, result);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getFavoriteId().hashCode();
        }
    }

    private class ItemMoveCallback extends ItemTouchHelper.Callback {

        private static final float DRAG_SCALE = 1.2f;

        private final FavoriteAdapter mAdapter;
        private boolean moved;

        private ItemMoveCallback(@NonNull FavoriteAdapter mAdapter) {
            this.mAdapter = mAdapter;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            if (((GridLayoutManager) recyclerView.getLayoutManager()).getSpanCount() < mAdapter.getItemCount()) {
                // more than one row
                dragFlags |= ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            }
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return mAdapter.moveItem(viewHolder.getAbsoluteAdapterPosition(), target.getAbsoluteAdapterPosition());
        }

        @Override
        public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
            super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
            moved = true;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            // as soon as the finger moves, the user is dragging: close the menu opened by the long press
            if (isCurrentlyActive && Math.hypot(dX, dY) > ViewConfiguration.get(recyclerView.getContext()).getScaledTouchSlop()) {
                Favorites.this.mainActivity.dismissPopup();
                Favorites.this.mainActivity.closeContextMenu();
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.animate().scaleX(1f).scaleY(1f);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ACTION_STATE_DRAG) {
                if (viewHolder != null) {
                    viewHolder.itemView.animate().scaleX(DRAG_SCALE).scaleY(DRAG_SCALE);
                    if (viewHolder.itemView.isLongClickable()) {
                        viewHolder.itemView.performLongClick();
                    }
                }
            } else if (actionState == ACTION_STATE_IDLE) {
                if (moved) {
                    mAdapter.updateFavoritePositions(mainActivity);
                    moved = false;
                }
            }
        }
    }

    Favorites(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        if (isExternalFavoriteBarEnabled()) {
            mainActivity.favoritesBar = mainActivity.findViewById(R.id.externalFavoriteBar);
            // Hide the embedded bar
            mainActivity.findViewById(R.id.embeddedFavoritesBar).setVisibility(View.INVISIBLE);
        } else {
            mainActivity.favoritesBar = mainActivity.findViewById(R.id.embeddedFavoritesBar);
            // Hide the external bar
            mainActivity.findViewById(R.id.externalFavoriteBar).setVisibility(View.GONE);
        }
        favoriteAdapter = new FavoriteAdapter();

        favoriteAdapter.setOnItemClickListener(this::onClick);
        favoriteAdapter.setOnItemLongClickListener(this::onLongClick);

        ItemTouchHelper.Callback callback = new ItemMoveCallback(favoriteAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mainActivity.favoritesBar);

        mainActivity.favoritesBar.setAdapter(favoriteAdapter);


        if (prefs.getBoolean("first-run-favorites", true)) {
            // It is the first run. Make sure this is not an update by checking if history is empty
            if (DBHelper.getHistoryLength(mainActivity) == 0) {
                addDefaultAppsToFavs();
            }
            // set flag to false
            prefs.edit().putBoolean("first-run-favorites", false).apply();
        }

        onFavoriteChange();
    }

    public void onFavoriteChange() {
        List<Pojo> favoritesPojo = KissApplication.getApplication(mainActivity).getDataHandler().getFavorites();
        int favSize = favoritesPojo.size();

        int rows = 1;
        RecyclerView favoritesBar = mainActivity.favoritesBar;
        if (isExternalFavoriteBarEnabled()) {
            int perRow = Integer.parseInt(prefs.getString("favorites-per-row", "6"));
            rows = Math.max((favSize + perRow - 1) / perRow, 1);
            int rowHeight = mainActivity.getResources().getDimensionPixelSize(prefs.getBoolean("large-favorites-bar", false) ? R.dimen.large_favorite_height : R.dimen.favorite_height);
            favoritesBar.getLayoutParams().height = rowHeight * rows;
            favoritesBar.requestLayout();
            favoriteAdapter.itemHeight = rows > 1 ? rowHeight : ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if (favoritesBar.getLayoutManager() instanceof GridLayoutManager) {
            ((GridLayoutManager) favoritesBar.getLayoutManager()).setSpanCount(Math.max((favSize + rows - 1) / rows, 1));
        }
        favoriteAdapter.setFavorites(favoritesPojo.stream().map(pojo -> Result.fromPojo(mainActivity, pojo)).collect(Collectors.toList()));
    }

    private void onClick(View v, Result<?> result) {
        result.fastLaunch(mainActivity, v);
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private boolean onLongClick(View v, Result<?> result) {
        ListPopup popup = result.getPopupMenu(mainActivity, mainActivity.adapter, v);
        mainActivity.registerPopup(popup);
        popup.show(v);
        return true;
    }

    private boolean isExternalFavoriteBarEnabled() {
        return prefs.getBoolean("enable-favorites-bar", true);
    }

    /**
     * On first run, fill the favorite bar with sensible defaults
     */
    private void addDefaultAppsToFavs() {
        {
            // Default phone call app
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
            phoneIntent.setData(Uri.parse("tel:0000"));
            ComponentName componentName = getLaunchingComponent(phoneIntent);
            if (componentName != null) {
                Log.i(TAG, "Dialer resolves to: " + componentName);
                KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites("app://" + componentName.getPackageName() + "/" + componentName.getClassName());
            }
        }
        {
            // Default contacts app
            Intent contactsIntent = new Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI);
            ComponentName componentName = getLaunchingComponent(contactsIntent);
            if (componentName != null) {
                Log.i(TAG, "Contacts resolves to: " + componentName);
                KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites("app://" + componentName.getPackageName() + "/" + componentName.getClassName());
            }
        }
        {
            // Default browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://"));
            ComponentName componentName = getLaunchingComponent(browserIntent);
            if (componentName != null) {
                Log.i(TAG, "Browser resolves to: " + componentName);
                KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites("app://" + componentName.getPackageName() + "/" + componentName.getClassName());
            }
        }
    }

    /**
     * @param intent intent
     * @return launching component for given intent
     */
    @Nullable
    private ComponentName getLaunchingComponent(Intent intent) {
        ComponentName componentName = PackageManagerUtils.getComponentName(mainActivity, intent);
        ComponentName launchingComponent = PackageManagerUtils.getLaunchingComponent(mainActivity, componentName, UserHandle.OWNER);
        if (launchingComponent != null && !launchingComponent.getClassName().equals(DEFAULT_RESOLVER)) {
            return launchingComponent;
        }
        return null;
    }

    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        onFavoriteChange();
    }

}

